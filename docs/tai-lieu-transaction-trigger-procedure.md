# Tài liệu học: Transaction, Trigger, Stored Procedure
## Case Study: Dự án Quản lý Quán Cà Phê Hashiji-Cafe
### Hệ quản trị: PostgreSQL

---

# PHẦN 1: TRANSACTION

## 1.1 Khái niệm Transaction

**Transaction** là một nhóm các câu lệnh SQL được thực thi **nguyên tử** (atomic) – nghĩa là hoặc TẤT CẢ thành công, hoặc KHÔNG CÂU NÀO có hiệu lực.

### Ví dụ thực tế trong Hashiji-Cafe:
Khi khách hàng đặt hàng, hệ thống cần:
1. Tạo đơn hàng mới trong bảng `orders`
2. Tạo các mục đơn hàng trong bảng `order_items`
3. Xóa giỏ hàng trong bảng `cart_items`
4. Cập nhật phiên mua sắm trong bảng `shopping_sessions`

Nếu bước 1-2 thành công nhưng bước 3 thất bại → dữ liệu không nhất quán (đơn hàng tồn tại nhưng giỏ hàng vẫn còn).

### ACID Properties

| Property | Mô tả | Ví dụ Hashiji-Cafe |
|----------|-------|---------------------|
| **Atomicity** (Nguyên tử) | Tất cả hoặc không gì cả | Đặt hàng + xóa giỏ: nếu xóa giỏ lỗi → đơn hàng cũng bị hủy |
| **Consistency** (Nhất quán) | DB luôn ở trạng thái hợp lệ | Tổng `grand_total` luôn = `sub_total - discount_amount` |
| **Isolation** (Cô lập) | Các transaction không ảnh hưởng lẫn nhau | 2 người đặt cùng lúc không đọc được dữ liệu dở dang của nhau |
| **Durability** (Bền vững) | Dữ liệu đã COMMIT thì không bao giờ mất | Mất điện sau COMMIT → dữ liệu vẫn còn |

---

## 1.2 Syntax: BEGIN, COMMIT, ROLLBACK (PostgreSQL)

### Cú pháp cơ bản

```sql
-- Bắt đầu transaction
BEGIN;

-- Thực hiện các câu lệnh SQL
INSERT INTO ...;
UPDATE ...;
DELETE ...;

-- Nếu thành công → lưu thay đổi
COMMIT;

-- Nếu có lỗi → hủy bỏ tất cả
ROLLBACK;
```

### Ví dụ 1: Transaction đơn giản trong Hashiji-Cafe

**Giải thích:** Khi khách đặt hàng, cần tạo đơn hàng + xóa giỏ hàng cùng thành công hoặc cùng thất bại.

```sql
-- Bắt đầu transaction
BEGIN;

-- Tạo đơn hàng mới
INSERT INTO orders (id, user_id, address_id, sub_total, discount_amount, grand_total, order_status, payment_status)
VALUES (uuid_generate_v4(), 'user-uuid-123', 'addr-uuid-456', 75000, 0, 75000, 'PENDING', 'UNPAID');

-- Tạo mục đơn hàng (mỗi sản phẩm trong giỏ → 1 dòng order_items)
INSERT INTO order_items (id, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total)
VALUES (uuid_generate_v4(), 'order-uuid-789', 'prod-uuid-abc', N'Cà phê Đen', 25000, 3, 75000);

-- Xóa giỏ hàng sau khi đã tạo đơn hàng
DELETE FROM cart_items WHERE session_id = 'session-uuid-def';

-- Reset tổng tiền phiên mua sắm về 0
UPDATE shopping_sessions SET total_amount = 0 WHERE id = 'session-uuid-def';

-- Nếu tất cả thành công → lưu vĩnh viễn
COMMIT;

-- Nếu có lỗi → hủy bỏ tất cả, database trở về trạng thái ban đầu
ROLLBACK;
```

---

## 1.3 Isolation Levels (PostgreSQL)

PostgreSQL hỗ trợ 4 mức độ cô lập:

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Mô tả |
|-------|------------|---------------------|--------------|-------|
| READ UNCOMMITTED | Không* | Có thể | Có thể | PostgreSQL thực tế giống READ COMMITTED |
| READ COMMITTED | Không | Có thể | Có thể | Mặc định của PostgreSQL |
| REPEATABLE READ | Không | Không | Có thể | Đảm bảo đọc lại cùng dữ liệu |
| SERIALIZABLE | Không | Không | Không | Cô lập hoàn toàn (chậm nhất) |

> *PostgreSQL không bao giờ cho phép Dirty Read ngay cả với READ UNCOMMITTED.

### Cú pháp Isolation Level

```sql
-- READ COMMITTED (mặc định): Chỉ đọc dữ liệu đã commit
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- REPEATABLE READ: Đảm bảo đọc lại cùng dữ liệu trong transaction
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- SERIALIZABLE: Cô lập hoàn toàn, như thể chạy tuần tự
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

### Ví dụ: Phantom Read trong Hashiji-Cafe

**Giải thích:** Phantom Read xảy ra khi kết quả query thay đổi giữa 2 lần đọc trong cùng 1 transaction, vì transaction khác đã thêm dữ liệu mới.

```sql
-- TRANSACTION 1: Đếm sản phẩm
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT COUNT(*) FROM products WHERE is_available = true;  -- Kết quả: 10

-- TRANSACTION 2 (song song): Thêm sản phẩm mới
BEGIN;
INSERT INTO products (id, name, base_price, is_available)
VALUES (uuid_generate_v4(), N'Trà sữa Matcha', 35000, true);
COMMIT;

-- TRANSACTION 1: Đếm lại → Kết quả: 11 (PHANTOM!)
SELECT COUNT(*) FROM products WHERE is_available = true;
COMMIT;
```

**Nhận xét:** Phantom Read xảy ra vì READ COMMITTED cho phép đọc dữ liệu mới được commit. Nếu dùng SERIALIZABLE, lần đếm thứ 2 sẽ vẫn là 10.

---

## 1.4 Dirty Read (Đọc bẩn)

**Giải thích:** Dirty Read là đọc dữ liệu mà transaction khác đã ghi nhưng CHƯA COMMIT. Nếu transaction đó rollback → dữ liệu đọc được là SAI.

```sql
-- TRANSACTION A: Cập nhật giá nhưng chưa commit
BEGIN;
UPDATE products SET base_price = 30000 WHERE name = N'Cà phê Đen';

-- TRANSACTION B (song song): Đọc giá → thấy 30000 (dữ liệu bẩn)
BEGIN TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SELECT base_price FROM products WHERE name = N'Cà phê Đen';  -- 30000

-- TRANSACTION A: Rollback → giá trở về 25000
ROLLBACK;

-- HẬU QUẢ: Transaction B đã đọc giá 30000, nhưng giá thực tế là 25000
-- Nếu tính hóa đơn dựa trên giá 30000 → SAI!
```

> **Lưu ý:** PostgreSQL không cho phép Dirty Read ngay cả với READ UNCOMMITTED (tự động nâng cấp lên READ COMMITTED).

---

## 1.5 EXCEPTION Error Handling (PostgreSQL)

PostgreSQL sử dụng `EXCEPTION` thay vì `TRY...CATCH`.

### Cú pháp EXCEPTION

```sql
DO $$
BEGIN
    -- Khối thực thi (TRY equivalent)
    INSERT INTO products (id, name, base_price)
    VALUES (uuid_generate_v4(), N'Cà phê Đen', 25000);
    
    RAISE NOTICE 'Thành công!';

EXCEPTION
    -- Khối xử lý lỗi (CATCH equivalent)
    WHEN unique_violation THEN
        RAISE NOTICE 'Lỗi: Tên sản phẩm đã tồn tại!';
    WHEN foreign_key_violation THEN
        RAISE NOTICE 'Lỗi: Vi phạm khóa ngoại!';
    WHEN not_null_violation THEN
        RAISE NOTICE 'Lỗi: Thiếu giá trị bắt buộc!';
    WHEN check_violation THEN
        RAISE NOTICE 'Lỗi: Giá trị không hợp lệ!';
    WHEN OTHERS THEN
        RAISE NOTICE 'Lỗi không xác định: % (Mã: %)', SQLERRM, SQLSTATE;
END $$;
```

### Ví dụ: EXCEPTION trong Hashiji-Cafe

**Giải thích:** Function đặt hàng với xử lý lỗi - kiểm tra dữ liệu trước khi thêm, báo lỗi rõ ràng nếu có vấn đề.

```sql
CREATE OR REPLACE FUNCTION place_order_with_error_handling(
    p_user_id UUID,
    p_address_id UUID,
    p_items JSONB
)
RETURNS UUID AS $$
DECLARE
    v_order_id UUID;
    v_item JSONB;
    v_product products%ROWTYPE;
    v_sub_total NUMERIC(12,2) := 0;
BEGIN
    -- Kiểm tra address có thuộc user không
    IF NOT EXISTS (
        SELECT 1 FROM user_addresses 
        WHERE id = p_address_id AND user_id = p_user_id
    ) THEN
        RAISE EXCEPTION 'Địa chỉ không thuộc về người dùng này';
    END IF;

    -- Tạo đơn hàng mới
    v_order_id := uuid_generate_v4();
    INSERT INTO orders (id, user_id, address_id, order_status, payment_status)
    VALUES (v_order_id, p_user_id, p_address_id, 'PENDING', 'UNPAID');

    -- Lặp qua từng item trong JSONB
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        SELECT * INTO v_product 
        FROM products 
        WHERE id = (v_item->>'product_id')::UUID;
        
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Sản phẩm không tồn tại: %', v_item->>'product_id';
        END IF;
        
        IF NOT v_product.is_available THEN
            RAISE EXCEPTION 'Sản phẩm % không khả dụng', v_product.name;
        END IF;
        
        v_sub_total := v_product.base_price * (v_item->>'quantity')::INT;
        
        INSERT INTO order_items (id, order_id, product_id, snapshot_product_name, 
                                 snapshot_unit_price, quantity, sub_total)
        VALUES (
            uuid_generate_v4(), v_order_id, v_product.id,
            v_product.name, v_product.base_price,
            (v_item->>'quantity')::INT, v_sub_total
        );
    END LOOP;

    UPDATE orders 
    SET sub_total = v_sub_total,
        grand_total = v_sub_total - COALESCE(discount_amount, 0)
    WHERE id = v_order_id;

    RETURN v_order_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Lỗi đặt hàng: %', SQLERRM;
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

