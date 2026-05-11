# Hướng dẫn thiết lập Cơ sở dữ liệu (Database Setup Guide)

Tài liệu này hướng dẫn chi tiết các bước để thiết lập cơ sở dữ liệu PostgreSQL cho dự án **Hashiji-Cafe** từ đầu. 

## 1. Yêu cầu hệ thống
- **Hệ quản trị CSDL**: PostgreSQL (Khuyên dùng bản 13 trở lên).
- **Extension**: `uuid-ossp` (Để hỗ trợ chuẩn mã định danh UUID).

## 2. Quy trình thiết lập (4 Bước)

### Bước 1: Kích hoạt Extension UUID
Mở công cụ truy vấn (Query Tool) và chạy lệnh sau để hệ thống nhận diện được hàm sinh mã UUID:

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

---

### Bước 2: Khởi tạo cấu trúc bảng (Schema)
Chạy đoạn script sau để tạo toàn bộ các bảng cần thiết. Cấu trúc này đã bao gồm các cột hỗ trợ cả logic mới và logic legacy (cũ) để đảm bảo tính tương thích:

```sql
-- 1. Categories
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    name_vi VARCHAR(50),
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(15),
    hourly_rate NUMERIC(12,2),
    user_code VARCHAR(20) UNIQUE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. User Addresses
CREATE TABLE IF NOT EXISTS user_addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address_line VARCHAR(500),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Promotions
CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    discount_type VARCHAR(50),
    discount_value NUMERIC(12,2),
    min_order_value NUMERIC(12,2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Products
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL UNIQUE,
    name_vi VARCHAR(100),
    description TEXT,
    description_vi TEXT,
    tags TEXT,
    image VARCHAR(500),
    base_price NUMERIC(10,2),
    is_available BOOLEAN DEFAULT TRUE,
    avg_rating NUMERIC(3,2) DEFAULT 0,
    review_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Orders
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    address_id UUID REFERENCES user_addresses(id),
    promotion_id UUID REFERENCES promotions(id),
    sub_total NUMERIC(12,2),
    discount_amount NUMERIC(12,2),
    grand_total NUMERIC(12,2),
    order_status VARCHAR(50),
    payment_method VARCHAR(50),
    payment_status VARCHAR(50),
    tracking_code VARCHAR(100) UNIQUE,
    -- Cột Legacy hỗ trợ UI cũ
    customer_name VARCHAR(255),
    phone VARCHAR(20),
    address_text TEXT,
    note TEXT,
    order_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    snapshot_product_name VARCHAR(255),
    snapshot_unit_price NUMERIC(12,2),
    quantity INTEGER,
    snapshot_options JSONB,
    sub_total NUMERIC(12,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Product Reviews
CREATE TABLE IF NOT EXISTS product_reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating_score INTEGER CHECK (rating_score >= 1 AND rating_score <= 5),
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Shopping Sessions & Cart Items
CREATE TABLE IF NOT EXISTS shopping_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    total_amount NUMERIC(12,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES shopping_sessions(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER DEFAULT 1,
    selected_options JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. User Behavior Logs
CREATE TABLE IF NOT EXISTS user_behavior_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID,
    action_type VARCHAR(50),
    action_weight NUMERIC(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### Bước 3: Cài đặt Đối tượng Nâng cao (Triggers & Stored Procedures)
Sau khi đã có bảng, hãy mở file `src/main/resources/schema-advanced.sql` và chạy toàn bộ nội dung trong đó. File này chứa:
- **Procedure `place_order`**: Logic đặt hàng phức tạp.
- **Trigger `update_product_rating`**: Tự động tính điểm trung bình sản phẩm khi có đánh giá mới.
- **Trigger `log_cart_behavior`**: Ghi log hành vi người dùng (ADD_TO_CART).
- **Trigger `enforce_single_default_address`**: Đảm bảo chỉ có một địa chỉ mặc định duy nhất cho mỗi user.

---

### Bước 4: Nạp dữ liệu mẫu (Seed Data)
Cuối cùng, chạy nội dung file `src/main/resources/seed-data.sql` để tạo dữ liệu ban đầu cho các tài khoản Admin, Khách hàng và các sản phẩm mẫu.

## 3. Lưu ý khi gặp lỗi
1. **Lỗi `uuid_generate_v4()` không tồn tại**: Đảm bảo đã chạy Bước 1 thành công. Nếu vẫn gặp lỗi, hãy thử chạy `CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;`.
2. **Lỗi khóa ngoại (Foreign Key)**: Khi muốn xóa bảng để tạo lại, hãy sử dụng lệnh `DROP TABLE ... CASCADE` để xóa sạch các liên kết.
3. **Thứ tự thực hiện**: Tuyệt đối tuân thủ thứ tự 1 -> 2 -> 3 -> 4.