---

# PHẦN 2: STORED PROCEDURE / FUNCTION

## 2.1 Khái niệm và ưu điểm

**Stored Procedure / Function** là khối mã SQL được lưu sẵn trong database, có thể nhận tham số đầu vào và trả về kết quả.

> ⚠️ PostgreSQL gọi là **Function** (dùng `CREATE FUNCTION`), khác MySQL dùng `CREATE PROCEDURE`. Về bản chất là giống nhau.

### Ưu điểm

| Ưu điểm | Mô tả |
|----------|-------|
| **Tái sử dụng** | Viết 1 lần, gọi nhiều lần |
| **Hiệu năng** | Chạy trực tiếp trong DB, không cần gửi nhiều câu SQL qua mạng |
| **Bảo mật** | Ứng dụng chỉ cần gọi tên hàm, không cần biết cấu trúc bảng |
| **Dễ bảo trì** | Thay đổi logic ở một nơi |

---

## 2.2 Syntax: CREATE FUNCTION (PostgreSQL)

### Cú pháp cơ bản

```sql
CREATE OR REPLACE FUNCTION function_name(
    param1 datatype,                    -- Tham số bắt buộc
    param2 datatype DEFAULT default_value  -- Tham số có giá trị mặc định
)
RETURNS return_type AS $$
DECLARE
    local_variable datatype;
    v_count INTEGER := 0;
BEGIN
    -- Logic xử lý
    local_variable := 'some value';
    SELECT COUNT(*) INTO v_count FROM products;
    RETURN result;
END;
$$ LANGUAGE plpgsql;
```

**Giải thích:**
- `$$ ... $$`: Delimiter đánh dấu khối PL/pgSQL (tránh conflict với dấu nháy đơn)
- `DECLARE`: Khai báo biến cục bộ, chỉ tồn tại trong function
- `INTO`: Gán kết quả query vào biến
- `LANGUAGE plpgsql`: Chỉ định ngôn ngữ PL/pgSQL

### Cú pháp gọi Function

```sql
-- Cách 1: Gọi đơn giản
SELECT function_name();

-- Cách 2: Truyền tham số theo vị trí
SELECT function_name(value1, value2);

-- Cách 3: Truyền tham số theo tên (có thể đảo thứ tự)
SELECT function_name(param2 := value2, param1 := value1);

-- Cách 4: Dùng trong SELECT
SELECT id, function_name(id) AS result FROM table;

-- Cách 5: Dùng với FROM (function trả về bảng)
SELECT * FROM function_name(value1, value2);
```

### Ví dụ 1: Function không có tham số

**Giải thích:** Function trả về danh sách sản phẩm đang bán, sắp xếp theo tên. Dùng `RETURNS TABLE` để trả về dạng bảng.

```sql
CREATE OR REPLACE FUNCTION get_available_products()
RETURNS TABLE(
    id UUID,
    name VARCHAR,
    base_price NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT p.id, p.name, p.base_price
    FROM products p
    WHERE p.is_available = true
    ORDER BY p.name;
END;
$$ LANGUAGE plpgsql;

-- Gọi function
SELECT * FROM get_available_products();
```

### Ví dụ 2: Function với INPUT parameter

**Giải thích:** Function tìm sản phẩm theo danh mục, sắp xếp rating cao nhất lên đầu. `NULLS LAST` đưa sản phẩm chưa có rating xuống cuối.

```sql
CREATE OR REPLACE FUNCTION get_products_by_category(p_category_id UUID)
RETURNS TABLE(
    id UUID,
    name VARCHAR,
    base_price NUMERIC,
    avg_rating NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT p.id, p.name, p.base_price, p.avg_rating
    FROM products p
    WHERE p.category_id = p_category_id
      AND p.is_available = true
    ORDER BY p.avg_rating DESC NULLS LAST;
END;
$$ LANGUAGE plpgsql;

-- Gọi function
SELECT * FROM get_products_by_category('category-uuid-123');
```

---

## 2.3 Parameters: INPUT, OUTPUT, DEFAULT

### OUTPUT Parameter (PostgreSQL dùng RETURNS)

```sql
CREATE OR REPLACE FUNCTION count_products_by_category(
    p_category_id UUID,
    OUT product_count INTEGER
) AS $$
BEGIN
    SELECT COUNT(*) INTO product_count
    FROM products
    WHERE category_id = p_category_id AND is_available = true;
END;
$$ LANGUAGE plpgsql;

-- Gọi function
SELECT count_products_by_category('category-uuid-123');
```

### DEFAULT Parameter

```sql
CREATE OR REPLACE FUNCTION get_products(
    p_is_available BOOLEAN DEFAULT true
)
RETURNS TABLE(id UUID, name VARCHAR, base_price NUMERIC) AS $$
BEGIN
    RETURN QUERY
    SELECT p.id, p.name, p.base_price
    FROM products p
    WHERE p.is_available = p_is_available
    ORDER BY p.name;
END;
$$ LANGUAGE plpgsql;

-- Gọi không truyền tham số → dùng giá trị mặc định
SELECT * FROM get_products();  -- Lấy sản phẩm đang bán

-- Gọi với tham số
SELECT * FROM get_products(false);  -- Lấy sản phẩm ngưng bán
```

---

## 2.4 Variables, IF/ELSE, CASE, WHILE (PL/pgSQL)

### Variables

**Giải thích:** Biến phải khai báo trong khối `DECLARE` trước `BEGIN`. Quy ước đặt tên: `v_` (viết tắt "variable").

```sql
DECLARE
    v_product_name VARCHAR(100);
    v_base_price NUMERIC(10,2) := 25000;  -- Gán giá trị mặc định
    v_product_count INTEGER;

-- Cách 1: Gán bằng INTO (lấy từ query)
SELECT name, base_price INTO v_product_name, v_base_price
FROM products WHERE id = 'product-uuid-123';

-- Cách 2: Gán bằng := (gán giá trị cứng)
v_product_count := 10;
v_product_count := v_product_count + 1;

-- Hiển thị giá trị biến
RAISE NOTICE 'Sản phẩm: %, Giá: %', v_product_name, v_base_price;
```

### IF...ELSE

**Giải thích:** Phân nhánh logic dựa trên điều kiện. Lưu ý: `ELSIF` (không phải `ELSE IF`), `END IF` bắt buộc.

```sql
DECLARE
    v_quantity INTEGER := 5;
    v_price NUMERIC(10,2) := 25000;
    v_discount NUMERIC(10,2);
BEGIN
    IF v_quantity > 10 THEN
        v_discount := v_price * 0.1;  -- Giảm 10%
    ELSIF v_quantity > 5 THEN
        v_discount := v_price * 0.05; -- Giảm 5%
    ELSE
        v_discount := 0;              -- Không giảm
    END IF;
    
    RAISE NOTICE 'Giảm giá: %', v_discount;
END;
```

### CASE...WHEN

**Giải thích:** Chuyển đổi giá trị hiển thị, tương tự `switch...case` trong các ngôn ngữ khác.

```sql
SELECT 
    id, name, order_status,
    CASE order_status
        WHEN 'PENDING' THEN N'Chờ xử lý'
        WHEN 'PROCESSING' THEN N'Đang xử lý'
        WHEN 'COMPLETED' THEN N'Hoàn thành'
        WHEN 'CANCELLED' THEN N'Đã hủy'
        ELSE N'Không xác định'
    END AS status_text
FROM orders;
```

### WHILE Loop

**Giải thích:** Lặp lại khi điều kiện còn đúng. Các câu lệnh điều khiển: `EXIT` (thoát), `CONTINUE` (bỏ qua, quay lại đầu).

```sql
DECLARE
    v_factorial INTEGER := 1;
    v_n INTEGER := 5;
BEGIN
    WHILE v_n > 1 LOOP
        v_factorial := v_factorial * v_n;  -- 1×5=5, 5×4=20, 20×3=60, 60×2=120
        v_n := v_n - 1;
    END LOOP;
    
    RAISE NOTICE '5! = %', v_factorial;  -- 5! = 120
END;
```

---

## 2.5 Function: Scalar, Table-valued (PostgreSQL)

### Scalar Function (trả về 1 giá trị)

```sql
CREATE OR REPLACE FUNCTION calculate_order_total(p_order_id UUID)
RETURNS NUMERIC(12,2) AS $$
DECLARE
    v_total NUMERIC(12,2);
BEGIN
    SELECT COALESCE(SUM(sub_total), 0) INTO v_total
    FROM order_items
    WHERE order_id = p_order_id;
    RETURN v_total;
END;
$$ LANGUAGE plpgsql;

-- Sử dụng
SELECT id, calculate_order_total(id) AS total_amount FROM orders;
```

### Table-valued Function (trả về bảng)

```sql
CREATE OR REPLACE FUNCTION get_user_recent_orders(p_user_id UUID, p_limit INTEGER DEFAULT 5)
RETURNS TABLE(
    order_id UUID,
    grand_total NUMERIC(12,2),
    order_status VARCHAR,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT o.id, o.grand_total, o.order_status::VARCHAR, o.created_at
    FROM orders o
    WHERE o.user_id = p_user_id
    ORDER BY o.created_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Sử dụng
SELECT * FROM get_user_recent_orders('user-uuid-123', 3);
```

---

# PHẦN 3: TRIGGER

## 3.1 Khái niệm và tại sao dùng

**Trigger** là hàm tự động chạy khi có sự kiện INSERT, UPDATE, hoặc DELETE xảy ra trên bảng. Không cần gọi bằng tay – database tự kích hoạt.

### Tại sao dùng Trigger?

| Use Case | Mô tả |
|----------|-------|
| **Implement business rules** | Tự động cập nhật rating khi có review mới |
| **Đảm bảo data integrity** | Kiểm tra dữ liệu trước khi insert/update |
| **Audit logging** | Ghi lại lịch sử thay đổi dữ liệu |
| **Tính toán derived attributes** | Tự động tính avg_rating khi review thay đổi |

### Trigger vs Function

| Đặc điểm | Trigger | Function |
|-----------|---------|----------|
| Cách gọi | Tự động (bởi sự kiện) | Thủ công (SELECT) |
| Tham số | Không có tham số | Có tham số |
| RETURN | RETURN NEW/OLD | RETURN giá trị |

---

## 3.2 Syntax: CREATE TRIGGER (PostgreSQL)

### Cú pháp cơ bản

**Giải thích:** Trigger gồm 2 phần: Function (logic xử lý) + Trigger (gắn vào bảng).

```sql
-- Bước 1: Tạo hàm trigger
CREATE OR REPLACE FUNCTION trigger_function_name()
RETURNS TRIGGER AS $$
BEGIN
    -- NEW: Dòng MỚI (INSERT/UPDATE)
    -- OLD: Dòng CŨ (UPDATE/DELETE)
    -- RETURN NEW: Cho phép thao tác tiếp tục
    -- RETURN NULL: Hủy bỏ thao tác
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Bước 2: Gắn trigger vào bảng
CREATE TRIGGER trigger_name
    AFTER INSERT OR UPDATE OR DELETE ON table_name
    FOR EACH ROW
    EXECUTE FUNCTION trigger_function_name();
```

### BEFORE vs AFTER

| Loại | Khi nào chạy | Dùng khi |
|------|--------------|----------|
| `BEFORE` | **Trước** khi dữ liệu được ghi | Muốn **sửa đổi** hoặc **chặn** dữ liệu |
| `AFTER` | **Sau** khi dữ liệu đã ghi | Muốn **cập nhật bảng khác** |

### NEW vs OLD

| Biến | Có trong sự kiện | Ý nghĩa |
|------|-------------------|---------|
| `NEW` | INSERT, UPDATE | Giá trị **mới** của dòng |
| `OLD` | UPDATE, DELETE | Giá trị **cũ** của dòng |

---

## 3.3 Trigger thực tế trong Hashiji-Cafe

### Trigger 1: Tự động cập nhật rating sản phẩm

**Giải thích:** Khi có review mới/sửa/xóa → tự động tính lại `avg_rating` và `review_count` trong bảng `products`.

```sql
CREATE OR REPLACE FUNCTION update_product_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE products SET
        avg_rating = (
            SELECT AVG(rating_score)::NUMERIC(3,2)
            FROM product_reviews
            WHERE product_id = NEW.product_id
        ),
        review_count = (
            SELECT COUNT(*)
            FROM product_reviews
            WHERE product_id = NEW.product_id
        )
    WHERE id = NEW.product_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_product_rating
    AFTER INSERT OR UPDATE OR DELETE ON product_reviews
    FOR EACH ROW EXECUTE FUNCTION update_product_rating();
```

### Trigger 2: Ghi log hành vi thêm giỏ hàng

**Giải thích:** Khi khách thêm sản phẩm vào giỏ → ghi log vào `user_behavior_logs` để phục vụ AI Recommendation Engine.

```sql
CREATE OR REPLACE FUNCTION log_cart_behavior()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
BEGIN
    -- Tìm user_id từ shopping_session
    SELECT user_id INTO v_user_id
    FROM shopping_sessions
    WHERE id = NEW.session_id;

    IF v_user_id IS NOT NULL THEN
        INSERT INTO user_behavior_logs
            (id, user_id, product_id, action_type, action_weight)
        VALUES (
            uuid_generate_v4(), v_user_id, NEW.product_id,
            'ADD_TO_CART', 0.5  -- Trọng số: VIEW=0.1, ADD_TO_CART=0.5, PURCHASE=1.0
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_cart_behavior
    AFTER INSERT ON cart_items
    FOR EACH ROW EXECUTE FUNCTION log_cart_behavior();
```

### Trigger 3: Giữ duy nhất 1 địa chỉ mặc định

**Giải thích:** Mỗi user chỉ có 1 địa chỉ mặc định. Khi thêm/sửa địa chỉ mới là mặc định → tự động tắt tất cả địa chỉ mặc định cũ.

```sql
CREATE OR REPLACE FUNCTION enforce_single_default_address()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE user_addresses
        SET is_default = FALSE
        WHERE user_id = NEW.user_id
          AND id <> NEW.id;  -- Loại trừ địa chỉ vừa được thêm/sửa
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_single_default_address
    BEFORE INSERT OR UPDATE ON user_addresses
    FOR EACH ROW EXECUTE FUNCTION enforce_single_default_address();
```

---

## 3.4 Transaction trong Trigger

**Giải thích:** Trigger luôn là một phần của transaction khởi tạo nó. Nếu trigger `RAISE EXCEPTION` → toàn bộ transaction bị rollback.

```sql
CREATE OR REPLACE FUNCTION prevent_invalid_product()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.base_price <= 0 THEN
        RAISE EXCEPTION 'Giá sản phẩm phải lớn hơn 0';
        -- Transaction sẽ bị rollback tự động
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_product
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION prevent_invalid_product();

-- Test: Chèn sản phẩm giá âm → lỗi, rollback
BEGIN;
INSERT INTO products (id, name, base_price)
VALUES (uuid_generate_v4(), N'Sản phẩm lỗi', -1000);
ROLLBACK;

-- Kiểm tra: Sản phẩm KHÔNG tồn tại
SELECT * FROM products WHERE name = N'Sản phẩm lỗi';
```

---

## 3.5 Cursor (PostgreSQL)

**Giải thích:** Cursor cho phép duyệt qua từng dòng trong kết quả query, tương tự vòng `for-each`.

### Cú pháp Cursor

```sql
DECLARE
    cursor_name CURSOR FOR SELECT statement;

OPEN cursor_name;

FETCH NEXT FROM cursor_name INTO var1, var2, ...;

WHILE FOUND LOOP
    -- Xử lý dữ liệu
    FETCH NEXT FROM cursor_name INTO var1, var2, ...;
END LOOP;

CLOSE cursor_name;
```

### Ví dụ: Cursor trong Hashiji-Cafe

**Giải thích:** Duyệt qua từng sản phẩm và phân loại theo rating. Dùng cursor vì cần xử lý từng dòng với logic phức tạp.

```sql
DO $$
DECLARE
    product_cursor CURSOR FOR
        SELECT id, name, base_price, avg_rating FROM products WHERE is_available = true;
    v_product RECORD;  -- RECORD: Chứa 1 dòng kết quả
BEGIN
    OPEN product_cursor;
    
    LOOP
        FETCH NEXT FROM product_cursor INTO v_product;
        EXIT WHEN NOT FOUND;
        
        RAISE NOTICE 'Sản phẩm: % - Giá: % - Rating: %', 
            v_product.name, v_product.base_price, v_product.avg_rating;
        
        IF v_product.avg_rating IS NULL THEN
            RAISE NOTICE '  → Chưa có đánh giá';
        ELSIF v_product.avg_rating >= 4.0 THEN
            RAISE NOTICE '  → Sản phẩm hot!';
        END IF;
    END LOOP;
    
    CLOSE product_cursor;
END $$;
```

---

# PHẦN 4: CASE STUDY - DỰ ÁN HASHIJI-CAFE

## 4.1 Transaction: Đặt hàng nguyên tử

### Tình huống
Khi khách hàng đặt hàng, cần đảm bảo:
1. Tạo đơn hàng mới trong `orders`
2. Tạo các mục đơn hàng trong `order_items`
3. Xóa giỏ hàng trong `cart_items`
4. Reset phiên mua sắm trong `shopping_sessions`
5. Nếu bất kỳ bước nào thất bại → rollback tất cả

### Code SQL

```sql
CREATE OR REPLACE FUNCTION place_order(
    p_user_id UUID,
    p_address_id UUID,
    p_promotion_id UUID,
    p_items JSONB
)
RETURNS UUID AS $$
DECLARE
    v_order_id UUID;
    v_item JSONB;
    v_product products%ROWTYPE;
    v_sub_total NUMERIC(12,2) := 0;
    v_discount_amount NUMERIC(12,2) := 0;
    v_grand_total NUMERIC(12,2);
BEGIN
    -- Kiểm tra address có thuộc user không
    IF NOT EXISTS (
        SELECT 1 FROM user_addresses 
        WHERE id = p_address_id AND user_id = p_user_id
    ) THEN
        RAISE EXCEPTION 'Địa chỉ không thuộc về người dùng này';
    END IF;

    v_order_id := uuid_generate_v4();

    -- Lặp qua từng item trong JSONB
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        SELECT * INTO v_product 
        FROM products 
        WHERE id = (v_item->>'product_id')::UUID;
        
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Sản phẩm không tồn tại: %', v_item->>'product_id';
        END IF;
        
        IF NOT v_product.is_available THEN
            RAISE EXCEPTION 'Sản phẩm % không khả dụng', v_product.name;
        END IF;
        
        DECLARE
            v_item_total NUMERIC(12,2);
        BEGIN
            v_item_total := v_product.base_price * (v_item->>'quantity')::INT;
            v_sub_total := v_sub_total + v_item_total;
            
            INSERT INTO order_items (
                id, order_id, product_id, 
                snapshot_product_name, snapshot_unit_price, 
                quantity, sub_total
            )
            VALUES (
                uuid_generate_v4(), v_order_id, v_product.id,
                v_product.name, v_product.base_price,
                (v_item->>'quantity')::INT, v_item_total
            );
        END;
    END LOOP;

    -- Tính discount nếu có promotion
    IF p_promotion_id IS NOT NULL THEN
        SELECT discount_amount INTO v_discount_amount
        FROM promotions
        WHERE id = p_promotion_id 
          AND is_active = true
          AND CURRENT_DATE BETWEEN start_date AND end_date;
        v_discount_amount := COALESCE(v_discount_amount, 0);
    END IF;

    v_grand_total := v_sub_total - v_discount_amount;

    -- Tạo đơn hàng
    INSERT INTO orders (
        id, user_id, address_id, promotion_id,
        sub_total, discount_amount, grand_total,
        order_status, payment_status
    )
    VALUES (
        v_order_id, p_user_id, p_address_id, p_promotion_id,
        v_sub_total, v_discount_amount, v_grand_total,
        'PENDING', 'UNPAID'
    );

    -- Xóa giỏ hàng
    DELETE FROM cart_items
    WHERE session_id IN (
        SELECT id FROM shopping_sessions WHERE user_id = p_user_id
    );

    -- Reset phiên mua sắm
    UPDATE shopping_sessions 
    SET total_amount = 0 
    WHERE user_id = p_user_id;

    RETURN v_order_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Lỗi đặt hàng: %', SQLERRM;
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

### Cách sử dụng

```sql
-- Đặt hàng thành công
BEGIN;
SELECT place_order(
    '550e8400-e29b-41d4-a716-446655440000'::UUID,
    '6ba7b810-9dad-11d1-80b4-00c04fd430c8'::UUID,
    NULL,
    '[{"product_id": "11111111-1111-1111-1111-111111111111", "quantity": 2}]'::JSONB
);
COMMIT;

-- Kiểm tra kết quả
SELECT * FROM orders WHERE user_id = '550e8400-e29b-41d4-a716-446655440000';
SELECT * FROM order_items WHERE order_id = (SELECT id FROM orders ORDER BY created_at DESC LIMIT 1);
```

### Test Rollback

```sql
-- Đặt hàng thất bại (sản phẩm không tồn tại)
BEGIN;
SELECT place_order(
    '550e8400-e29b-41d4-a716-446655440000'::UUID,
    '6ba7b810-9dad-11d1-80b4-00c04fd430c8'::UUID,
    NULL,
    '[{"product_id": "99999999-9999-9999-9999-999999999999", "quantity": 1}]'::JSONB
);
ROLLBACK;

-- Kiểm tra: Không có đơn hàng mới được tạo
SELECT COUNT(*) FROM orders WHERE user_id = '550e8400-e29b-41d4-a716-446655440000';
```

---

## 4.2 Trigger: Tự động cập nhật rating sản phẩm

### Tình huống
Khi có review mới (INSERT), review bị sửa (UPDATE), hoặc review bị xóa (DELETE) → tự động cập nhật `avg_rating` và `review_count` trong bảng `products`.

### Code SQL

```sql
CREATE OR REPLACE FUNCTION update_product_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE products SET
        avg_rating = (
            SELECT COALESCE(AVG(rating_score), 0)::NUMERIC(3,2)
            FROM product_reviews
            WHERE product_id = NEW.product_id
        ),
        review_count = (
            SELECT COUNT(*)
            FROM product_reviews
            WHERE product_id = NEW.product_id
        )
    WHERE id = NEW.product_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_product_rating
    AFTER INSERT OR UPDATE OR DELETE ON product_reviews
    FOR EACH ROW EXECUTE FUNCTION update_product_rating();
```

### Test trigger

```sql
-- 1. Xem trạng thái ban đầu
SELECT name, avg_rating, review_count 
FROM products 
WHERE id = '11111111-1111-1111-1111-111111111111';

-- 2. Thêm review mới
INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text)
VALUES (
    uuid_generate_v4(),
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    5,
    N'Rất ngon!'
);

-- 3. Kiểm tra lại → avg_rating = 5.00, review_count = 1
SELECT name, avg_rating, review_count 
FROM products 
WHERE id = '11111111-1111-1111-1111-111111111111';

-- 4. Thêm review thứ 2
INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text)
VALUES (
    uuid_generate_v4(),
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    4,
    N'Hơi nhạt'
);

-- 5. Kiểm tra lại → avg_rating = 4.50, review_count = 2
SELECT name, avg_rating, review_count 
FROM products 
WHERE id = '11111111-1111-1111-1111-111111111111';
```

---

## 4.3 Trigger: Ghi log hành vi người dùng

### Tình huống
Khi khách hàng thêm sản phẩm vào giỏ hàng → tự động ghi log vào `user_behavior_logs` để phục vụ AI Recommendation.

### Code SQL

```sql
CREATE OR REPLACE FUNCTION log_cart_behavior()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
BEGIN
    SELECT user_id INTO v_user_id
    FROM shopping_sessions
    WHERE id = NEW.session_id;

    IF v_user_id IS NOT NULL THEN
        INSERT INTO user_behavior_logs
            (id, user_id, product_id, action_type, action_weight)
        VALUES
            (uuid_generate_v4(), v_user_id, NEW.product_id, 'ADD_TO_CART', 0.5);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_cart_behavior
    AFTER INSERT ON cart_items
    FOR EACH ROW EXECUTE FUNCTION log_cart_behavior();
```

### Test trigger

```sql
-- 1. Xem log trước khi thêm giỏ hàng
SELECT COUNT(*) FROM user_behavior_logs WHERE action_type = 'ADD_TO_CART';

-- 2. Thêm sản phẩm vào giỏ hàng
INSERT INTO cart_items (id, session_id, product_id, quantity)
VALUES (
    uuid_generate_v4(),
    '7c9e6679-7425-40de-944b-e07fc1f90ae7',
    '11111111-1111-1111-1111-111111111111',
    2
);

-- 3. Kiểm tra log → Có 1 dòng log mới
SELECT * FROM user_behavior_logs WHERE action_type = 'ADD_TO_CART';
```

---

## 4.4 Trigger: Validation dữ liệu trước khi INSERT

### Tình huống
Kiểm tra trước khi thêm sản phẩm mới:
1. Tên sản phẩm không được trùng
2. Giá sản phẩm phải > 0
3. Tên sản phẩm không được rỗng

### Code SQL

```sql
CREATE OR REPLACE FUNCTION validate_product()
RETURNS TRIGGER AS $$
BEGIN
    -- Tên sản phẩm không được rỗng
    IF NEW.name IS NULL OR TRIM(NEW.name) = '' THEN
        RAISE EXCEPTION 'Tên sản phẩm không được rỗng';
    END IF;
    
    -- Giá sản phẩm phải > 0
    IF NEW.base_price <= 0 THEN
        RAISE EXCEPTION 'Giá sản phẩm phải lớn hơn 0';
    END IF;
    
    -- Tên sản phẩm không được trùng (trừ chính nó khi UPDATE)
    IF EXISTS (
        SELECT 1 FROM products 
        WHERE name = NEW.name 
          AND id <> COALESCE(NEW.id, uuid_generate_v4())
    ) THEN
        RAISE EXCEPTION 'Tên sản phẩm "%" đã tồn tại', NEW.name;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_product
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION validate_product();
```

### Test validation

```sql
-- Test 1: Tên rỗng → Lỗi
INSERT INTO products (id, name, base_price)
VALUES (uuid_generate_v4(), '', 25000);

-- Test 2: Giá <= 0 → Lỗi
INSERT INTO products (id, name, base_price)
VALUES (uuid_generate_v4(), N'Test', -1000);

-- Test 3: Tên trùng → Lỗi
INSERT INTO products (id, name, base_price)
VALUES (uuid_generate_v4(), N'Cà phê Đen', 25000);

-- Test 4: Hợp lệ → Thành công
INSERT INTO products (id, name, base_price)
VALUES (uuid_generate_v4(), N'Sản phẩm mới', 30000);
```

---

## 4.5 Function: Tìm sản phẩm phổ biến

```sql
CREATE OR REPLACE FUNCTION get_popular_products(p_limit INTEGER DEFAULT 10)
RETURNS TABLE(
    product_id UUID,
    product_name VARCHAR,
    base_price NUMERIC,
    total_ordered BIGINT,
    avg_rating NUMERIC,
    review_count INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.id, p.name, p.base_price,
        COALESCE(SUM(oi.quantity), 0) AS total_ordered,
        p.avg_rating, p.review_count
    FROM products p
    LEFT JOIN order_items oi ON p.id = oi.product_id
    LEFT JOIN orders o ON oi.order_id = o.id AND o.order_status = 'COMPLETED'
    WHERE p.is_available = true
    GROUP BY p.id, p.name, p.base_price, p.avg_rating, p.review_count
    ORDER BY total_ordered DESC, p.avg_rating DESC NULLS LAST
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Sử dụng
SELECT * FROM get_popular_products(5);
```

---

## 4.6 Function: Thống kê doanh thu theo ngày

```sql
CREATE OR REPLACE FUNCTION get_revenue_report(
    p_from_date DATE,
    p_to_date DATE
)
RETURNS TABLE(
    report_date DATE,
    total_orders BIGINT,
    total_revenue NUMERIC(12,2),
    avg_order_value NUMERIC(12,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        o.created_at::date AS report_date,
        COUNT(o.id) AS total_orders,
        COALESCE(SUM(o.grand_total), 0) AS total_revenue,
        COALESCE(AVG(o.grand_total), 0) AS avg_order_value
    FROM orders o
    WHERE o.created_at::date BETWEEN p_from_date AND p_to_date
      AND o.order_status = 'COMPLETED'
    GROUP BY o.created_at::date
    ORDER BY report_date;
END;
$$ LANGUAGE plpgsql;

-- Sử dụng
SELECT * FROM get_revenue_report('2026-04-01', '2026-04-30');
```

### Kết quả mẫu

| report_date | total_orders | total_revenue | avg_order_value |
|-------------|-------------|---------------|-----------------|
| 2026-04-20 | 3 | 345000 | 115000.00 |
| 2026-04-21 | 5 | 520000 | 104000.00 |

---

## 4.7 Function: Tính tổng tiền đơn hàng

```sql
CREATE OR REPLACE FUNCTION calculate_order_total(p_order_id UUID)
RETURNS NUMERIC(12,2) AS $$
DECLARE
    v_total NUMERIC(12,2);
BEGIN
    SELECT COALESCE(SUM(sub_total), 0) INTO v_total
    FROM order_items
    WHERE order_id = p_order_id;
    RETURN v_total;
END;
$$ LANGUAGE plpgsql;

-- Sử dụng
SELECT 
    o.id AS order_id,
    u.username,
    o.grand_total,
    calculate_order_total(o.id) AS calculated_total,
    CASE 
        WHEN o.grand_total = calculate_order_total(o.id) THEN 'OK'
        ELSE 'MISMATCH!'
    END AS validation
FROM orders o
JOIN users u ON o.user_id = u.id;
```

---

## 4.8 View: Dashboard tổng quan

```sql
CREATE OR REPLACE VIEW vw_cafe_dashboard AS
SELECT
    (SELECT COUNT(*) FROM products WHERE is_available = true) AS total_products,
    (SELECT COUNT(*) FROM products WHERE avg_rating >= 4.0) AS top_rated_products,
    
    (SELECT COUNT(*) FROM orders WHERE created_at::date = CURRENT_DATE) AS today_orders,
    (SELECT COALESCE(SUM(grand_total), 0) FROM orders 
     WHERE created_at::date = CURRENT_DATE AND order_status = 'COMPLETED') AS today_revenue,
    
    (SELECT COUNT(*) FROM orders 
     WHERE EXTRACT(MONTH FROM created_at) = EXTRACT(MONTH FROM CURRENT_DATE)
       AND EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM CURRENT_DATE)) AS month_orders,
    (SELECT COALESCE(SUM(grand_total), 0) FROM orders 
     WHERE EXTRACT(MONTH FROM created_at) = EXTRACT(MONTH FROM CURRENT_DATE)
       AND EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM CURRENT_DATE)
       AND order_status = 'COMPLETED') AS month_revenue,
    
    (SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') AS total_customers,
    (SELECT COUNT(*) FROM users WHERE role = 'STAFF') AS total_staff;

-- Sử dụng
SELECT * FROM vw_cafe_dashboard;
```

### View: Chi tiết đơn hàng

```sql
CREATE OR REPLACE VIEW vw_order_details AS
SELECT 
    o.id AS order_id,
    u.username AS customer_name,
    ua.address_line AS delivery_address,
    o.sub_total, o.discount_amount, o.grand_total,
    o.order_status, o.payment_status, o.payment_method,
    o.created_at AS order_date,
    COUNT(oi.id) AS total_items,
    STRING_AGG(oi.snapshot_product_name, ', ') AS product_list
FROM orders o
JOIN users u ON o.user_id = u.id
LEFT JOIN user_addresses ua ON o.address_id = ua.id
LEFT JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, u.username, ua.address_line, o.sub_total, o.discount_amount, 
         o.grand_total, o.order_status, o.payment_status, o.payment_method, o.created_at;

-- Sử dụng
SELECT * FROM vw_order_details
WHERE order_date::date = CURRENT_DATE
ORDER BY order_date DESC;
```

---

# TÓM TẮT

## Transaction
- Dùng `BEGIN`, `COMMIT`, `ROLLBACK` (PostgreSQL)
- Áp dụng ACID properties
- Sử dụng `EXCEPTION` block để xử lý lỗi
- Quan trọng cho các thao tác nhiều bước cần tính nguyên tử

## Stored Procedure / Function
- PostgreSQL dùng `CREATE FUNCTION` (không phải `CREATE PROCEDURE`)
- Hỗ trợ INPUT, OUTPUT parameters
- Kết hợp với variables, IF/ELSE, CASE, WHILE
- Tăng hiệu suất và bảo mật

## Trigger
- Tự động thực thi khi INSERT/UPDATE/DELETE
- Sử dụng biến `NEW` và `OLD`
- Dùng `BEFORE` để sửa đổi/chặn dữ liệu
- Dùng `AFTER` để cập nhật bảng khác
- Phù hợp cho validation, audit, auto-update

## Case Study Hashiji-Cafe
- **Transaction:** Đặt hàng (tạo order + xóa giỏ + reset session)
- **Trigger:** Auto-update rating, log behavior, validate data
- **Function:** Tìm sản phẩm phổ biến, thống kê doanh thu, tính tổng tiền
- **View:** Dashboard tổng quan, chi tiết đơn hàng

---

# BÀI TẬP THỰC HÀNH

> **Ghi chú:** Tất cả bài tập đều dựa trên database Hashiji-Cafe với các bảng: `users`, `products`, `categories`, `orders`, `order_items`, `product_reviews`, `user_addresses`, `shopping_sessions`, `cart_items`, `user_behavior_logs`, `promotions`.

---

## BÀI TẬP 1: TRANSACTION

### Bài 1.1: Đặt hàng với kiểm tra tồn kho

**Đề bài:** Viết function `place_order_with_stock_check` để đặt hàng, nhưng trước khi thêm vào order_items phải kiểm tra sản phẩm còn `is_available = true`. Nếu bất kỳ sản phẩm nào không khả dụng → rollback toàn bộ.

**Yêu cầu:**
- Nhận `p_user_id`, `p_address_id`, `p_items` (JSONB)
- Kiểm tra từng sản phẩm có `is_available = true`
- Nếu sản phẩm không khả dụng → RAISE EXCEPTION
- Trả về `order_id` nếu thành công

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION place_order_with_stock_check(
    p_user_id UUID,
    p_address_id UUID,
    p_items JSONB
)
RETURNS UUID AS $$
DECLARE
    v_order_id UUID;
    v_item JSONB;
    v_product RECORD;
    v_sub_total NUMERIC(12,2) := 0;
BEGIN
    -- Kiểm tra address hợp lệ
    IF NOT EXISTS (
        SELECT 1 FROM user_addresses 
        WHERE id = p_address_id AND user_id = p_user_id
    ) THEN
        RAISE EXCEPTION 'Địa chỉ không hợp lệ';
    END IF;

    v_order_id := uuid_generate_v4();

    -- Duyệt từng item
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        SELECT id, name, base_price, is_available INTO v_product
        FROM products
        WHERE id = (v_item->>'product_id')::UUID;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Sản phẩm không tồn tại: %', v_item->>'product_id';
        END IF;

        IF NOT v_product.is_available THEN
            RAISE EXCEPTION 'Sản phẩm "%" không khả dụng', v_product.name;
        END IF;

        v_sub_total := v_sub_total + v_product.base_price * (v_item->>'quantity')::INT;

        INSERT INTO order_items (id, order_id, product_id, snapshot_product_name, 
                                 snapshot_unit_price, quantity, sub_total)
        VALUES (uuid_generate_v4(), v_order_id, v_product.id, v_product.name,
                v_product.base_price, (v_item->>'quantity')::INT,
                v_product.base_price * (v_item->>'quantity')::INT);
    END LOOP;

    INSERT INTO orders (id, user_id, address_id, sub_total, grand_total, order_status, payment_status)
    VALUES (v_order_id, p_user_id, p_address_id, v_sub_total, v_sub_total, 'PENDING', 'UNPAID');

    RETURN v_order_id;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
-- Test thành công
BEGIN;
SELECT place_order_with_stock_check(
    '22222222-2222-2222-2222-222222222222'::UUID,
    '33333333-3333-3333-3333-333333333333'::UUID,
    '[{"product_id": "f0000000-0000-0000-0000-000000000001", "quantity": 2}]'::JSONB
);
COMMIT;

-- Test thất bại (sản phẩm không tồn tại)
BEGIN;
SELECT place_order_with_stock_check(
    '22222222-2222-2222-2222-222222222222'::UUID,
    '33333333-3333-3333-3333-333333333333'::UUID,
    '[{"product_id": "99999999-9999-9999-9999-999999999999", "quantity": 1}]'::JSONB
);
ROLLBACK;
```

</details>

---

### Bài 1.2: Hủy đơn hàng và hoàn tiền

**Đề bài:** Viết function `cancel_order` để hủy đơn hàng. Phải đảm bảo:
1. Chỉ hủy được đơn hàng có `order_status = 'PENDING'`
2. Cập nhật `order_status = 'CANCELLED'`
3. Nếu đơn đã thanh toán (`payment_status = 'PAID'`) → đánh dấu cần hoàn tiền

**Yêu cầu:**
- Nhận `p_order_id`, `p_reason`
- Kiểm tra trạng thái đơn hàng trước khi hủy
- Trả về `TRUE` nếu thành công

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION cancel_order(
    p_order_id UUID,
    p_reason TEXT DEFAULT 'Khách hàng yêu cầu'
)
RETURNS BOOLEAN AS $$
DECLARE
    v_order RECORD;
BEGIN
    -- Lấy thông tin đơn hàng
    SELECT * INTO v_order FROM orders WHERE id = p_order_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Đơn hàng không tồn tại';
    END IF;

    -- Chỉ hủy được đơn PENDING
    IF v_order.order_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Không thể hủy đơn hàng ở trạng thái "%"', v_order.order_status;
    END IF;

    -- Cập nhật trạng thái
    UPDATE orders SET
        order_status = 'CANCELLED',
        updated_at = NOW()
    WHERE id = p_order_id;

    -- Nếu đã thanh toán → đánh dấu cần hoàn tiền
    IF v_order.payment_status = 'PAID' THEN
        RAISE NOTICE 'Đơn hàng % đã thanh toán, cần hoàn tiền % VNĐ', 
            p_order_id, v_order.grand_total;
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
-- Hủy đơn PENDING
SELECT cancel_order('d0000000-0000-0000-0000-000000000002'::UUID, 'Không muốn mua nữa');

-- Thử hủy đơn COMPLETED → Báo lỗi
SELECT cancel_order('d0000000-0000-0000-0000-000000000001'::UUID);

-- Kiểm tra kết quả
SELECT id, order_status, payment_status FROM orders 
WHERE id = 'd0000000-0000-0000-0000-000000000002';
```

</details>

---

### Bài 1.3: Cập nhật số lượng trong giỏ hàng

**Đề bài:** Viết function `update_cart_quantity` để cập nhật số lượng sản phẩm trong giỏ hàng. Nếu số lượng mới = 0 → xóa sản phẩm khỏi giỏ. Cập nhật lại `total_amount` trong `shopping_sessions`.

**Yêu cầu:**
- Nhận `p_session_id`, `p_product_id`, `p_new_quantity`
- Nếu `p_new_quantity = 0` → DELETE
- Nếu `p_new_quantity > 0` → UPDATE
- Cập nhật `total_amount` trong `shopping_sessions`

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION update_cart_quantity(
    p_session_id UUID,
    p_product_id UUID,
    p_new_quantity INTEGER
)
RETURNS VOID AS $$
DECLARE
    v_product_price NUMERIC(12,2);
BEGIN
    -- Lấy giá sản phẩm
    SELECT base_price INTO v_product_price FROM products WHERE id = p_product_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sản phẩm không tồn tại';
    END IF;

    IF p_new_quantity <= 0 THEN
        -- Xóa sản phẩm khỏi giỏ
        DELETE FROM cart_items 
        WHERE session_id = p_session_id AND product_id = p_product_id;
    ELSE
        -- Cập nhật số lượng
        UPDATE cart_items 
        SET quantity = p_new_quantity
        WHERE session_id = p_session_id AND product_id = p_product_id;

        -- Nếu không có trong giỏ → thêm mới
        IF NOT FOUND THEN
            INSERT INTO cart_items (id, session_id, product_id, quantity)
            VALUES (uuid_generate_v4(), p_session_id, p_product_id, p_new_quantity);
        END IF;
    END IF;

    -- Cập nhật tổng tiền phiên mua sắm
    UPDATE shopping_sessions SET total_amount = (
        SELECT COALESCE(SUM(ci.quantity * p.base_price), 0)
        FROM cart_items ci
        JOIN products p ON ci.product_id = p.id
        WHERE ci.session_id = p_session_id
    )
    WHERE id = p_session_id;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
-- Thêm sản phẩm vào giỏ (hoặc cập nhật số lượng)
SELECT update_cart_quantity(
    '7c9e6679-7425-40de-944b-e07fc1f90ae7'::UUID,
    'f0000000-0000-0000-0000-000000000001'::UUID,
    3
);

-- Kiểm tra giỏ hàng
SELECT * FROM cart_items WHERE session_id = '7c9e6679-7425-40de-944b-e07fc1f90ae7';

-- Xóa sản phẩm khỏi giỏ
SELECT update_cart_quantity(
    '7c9e6679-7425-40de-944b-e07fc1f90ae7'::UUID,
    'f0000000-0000-0000-0000-000000000001'::UUID,
    0
);
```

</details>

---

## BÀI TẬP 2: TRIGGER

### Bài 2.1: Ghi log khi trạng thái đơn hàng thay đổi

**Đề bài:** Tạo trigger `trg_log_order_status_change` để tự động ghi log mỗi khi `order_status` trong bảng `orders` thay đổi.

**Yêu cầu:**
- Tạo bảng `order_status_logs` để lưu log
- Trigger chỉ chạy khi `order_status` thực sự thay đổi
- Lưu cả giá trị cũ (OLD) và giá trị mới (NEW)

<details>
<summary>Đáp án</summary>

```sql
-- Tạo bảng log
CREATE TABLE IF NOT EXISTS order_status_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    changed_at TIMESTAMP DEFAULT NOW(),
    changed_by VARCHAR(100)
);

-- Tạo hàm trigger
CREATE OR REPLACE FUNCTION log_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Chỉ ghi log khi status thực sự thay đổi
    IF OLD.order_status <> NEW.order_status THEN
        INSERT INTO order_status_logs (id, order_id, old_status, new_status, changed_by)
        VALUES (uuid_generate_v4(), NEW.id, OLD.order_status, NEW.order_status, current_user);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Gắn trigger
CREATE TRIGGER trg_log_order_status_change
    AFTER UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION log_order_status_change();
```

**Cách test:**
```sql
-- Cập nhật trạng thái đơn hàng
UPDATE orders SET order_status = 'PROCESSING' 
WHERE id = 'd0000000-0000-0000-0000-000000000002';

-- Kiểm tra log
SELECT * FROM order_status_logs ORDER BY changed_at DESC;

-- Cập nhật thêm lần nữa
UPDATE orders SET order_status = 'COMPLETED' 
WHERE id = 'd0000000-0000-0000-0000-000000000002';

-- Kiểm tra log → Có 2 dòng
SELECT * FROM order_status_logs ORDER BY changed_at DESC;
```

</details>

---

### Bài 2.2: Tự động cập nhật tổng tiền đơn hàng

**Đề bài:** Tạo trigger `trg_update_order_total` để tự động cập nhật `grand_total` trong bảng `orders` mỗi khi có thay đổi trong bảng `order_items` (INSERT, UPDATE, DELETE).

**Yêu cầu:**
- Trigger chạy AFTER INSERT OR UPDATE OR DELETE ON order_items
- Tính lại tổng tiền từ tất cả order_items của đơn hàng đó
- Cập nhật `sub_total` và `grand_total` trong orders

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION update_order_total()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id UUID;
    v_new_total NUMERIC(12,2);
BEGIN
    -- Xác định order_id bị ảnh hưởng
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.order_id;
    ELSE
        v_order_id := NEW.order_id;
    END IF;

    -- Tính tổng tiền mới
    SELECT COALESCE(SUM(sub_total), 0) INTO v_new_total
    FROM order_items
    WHERE order_id = v_order_id;

    -- Cập nhật orders
    UPDATE orders SET
        sub_total = v_new_total,
        grand_total = v_new_total - COALESCE(discount_amount, 0)
    WHERE id = v_order_id;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_order_total
    AFTER INSERT OR UPDATE OR DELETE ON order_items
    FOR EACH ROW EXECUTE FUNCTION update_order_total();
```

**Cách test:**
```sql
-- Xem tổng tiền đơn hàng ban đầu
SELECT id, sub_total, grand_total FROM orders 
WHERE id = 'd0000000-0000-0000-0000-000000000001';

-- Thêm item mới vào đơn hàng
INSERT INTO order_items (id, order_id, product_id, snapshot_product_name, 
                         snapshot_unit_price, quantity, sub_total)
VALUES (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 
        'f0000000-0000-0000-0000-000000000005', 'Americano', 30000, 1, 30000);

-- Kiểm tra → Tổng tiền đã tự động cập nhật
SELECT id, sub_total, grand_total FROM orders 
WHERE id = 'd0000000-0000-0000-0000-000000000001';
```

</details>

---

### Bài 2.3: Kiểm tra rating hợp lệ

**Đề bài:** Tạo trigger `trg_validate_review` để kiểm tra trước khi thêm/sửa đánh giá:
1. `rating_score` phải từ 1 đến 5
2. Không cho phép tự đánh giá sản phẩm của chính mình (kiểm tra qua `user_id`)

**Yêu cầu:**
- Dùng BEFORE INSERT OR UPDATE
- RAISE EXCEPTION nếu vi phạm

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION validate_review()
RETURNS TRIGGER AS $$
BEGIN
    -- Kiểm tra rating_score hợp lệ
    IF NEW.rating_score < 1 OR NEW.rating_score > 5 THEN
        RAISE EXCEPTION 'Điểm đánh giá phải từ 1 đến 5 (hiện tại: %)', NEW.rating_score;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_review
    BEFORE INSERT OR UPDATE ON product_reviews
    FOR EACH ROW EXECUTE FUNCTION validate_review();
```

**Cách test:**
```sql
-- Test rating không hợp lệ (0)
INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text)
VALUES (uuid_generate_v4(), 'f0000000-0000-0000-0000-000000000001', 
        '22222222-2222-2222-2222-222222222222', 0, 'Test');
-- Lỗi: Điểm đánh giá phải từ 1 đến 5

-- Test rating không hợp lệ (6)
INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text)
VALUES (uuid_generate_v4(), 'f0000000-0000-0000-0000-000000000001', 
        '22222222-2222-2222-2222-222222222222', 6, 'Test');
-- Lỗi: Điểm đánh giá phải từ 1 đến 5

-- Test hợp lệ
INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text)
VALUES (uuid_generate_v4(), 'f0000000-0000-0000-0000-000000000001', 
        '22222222-2222-2222-2222-222222222222', 5, 'Ngon!');
-- Thành công
```

</details>

---

### Bài 2.4: Tự động tạo shopping session khi user đăng ký

**Đề bài:** Tạo trigger `trg_create_shopping_session` để tự động tạo `shopping_session` mới mỗi khi có user mới đăng ký (INSERT vào bảng `users`).

**Yêu cầu:**
- Trigger chạy AFTER INSERT ON users
- Tạo shopping_session với `total_amount = 0`

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION create_shopping_session()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO shopping_sessions (id, user_id, total_amount)
    VALUES (uuid_generate_v4(), NEW.id, 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_create_shopping_session
    AFTER INSERT ON users
    FOR EACH ROW EXECUTE FUNCTION create_shopping_session();
```

**Cách test:**
```sql
-- Tạo user mới
INSERT INTO users (id, created_at, updated_at, full_name, username, password, email, phone, role, active)
VALUES (uuid_generate_v4(), NOW(), NOW(), 'Test User', 'testuser', 'hashed_pw', 'test@test.com', '0900000000', 'USER', true);

-- Kiểm tra shopping_session đã được tạo
SELECT * FROM shopping_sessions 
WHERE user_id = (SELECT id FROM users WHERE username = 'testuser');
```

</details>

---

## BÀI TẬP 3: FUNCTION

### Bài 3.1: Tính giá trị vòng đời khách hàng (CLV)

**Đề bài:** Viết function `get_customer_lifetime_value` để tính tổng giá trị đơn hàng của một khách hàng, chỉ tính đơn `COMPLETED`.

**Yêu cầu:**
- Nhận `p_user_id`
- Trả về: `total_orders`, `total_spent`, `avg_order_value`, `first_order_date`, `last_order_date`

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION get_customer_lifetime_value(p_user_id UUID)
RETURNS TABLE(
    total_orders BIGINT,
    total_spent NUMERIC(14,2),
    avg_order_value NUMERIC(10,2),
    first_order_date TIMESTAMP,
    last_order_date TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*) AS total_orders,
        COALESCE(SUM(o.grand_total), 0) AS total_spent,
        COALESCE(AVG(o.grand_total), 0) AS avg_order_value,
        MIN(o.created_at) AS first_order_date,
        MAX(o.created_at) AS last_order_date
    FROM orders o
    WHERE o.user_id = p_user_id
      AND o.order_status = 'COMPLETED';
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
-- Xem CLV của user1
SELECT * FROM get_customer_lifetime_value('22222222-2222-2222-2222-222222222222');

-- Xem CLV của user2
SELECT * FROM get_customer_lifetime_value('33333333-3333-3333-3333-333333333333');
```

</details>

---

### Bài 3.2: Tìm sản phẩm bán chạy theo danh mục

**Đề bài:** Viết function `get_top_products_by_category` để lấy top N sản phẩm bán chạy nhất trong một danh mục.

**Yêu cầu:**
- Nhận `p_category_id`, `p_limit` (mặc định 5)
- Trả về: `product_id`, `product_name`, `total_quantity_sold`, `total_revenue`, `avg_rating`
- Sắp xếp theo số lượng bán giảm dần

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION get_top_products_by_category(
    p_category_id UUID,
    p_limit INTEGER DEFAULT 5
)
RETURNS TABLE(
    product_id UUID,
    product_name VARCHAR,
    total_quantity_sold BIGINT,
    total_revenue NUMERIC(14,2),
    avg_rating NUMERIC(3,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id AS product_id,
        p.name AS product_name,
        COALESCE(SUM(oi.quantity), 0) AS total_quantity_sold,
        COALESCE(SUM(oi.sub_total), 0) AS total_revenue,
        p.avg_rating
    FROM products p
    LEFT JOIN order_items oi ON p.id = oi.product_id
    LEFT JOIN orders o ON oi.order_id = o.id AND o.order_status = 'COMPLETED'
    WHERE p.category_id = p_category_id
    GROUP BY p.id, p.name, p.avg_rating
    ORDER BY total_quantity_sold DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
-- Top 3 sản phẩm bán chạy trong danh mục Coffee
SELECT * FROM get_top_products_by_category(
    'c0000000-0000-0000-0000-000000000001'::UUID, 3
);

-- Top sản phẩm trong danh mục Tea
SELECT * FROM get_top_products_by_category(
    'c0000000-0000-0000-0000-000000000002'::UUID
);
```

</details>

---

### Bài 3.3: Thống kê đơn hàng theo trạng thái

**Đề bài:** Viết function `get_order_stats_by_status` để thống kê số lượng và tổng tiền đơn hàng theo từng trạng thái.

**Yêu cầu:**
- Trả về: `order_status`, `order_count`, `total_amount`, `percentage`
- Phần trăm tính trên tổng số đơn hàng

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION get_order_stats_by_status()
RETURNS TABLE(
    order_status VARCHAR,
    order_count BIGINT,
    total_amount NUMERIC(14,2),
    percentage NUMERIC(5,2)
) AS $$
DECLARE
    v_total_orders BIGINT;
BEGIN
    -- Lấy tổng số đơn hàng
    SELECT COUNT(*) INTO v_total_orders FROM orders;

    RETURN QUERY
    SELECT
        o.order_status,
        COUNT(*) AS order_count,
        COALESCE(SUM(o.grand_total), 0) AS total_amount,
        ROUND(COUNT(*) * 100.0 / NULLIF(v_total_orders, 0), 2) AS percentage
    FROM orders o
    GROUP BY o.order_status
    ORDER BY order_count DESC;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
SELECT * FROM get_order_stats_by_status();
```

</details>

---

### Bài 3.4: Tìm khách hàng VIP

**Đề bài:** Viết function `find_vip_customers` để tìm khách hàng VIP (tổng chi tiêu > ngưỡng và có nhiều đơn hàng).

**Yêu cầu:**
- Nhận `p_min_spent` (tổng chi tiêu tối thiểu), `p_min_orders` (số đơn tối thiểu)
- Trả về: `user_id`, `username`, `total_orders`, `total_spent`, `avg_order_value`
- Chỉ tính đơn `COMPLETED`

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION find_vip_customers(
    p_min_spent NUMERIC DEFAULT 100000,
    p_min_orders INTEGER DEFAULT 2
)
RETURNS TABLE(
    user_id UUID,
    username VARCHAR,
    total_orders BIGINT,
    total_spent NUMERIC(14,2),
    avg_order_value NUMERIC(10,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.id AS user_id,
        u.username,
        COUNT(o.id) AS total_orders,
        COALESCE(SUM(o.grand_total), 0) AS total_spent,
        COALESCE(AVG(o.grand_total), 0) AS avg_order_value
    FROM users u
    JOIN orders o ON u.id = o.user_id AND o.order_status = 'COMPLETED'
    GROUP BY u.id, u.username
    HAVING COALESCE(SUM(o.grand_total), 0) >= p_min_spent
       AND COUNT(o.id) >= p_min_orders
    ORDER BY total_spent DESC;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
-- Tìm khách hàng chi tiêu > 100k và có >= 2 đơn
SELECT * FROM find_vip_customers(100000, 2);

-- Tìm khách hàng chi tiêu > 50k
SELECT * FROM find_vip_customers(50000);
```

</details>

---

## BÀI TẬP 4: VIEW

### Bài 4.1: Dashboard sản phẩm

**Đề bài:** Tạo view `vw_product_dashboard` để hiển thị thông tin tổng quan về sản phẩm.

**Yêu cầu:**
- `product_id`, `product_name`, `category_name`, `base_price`, `is_available`
- `total_sold` (tổng số lượng đã bán)
- `total_revenue` (tổng doanh thu)
- `avg_rating`, `review_count`
- `last_sold_at` (lần bán cuối)

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE VIEW vw_product_dashboard AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    c.name_vi AS category_name,
    p.base_price,
    p.is_available,
    COALESCE(SUM(oi.quantity), 0) AS total_sold,
    COALESCE(SUM(oi.sub_total), 0) AS total_revenue,
    p.avg_rating,
    p.review_count,
    MAX(o.created_at) AS last_sold_at
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN order_items oi ON p.id = oi.product_id
LEFT JOIN orders o ON oi.order_id = o.id AND o.order_status = 'COMPLETED'
GROUP BY p.id, p.name, c.name_vi, p.base_price, p.is_available, p.avg_rating, p.review_count;
```

**Cách test:**
```sql
SELECT * FROM vw_product_dashboard ORDER BY total_revenue DESC;

-- Lọc sản phẩm bán chạy
SELECT * FROM vw_product_dashboard WHERE total_sold > 0;

-- Lọc theo danh mục
SELECT * FROM vw_product_dashboard WHERE category_name = 'Cà phê';
```

</details>

---

### Bài 4.2: Báo cáo doanh thu theo danh mục

**Đề bài:** Tạo view `vw_revenue_by_category` để hiển thị doanh thu theo từng danh mục sản phẩm.

**Yêu cầu:**
- `category_name`, `total_products`, `total_sold`, `total_revenue`, `avg_rating`
- Chỉ tính đơn `COMPLETED`

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE VIEW vw_revenue_by_category AS
SELECT
    c.name_vi AS category_name,
    COUNT(DISTINCT p.id) AS total_products,
    COALESCE(SUM(oi.quantity), 0) AS total_sold,
    COALESCE(SUM(oi.sub_total), 0) AS total_revenue,
    ROUND(AVG(p.avg_rating), 2) AS avg_rating
FROM categories c
LEFT JOIN products p ON c.id = p.category_id
LEFT JOIN order_items oi ON p.id = oi.product_id
LEFT JOIN orders o ON oi.order_id = o.id AND o.order_status = 'COMPLETED'
GROUP BY c.id, c.name_vi
ORDER BY total_revenue DESC;
```

**Cách test:**
```sql
SELECT * FROM vw_revenue_by_category;
```

</details>

---

### Bài 4.3: Thông tin khách hàng chi tiết

**Đề bài:** Tạo view `vw_customer_details` để hiển thị thông tin chi tiết về khách hàng.

**Yêu cầu:**
- `user_id`, `username`, `full_name`, `email`, `phone`
- `total_orders`, `total_spent`, `avg_order_value`
- `default_address`
- `last_order_date`

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE VIEW vw_customer_details AS
SELECT
    u.id AS user_id,
    u.username,
    u.full_name,
    u.email,
    u.phone,
    COUNT(o.id) AS total_orders,
    COALESCE(SUM(o.grand_total), 0) AS total_spent,
    COALESCE(AVG(o.grand_total), 0) AS avg_order_value,
    ua.address_line AS default_address,
    MAX(o.created_at) AS last_order_date
FROM users u
LEFT JOIN orders o ON u.id = o.user_id AND o.order_status = 'COMPLETED'
LEFT JOIN user_addresses ua ON u.id = ua.user_id AND ua.is_default = true
WHERE u.role = 'USER'
GROUP BY u.id, u.username, u.full_name, u.email, u.phone, ua.address_line;
```

**Cách test:**
```sql
SELECT * FROM vw_customer_details ORDER BY total_spent DESC;

-- Tìm khách hàng chi tiêu nhiều
SELECT * FROM vw_customer_details WHERE total_spent > 100000;
```

</details>

---

## BÀI TẬP 5: BÀI TẬP TỔNG HỢP

### Bài 5.1: Hệ thống khuyến mãi tự động

**Đề bài:** Tạo hệ thống khuyến mãi tự động:
1. Function `check_promotion_eligibility` kiểm tra đơn hàng có đủ điều kiện áp dụng khuyến mãi không
2. Trigger `trg_auto_apply_promotion` tự động áp dụng khi tạo đơn hàng mới

**Yêu cầu:**
- Kiểm tra: `sub_total >= min_order_value`, promotion còn hiệu lực
- Áp dụng: cập nhật `discount_amount` và `grand_total`

<details>
<summary>Đáp án</summary>

```sql
-- Function kiểm tra điều kiện
CREATE OR REPLACE FUNCTION check_promotion_eligibility(
    p_order_id UUID,
    p_promotion_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    v_order RECORD;
    v_promo RECORD;
BEGIN
    SELECT * INTO v_order FROM orders WHERE id = p_order_id;
    SELECT * INTO v_promo FROM promotions WHERE id = p_promotion_id;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    -- Kiểm tra promotion còn hiệu lực
    IF NOT v_promo.is_active THEN
        RETURN FALSE;
    END IF;

    IF CURRENT_TIMESTAMP NOT BETWEEN v_promo.start_date AND v_promo.end_date THEN
        RETURN FALSE;
    END IF;

    -- Kiểm tra đơn hàng đủ điều kiện
    IF v_order.sub_total < v_promo.min_order_value THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function áp dụng khuyến mãi
CREATE OR REPLACE FUNCTION apply_promotion_to_order()
RETURNS TRIGGER AS $$
DECLARE
    v_promo RECORD;
    v_discount NUMERIC(12,2) := 0;
BEGIN
    -- Chỉ áp dụng khi có promotion_id
    IF NEW.promotion_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT * INTO v_promo FROM promotions WHERE id = NEW.promotion_id;

    IF NOT FOUND OR NOT v_promo.is_active THEN
        RETURN NEW;
    END IF;

    IF CURRENT_TIMESTAMP NOT BETWEEN v_promo.start_date AND v_promo.end_date THEN
        RETURN NEW;
    END IF;

    IF NEW.sub_total >= v_promo.min_order_value THEN
        IF v_promo.discount_type = 'PERCENTAGE' THEN
            v_discount := NEW.sub_total * v_promo.discount_value / 100;
        ELSE
            v_discount := v_promo.discount_value;
        END IF;

        NEW.discount_amount := v_discount;
        NEW.grand_total := NEW.sub_total - v_discount;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auto_apply_promotion
    BEFORE INSERT ON orders
    FOR EACH ROW EXECUTE FUNCTION apply_promotion_to_order();
```

**Cách test:**
```sql
-- Tạo promotion mẫu
INSERT INTO promotions (id, discount_type, discount_value, min_order_value, start_date, end_date, is_active)
VALUES (uuid_generate_v4(), 'PERCENTAGE', 10, 50000, NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days', true);

-- Tạo đơn hàng với promotion_id
-- (Cần chạy trong transaction)
```

</details>

---

### Bài 5.2: Hệ thống audit trail

**Đề bài:** Tạo hệ thống audit trail để ghi lại tất cả thay đổi trên bảng `products`.

**Yêu cầu:**
- Tạo bảng `product_audit_logs`
- Trigger ghi log khi INSERT, UPDATE, DELETE
- Lưu cả giá trị cũ và mới

<details>
<summary>Đáp án</summary>

```sql
-- Tạo bảng audit
CREATE TABLE IF NOT EXISTS product_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL,
    action VARCHAR(10) NOT NULL,  -- INSERT, UPDATE, DELETE
    old_data JSONB,
    new_data JSONB,
    changed_at TIMESTAMP DEFAULT NOW(),
    changed_by VARCHAR(100)
);

-- Tạo hàm trigger
CREATE OR REPLACE FUNCTION audit_product_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO product_audit_logs (id, product_id, action, new_data, changed_by)
        VALUES (uuid_generate_v4(), NEW.id, 'INSERT', to_jsonb(NEW), current_user);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO product_audit_logs (id, product_id, action, old_data, new_data, changed_by)
        VALUES (uuid_generate_v4(), NEW.id, 'UPDATE', to_jsonb(OLD), to_jsonb(NEW), current_user);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO product_audit_logs (id, product_id, action, old_data, changed_by)
        VALUES (uuid_generate_v4(), OLD.id, 'DELETE', to_jsonb(OLD), current_user);
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Gắn trigger
CREATE TRIGGER trg_audit_product_changes
    AFTER INSERT OR UPDATE OR DELETE ON products
    FOR EACH ROW EXECUTE FUNCTION audit_product_changes();
```

**Cách test:**
```sql
-- Thêm sản phẩm mới
INSERT INTO products (id, created_at, updated_at, name, name_vi, category_id, base_price, is_available)
VALUES (uuid_generate_v4(), NOW(), NOW(), 'Test Product', 'Sản phẩm test', 
        'c0000000-0000-0000-0000-000000000001', 25000, true);

-- Cập nhật giá
UPDATE products SET base_price = 30000 WHERE name = 'Test Product';

-- Xóa sản phẩm
DELETE FROM products WHERE name = 'Test Product';

-- Kiểm tra audit log
SELECT * FROM product_audit_logs ORDER BY changed_at DESC;
```

</details>

---

## BÀI TẬP 6: BÀI TẬP NÂNG CAO

### Bài 6.1: Function tìm kiếm sản phẩm linh hoạt

**Đề bài:** Viết function `search_products` để tìm kiếm sản phẩm với nhiều tiêu chí lọc.

**Yêu cầu:**
- Nhận các tham số OPTIONAL: `p_keyword`, `p_category_id`, `p_min_price`, `p_max_price`, `p_min_rating`
- Tìm kiếm linh hoạt (chỉ lọc theo tham số được truyền)
- Trả về danh sách sản phẩm phù hợp

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION search_products(
    p_keyword TEXT DEFAULT NULL,
    p_category_id UUID DEFAULT NULL,
    p_min_price NUMERIC DEFAULT NULL,
    p_max_price NUMERIC DEFAULT NULL,
    p_min_rating NUMERIC DEFAULT NULL
)
RETURNS TABLE(
    product_id UUID,
    product_name VARCHAR,
    category_name VARCHAR,
    base_price NUMERIC,
    avg_rating NUMERIC,
    review_count INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id AS product_id,
        p.name AS product_name,
        c.name_vi AS category_name,
        p.base_price,
        p.avg_rating,
        p.review_count
    FROM products p
    LEFT JOIN categories c ON p.category_id = c.id
    WHERE p.is_available = true
      AND (p_keyword IS NULL OR p.name ILIKE '%' || p_keyword || '%')
      AND (p_category_id IS NULL OR p.category_id = p_category_id)
      AND (p_min_price IS NULL OR p.base_price >= p_min_price)
      AND (p_max_price IS NULL OR p.base_price <= p_max_price)
      AND (p_min_rating IS NULL OR p.avg_rating >= p_min_rating)
    ORDER BY p.avg_rating DESC NULLS LAST;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
-- Tìm tất cả sản phẩm
SELECT * FROM search_products();

-- Tìm theo keyword
SELECT * FROM search_products(p_keyword := 'Latte');

-- Tìm theo khoảng giá
SELECT * FROM search_products(p_min_price := 40000, p_max_price := 60000);

-- Tìm theo rating
SELECT * FROM search_products(p_min_rating := 4.8);

-- Kết hợp nhiều tiêu chí
SELECT * FROM search_products(
    p_keyword := 'Cà',
    p_min_price := 30000,
    p_min_rating := 4.5
);
```

</details>

---

### Bài 6.2: Procedure xử lý đơn hàng hoàn chỉnh

**Đề bài:** Viết procedure `process_order` để xử lý đơn hàng hoàn chỉnh: đặt hàng → thanh toán → cập nhật trạng thái.

**Yêu cầu:**
- Nhận: `p_user_id`, `p_address_id`, `p_items` (JSONB), `p_payment_method`
- Thực hiện: tạo đơn hàng, cập nhật payment_status = 'PAID', order_status = 'COMPLETED'
- Nếu có lỗi → rollback toàn bộ

<details>
<summary>Đáp án</summary>

```sql
CREATE OR REPLACE FUNCTION process_order(
    p_user_id UUID,
    p_address_id UUID,
    p_items JSONB,
    p_payment_method VARCHAR DEFAULT 'CASH'
)
RETURNS UUID AS $$
DECLARE
    v_order_id UUID;
    v_item JSONB;
    v_product RECORD;
    v_sub_total NUMERIC(12,2) := 0;
BEGIN
    -- Kiểm tra address
    IF NOT EXISTS (
        SELECT 1 FROM user_addresses 
        WHERE id = p_address_id AND user_id = p_user_id
    ) THEN
        RAISE EXCEPTION 'Địa chỉ không hợp lệ';
    END IF;

    v_order_id := uuid_generate_v4();

    -- Xử lý từng item
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        SELECT id, name, base_price, is_available INTO v_product
        FROM products
        WHERE id = (v_item->>'product_id')::UUID;

        IF NOT FOUND OR NOT v_product.is_available THEN
            RAISE EXCEPTION 'Sản phẩm không khả dụng: %', v_item->>'product_id';
        END IF;

        v_sub_total := v_sub_total + v_product.base_price * (v_item->>'quantity')::INT;

        INSERT INTO order_items (id, order_id, product_id, snapshot_product_name,
                                 snapshot_unit_price, quantity, sub_total)
        VALUES (uuid_generate_v4(), v_order_id, v_product.id, v_product.name,
                v_product.base_price, (v_item->>'quantity')::INT,
                v_product.base_price * (v_item->>'quantity')::INT);
    END LOOP;

    -- Tạo đơn hàng với trạng thái COMPLETED
    INSERT INTO orders (id, user_id, address_id, sub_total, grand_total,
                        order_status, payment_method, payment_status)
    VALUES (v_order_id, p_user_id, p_address_id, v_sub_total, v_sub_total,
            'COMPLETED', p_payment_method, 'PAID');

    -- Xóa giỏ hàng
    DELETE FROM cart_items
    WHERE session_id IN (
        SELECT id FROM shopping_sessions WHERE user_id = p_user_id
    );

    -- Reset session
    UPDATE shopping_sessions SET total_amount = 0 WHERE user_id = p_user_id;

    RETURN v_order_id;
END;
$$ LANGUAGE plpgsql;
```

**Cách test:**
```sql
BEGIN;
SELECT process_order(
    '22222222-2222-2222-2222-222222222222'::UUID,
    '33333333-3333-3333-3333-333333333333'::UUID,
    '[{"product_id": "f0000000-0000-0000-0000-000000000001", "quantity": 2}]'::JSONB,
    'CASH'
);
COMMIT;

-- Kiểm tra
SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;
```

</details>

---

## ĐÁP ÁN TÓM TẮT

| Bài | Chủ đề | Đối tượng DBMS | Độ khó |
|-----|--------|----------------|--------|
| 1.1 | Đặt hàng + kiểm tra tồn kho | Transaction + Function | Dễ |
| 1.2 | Hủy đơn hàng | Transaction + Function | Dễ |
| 1.3 | Cập nhật giỏ hàng | Transaction + Function | Trung bình |
| 2.1 | Log trạng thái đơn hàng | Trigger | Dễ |
| 2.2 | Cập nhật tổng tiền tự động | Trigger | Trung bình |
| 2.3 | Kiểm tra rating hợp lệ | Trigger | Dễ |
| 2.4 | Tạo session khi đăng ký | Trigger | Dễ |
| 3.1 | Tính CLV khách hàng | Function | Trung bình |
| 3.2 | Sản phẩm bán chạy theo danh mục | Function | Trung bình |
| 3.3 | Thống kê theo trạng thái | Function | Trung bình |
| 3.4 | Tìm khách hàng VIP | Function | Trung bình |
| 4.1 | Dashboard sản phẩm | View | Dễ |
| 4.2 | Doanh thu theo danh mục | View | Dễ |
| 4.3 | Thông tin khách hàng | View | Dễ |
| 5.1 | Hệ thống khuyến mãi tự động | Trigger + Function | Khó |
| 5.2 | Audit trail | Trigger | Trung bình |
| 6.1 | Tìm kiếm linh hoạt | Function | Khó |
| 6.2 | Xử lý đơn hàng hoàn chỉnh | Transaction + Function | Khó |
