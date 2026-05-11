# Hướng dẫn Deploy dự án Hashiji Cafe

Tài liệu này hướng dẫn deploy ứng dụng Hashiji Cafe theo 2 cách:
1. **Docker** (Khuyến khích) - Nhanh, đơn giản, chạy được ngay
2. **Cài đặt thủ công** - Phù hợp khi muốn tùy chỉnh hoặc debug

---

# PHẦN 1: DEPLOY BẰNG DOCKER (Khuyến khích)

## 1.1. Yêu cầu hệ thống

- Đã cài **Docker Desktop** (Mac/Windows/Linux)
  - Mac: Tải từ https://www.docker.com/products/docker-desktop/
  - Windows: Tải từ https://www.docker.com/products/docker-desktop/
  - Linux: Cài Docker Engine + Docker Compose
- Kiểm tra Docker đã cài chưa:
  ```bash
  docker --version        # VD: Docker version 24.0.7
  docker compose version  # VD: Docker Compose version v2.23.3
  ```

## 1.2. Cấu trúc file Docker

```
Hashiji-Cafe/
├── Dockerfile              # Build ứng dụng Spring Boot
├── docker-compose.yml      # Định nghĩa các service
└── src/main/resources/
    ├── schema-advanced.sql  # Schema + Functions + Triggers
    └── seed-data.sql        # Dữ liệu mẫu
```

### Giải thích docker-compose.yml

```yaml
services:
  postgres:      # PostgreSQL 15 - Cơ sở dữ liệu
  pgadmin:       # pgAdmin 4 - Giao diện quản trị DB
  hashiji-app:   # Spring Boot App - Backend + Frontend
```

## 1.3. Các bước deploy

### Bước 1: Clone project (nếu chưa có)

```bash
git clone <repository-url>
cd Hashiji-Cafe
```

### Bước 2: Kiểm tra file Docker

Đảm bảo có đủ 2 file:
```bash
ls -la Dockerfile docker-compose.yml
# Phải thấy cả 2 file
```

### Bước 3: Build và chạy Docker

```bash
# Build image và chạy tất cả service
docker compose up --build -d
```

**Giải thích:**
- `up`: Khởi động tất cả service
- `--build`: Build lại image (chạy lần đầu hoặc khi code thay đổi)
- `-d`: Chạy ở chế độ background (detach)

### Bước 4: Kiểm tra trạng thái

```bash
# Xem danh sách container đang chạy
docker compose ps
```

Kết quả mong đợi:
```
NAME                STATUS          PORTS
hashiji-postgres    Up (healthy)    0.0.0.0:5432->5432/tcp
hashiji-pgadmin     Up              0.0.0.0:5050->80/tcp
hashiji-app         Up              0.0.0.0:8080->8080/tcp
```

### Bước 5: Kiểm tra logs (nếu có lỗi)

```bash
# Xem logs tất cả service
docker compose logs

# Xem logs riêng từng service
docker compose logs postgres
docker compose logs hashiji-app

# Xem logs real-time (theo dõi liên tục)
docker compose logs -f hashiji-app
```

### Bước 6: Validate ứng dụng hoạt động

#### 6a. Truy cập ứng dụng web

Mở trình duyệt: http://localhost:8080

Đăng nhập với tài khoản:
- **Admin**: `admin` / `password`
- **Nhân viên**: `staff1` / `password`
- **Khách hàng**: `user1` / `password` hoặc `user2` / `password`

#### 6b. Truy cập pgAdmin (quản trị database)

Mở trình duyệt: http://localhost:5050

Đăng nhập:
- **Email**: `admin@hashiji.cafe`
- **Password**: `admin123`

Kết nối database trong pgAdmin:
1. Right-click **Servers** > **Register** > **Server...**
2. Tab **General**: Name nhập `Hashiji DB`
3. Tab **Connection**:
   - Host: `postgres` (tên service trong docker-compose)
   - Port: `5432`
   - Database: `cafe_db`
   - Username: `cafe_admin`
   - Password: `123`
4. Nhấn **Save**

#### 6c. Kiểm tra database trực tiếp

```bash
# Vào shell của container postgres
docker compose exec postgres psql -U cafe_admin -d cafe_db

# Kiểm tra bảng đã tạo
\dt

# Kiểm tra dữ liệu mẫu
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM users;

# Kiểm tra triggers đã tạo
SELECT trigger_name, event_object_table 
FROM information_schema.triggers 
WHERE trigger_schema = 'public';

# Thoát
\q
```

#### 6d. Test Transaction/Trigger/Procedure

```sql
-- Vào psql trong container
docker compose exec postgres psql -U cafe_admin -d cafe_db

-- Test 1: Xem sản phẩm
SELECT name, base_price, avg_rating FROM products;

-- Test 2: Thêm review → Trigger tự động cập nhật rating
INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text)
VALUES (uuid_generate_v4(), 'f0000000-0000-0000-0000-000000000001', 
        '22222222-2222-2222-2222-222222222222', 5, 'Rất ngon!');

-- Kiểm tra rating đã tự động cập nhật chưa
SELECT name, avg_rating, review_count FROM products 
WHERE id = 'f0000000-0000-0000-0000-000000000001';

-- Test 3: Gọi procedure đặt hàng
BEGIN;
SELECT place_order(
    '22222222-2222-2222-2222-222222222222'::UUID,
    '33333333-3333-3333-3333-333333333333'::UUID,
    NULL,
    '[{"product_id": "f0000000-0000-0000-0000-000000000001", "quantity": 2}]'::JSONB
);
COMMIT;

-- Kiểm tra đơn hàng mới
SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;
```

## 1.4. Các lệnh Docker thường dùng

```bash
# Khởi động
docker compose up -d              # Chạy background
docker compose up --build -d      # Build lại và chạy

# Dừng và xóa
docker compose down               # Dừng container
docker compose down -v            # Dừng + xóa volume (mất dữ liệu DB)

# Xem trạng thái
docker compose ps                 # Danh sách container
docker compose logs -f            # Logs real-time

# Vào shell container
docker compose exec postgres bash      # Vào shell container postgres
docker compose exec postgres psql -U cafe_admin -d cafe_db  # Vào psql

# Rebuild riêng app (khi code thay đổi)
docker compose build hashiji-app
docker compose up -d hashiji-app
```

## 1.5. Xử lý lỗi thường gặp

### Lỗi: Port 5432 đã bị chiếm

```
Error: bind: address already in use
```

**Cách sửa:**
```bash
# Tìm process đang dùng port 5432
lsof -i :5432

# Hoặc đổi port trong docker-compose.yml
ports:
  - "5433:5432"  # Đổi 5432 thành 5433
```

### Lỗi: Container không khởi động

```bash
# Xem logs để biết nguyên nhân
docker compose logs postgres

# Khởi động lại
docker compose down
docker compose up -d
```

### Lỗi: Mất dữ liệu sau khi restart

Dữ liệu được lưu trong Docker volume `pgdata`. Nếu chạy `docker compose down -v` sẽ mất dữ liệu.

```bash
# Xem volume
docker volume ls

# Backup dữ liệu trước khi xóa
docker compose exec postgres pg_dump -U cafe_admin cafe_db > backup.sql
```

---

# PHẦN 2: CÀI ĐẶT THỦ CÔNG (Local Deployment)

## 2.1. Yêu cầu hệ thống

- **PostgreSQL 13** trở lên (khuyến nghị 15)
- **Java 17** (JDK)
- **Maven 3.8+** (hoặc dùng Maven Wrapper trong project)
- Extension: `uuid-ossp` (hỗ trợ mã định danh UUID)
- Công cụ quản lý DB: **psql**, **pgAdmin** hoặc **DBeaver**

### Kiểm tra đã cài đặt chưa

```bash
# Kiểm tra PostgreSQL
psql --version          # VD: psql (PostgreSQL) 15.4

# Kiểm tra Java
java -version           # VD: openjdk version "17.0.x"

# Kiểm tra Maven (nếu cài riêng)
mvn -version            # VD: Apache Maven 3.8.x
```

## 2.2. Cài đặt PostgreSQL

### Mac (Homebrew)

```bash
# Cài PostgreSQL 15
brew install postgresql@15

# Khởi động service
brew services start postgresql@15

# Thêm vào PATH (nếu lệnh psql không tìm thấy)
echo 'export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Windows

1. Tải PostgreSQL 15 từ: https://www.postgresql.org/download/windows/
2. Chạy installer, ghi nhớ password của user `postgres`
3. Thêm `C:\Program Files\PostgreSQL\15\bin` vào biến môi trường PATH

### Ubuntu/Debian

```bash
# Thêm repository PostgreSQL
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt-get update

# Cài PostgreSQL 15
sudo apt-get install postgresql-15

# Khởi động service
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

## 2.3. Các bước cài đặt chi tiết

### Bước 1: Tạo Database và User

#### Cách 1: Dùng lệnh psql (Khuyến khích)

```bash
# Đăng nhập vào PostgreSQL bằng quyền superuser
psql -U postgres
```

Sau khi vào giao diện lệnh (`postgres=#`), chạy lần lượt:

```sql
-- 1. Tạo Database
CREATE DATABASE cafe_db;

-- 2. Tạo User với mật khẩu
CREATE USER cafe_admin WITH ENCRYPTED PASSWORD '123';

-- 3. Cấp quyền cho User trên Database
GRANT ALL PRIVILEGES ON DATABASE cafe_db TO cafe_admin;

-- 4. Chuyển sang kết nối tới database vừa tạo
\c cafe_db

-- 5. Cấp quyền schema public cho user (bắt buộc với Postgres 15+)
GRANT ALL ON SCHEMA public TO cafe_admin;

-- 6. Cấp quyền tạo object trong schema public
GRANT CREATE ON SCHEMA public TO cafe_admin;

-- Thoát
\q
```

#### Cách 2: Dùng giao diện pgAdmin

1. Mở **pgAdmin** và kết nối vào server cục bộ
2. **Tạo User:**
   - Chuột phải vào **Login/Group Roles** > **Create** > **Login/Group Role...**
   - Tab **General**: Name nhập `cafe_admin`
   - Tab **Definition**: Password nhập `123`
   - Tab **Privileges**: Bật **Can login?**
   - Nhấn **Save**
3. **Tạo Database:**
   - Chuột phải vào **Databases** > **Create** > **Database...**
   - Tab **General**: Database nhập `cafe_db`, Owner chọn `cafe_admin`
   - Nhấn **Save**
4. **Cấp quyền Schema:**
   - Mở **Query Tool** trên database `cafe_db`
   - Chạy lệnh:
     ```sql
     GRANT ALL ON SCHEMA public TO cafe_admin;
     GRANT CREATE ON SCHEMA public TO cafe_admin;
     ```

### Bước 2: Khởi tạo cấu trúc bảng (Schema)

Đăng nhập vào database `cafe_db` và chạy các lệnh sau theo đúng thứ tự.

```bash
psql -U cafe_admin -d cafe_db
```

#### 2a. Kích hoạt Extension UUID

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

> Nếu gặp lỗi, thử: `CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;`

#### 2b. Tạo toàn bộ bảng

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

### Bước 3: Cài đặt Functions & Triggers

Chạy file `src/main/resources/schema-advanced.sql` để tạo các đối tượng nâng cao:
- **Function `place_order`**: Logic đặt hàng (kiểm tra địa chỉ, tính tổng, áp dụng KM)
- **Function `get_revenue_report`**: Thống kê doanh thu theo ngày
- **Trigger `trg_update_product_rating`**: Tự động cập nhật điểm đánh giá sản phẩm
- **Trigger `trg_log_cart_behavior`**: Ghi log hành vi ADD_TO_CART
- **Trigger `trg_single_default_address`**: Đảm bảo chỉ 1 địa chỉ mặc định mỗi user

**Cách 1: Chạy trực tiếp file SQL**

```bash
psql -U cafe_admin -d cafe_db -f src/main/resources/schema-advanced.sql
```

**Cách 2: Copy-paste nội dung file**

1. Mở file `src/main/resources/schema-advanced.sql`
2. Copy toàn bộ nội dung
3. Dán vào psql hoặc Query Tool của pgAdmin
4. Chạy (nhấn Enter trong psql, hoặc F5 trong pgAdmin)

**Kết quả mong đợi:**
```
CREATE EXTENSION
CREATE FUNCTION
CREATE FUNCTION
CREATE FUNCTION
CREATE TRIGGER
CREATE TRIGGER
CREATE TRIGGER
```

### Bước 4: Nạp dữ liệu mẫu (Seed Data)

File `seed-data.sql` chứa dữ liệu mẫu cho:
- Users (admin, staff, khách hàng)
- Categories (Coffee, Tea, Smoothie, Cake)
- Products (Espresso, Latte, Peach Tea, ...)
- Orders và Order Items mẫu

```bash
# Chạy từ terminal:
psql -U cafe_admin -d cafe_db -f src/main/resources/seed-data.sql
```

Hoặc copy-paste nội dung file vào psql/pgAdmin như Bước 3.

**Kết quả mong đợi:**
```
INSERT 0 4    -- 4 users
INSERT 0 4    -- 4 categories
INSERT 0 8    -- 8 products
INSERT 0 5    -- 5 orders
INSERT 0 ...  -- order items
```

### Bước 5: Kiểm tra database đã tạo đúng chưa

```bash
# Đăng nhập vào database
psql -U cafe_admin -d cafe_db
```

```sql
-- Kiểm tra bảng đã tạo
\dt
-- Phải thấy: users, products, categories, orders, order_items, 
--            product_reviews, user_addresses, shopping_sessions, 
--            cart_items, user_behavior_logs, promotions

-- Kiểm tra dữ liệu mẫu
SELECT COUNT(*) FROM users;       -- Kết quả: 4
SELECT COUNT(*) FROM products;    -- Kết quả: 8
SELECT COUNT(*) FROM orders;      -- Kết quả: 5

-- Kiểm tra functions đã tạo
SELECT routine_name FROM information_schema.routines 
WHERE routine_schema = 'public';
-- Phải thấy: place_order, get_revenue_report, update_product_rating, ...

-- Kiểm tra triggers đã tạo
SELECT trigger_name, event_object_table 
FROM information_schema.triggers 
WHERE trigger_schema = 'public';
-- Phải thấy: trg_update_product_rating, trg_log_cart_behavior, ...

-- Thoát
\q
```

### Bước 6: Cấu hình Spring Boot

File cấu hình: `src/main/resources/application.properties` (hoặc `application-dev.properties`)

Đảm bảo các thông số kết nối đúng:

```properties
# Kết nối database
spring.datasource.url=jdbc:postgresql://localhost:5432/cafe_db
spring.datasource.username=cafe_admin
spring.datasource.password=123

# Hibernate - KHÔNG dùng ddl-auto=create (sẽ mất functions/triggers)
spring.jpa.hibernate.ddl-auto=update

# Hiển thị SQL khi debug (tùy chọn)
spring.jpa.show-sql=true
```

**Lưu ý quan trọng:**
- `ddl-auto=update`: Chỉ cập nhật schema, KHÔNG drop bảng cũ
- KHÔNG dùng `ddl-auto=create` hoặc `ddl-auto=create-drop` vì sẽ mất functions và triggers
- Nếu muốn reset database hoàn toàn, chạy lại file SQL thủ công

### Bước 7: Build và chạy ứng dụng

#### Cách 1: Dùng Maven Wrapper (Khuyến khích)

```bash
# Build project
./mvnw clean package -DskipTests

# Chạy ứng dụng
./mvnw spring-boot:run
```

#### Cách 2: Dùng Maven cài sẵn

```bash
# Build project
mvn clean package -DskipTests

# Chạy ứng dụng
mvn spring-boot:run
```

#### Cách 3: Chạy file JAR trực tiếp

```bash
# Build
./mvnw clean package -DskipTests

# Chạy file JAR
java -jar target/*.jar
```

#### Cách 4: Chạy từ IDE

1. Mở project trong IntelliJ IDEA hoặc Eclipse
2. Tìm file `HashijiCafeApplication.java` (class có `@SpringBootApplication`)
3. Click chuột phải > **Run**

### Bước 8: Validate ứng dụng hoạt động

#### 8a. Kiểm tra console logs

Khi khởi động thành công, console sẽ hiển thị:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.x.x)

Started HashijiCafeApplication in X.XXX seconds
```

#### 8b. Truy cập ứng dụng web

Mở trình duyệt: http://localhost:8080

Đăng nhập:
- **Admin**: `admin` / `password`
- **Nhân viên**: `staff1` / `password`
- **Khách hàng**: `user1` / `password` hoặc `user2` / `password`

#### 8c. Test API (tùy chọn)

```bash
# Test endpoint công khai
curl http://localhost:8080/api/products

# Test đăng nhập
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "password"}'
```

#### 8d. Test Transaction/Trigger/Procedure

```bash
# Đăng nhập vào database
psql -U cafe_admin -d cafe_db
```

```sql
-- Test 1: Trigger tự động cập nhật rating
SELECT name, avg_rating, review_count FROM products 
WHERE id = 'f0000000-0000-0000-0000-000000000001';
-- Kết quả: avg_rating = 4.8, review_count = 12

INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text)
VALUES (uuid_generate_v4(), 'f0000000-0000-0000-0000-000000000001', 
        '22222222-2222-2222-2222-222222222222', 5, 'Ngon!');

SELECT name, avg_rating, review_count FROM products 
WHERE id = 'f0000000-0000-0000-0000-000000000001';
-- Kết quả: avg_rating đã cập nhật, review_count = 13

-- Test 2: Procedure đặt hàng
BEGIN;
SELECT place_order(
    '22222222-2222-2222-2222-222222222222'::UUID,
    '33333333-3333-3333-3333-333333333333'::UUID,
    NULL,
    '[{"product_id": "f0000000-0000-0000-0000-000000000001", "quantity": 2}]'::JSONB
);
COMMIT;

SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;
-- Kết quả: Đơn hàng mới với grand_total = 70000 (2 x 35000)

-- Test 3: Xem báo cáo doanh thu
SELECT * FROM get_revenue_report('2026-01-01', '2026-12-31');
```

## 2.4. Xử lý lỗi thường gặp

### Lỗi: `uuid_generate_v4()` không tồn tại

```
ERROR: function uuid_generate_v4() does not exist
```

**Cách sửa:** Đảm bảo đã chạy Bước 2a (CREATE EXTENSION). Nếu vẫn lỗi:
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
```

### Lỗi: Khóa ngoại (Foreign Key) khi xóa bảng

```
ERROR: cannot drop table ... because other objects depend on it
```

**Cách sửa:** Dùng `CASCADE` để xóa sạch liên kết:
```sql
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
-- ... xóa theo thứ tự ngược lại
```

### Lỗi: Hibernate mất functions/triggers

```
ERROR: function place_order(...) does not exist
```

**Cách sửa:** Kiểm tra `application.properties`, đảm bảo:
```properties
spring.jpa.hibernate.ddl-auto=update
```
KHÔNG dùng `ddl-auto=create` hoặc `ddl-auto=create-drop`.

### Thứ tự thực hiện quan trọng

Tuyệt đối tuân thủ thứ tự: **Bước 2a → 2b → 3 → 4**. Nếu chạy sai thứ tự, có thể gặp lỗi khóa ngoại hoặc thiếu extension.

---

# PHẦN 3: SO SÁNH 2 CÁCH DEPLOY

| Tiêu chí | Docker | Thủ công |
|----------|--------|----------|
| **Thời gian cài đặt** | ~5 phút | ~30 phút |
| **Độ phức tạp** | Thấp | Trung bình |
| **Yêu cầu kiến thức** | Docker cơ bản | PostgreSQL + Java |
| **Tùy chỉnh** | Hạn chế | Linh hoạt |
| **Debug** | Khó hơn | Dễ hơn |
| **Phù hợp** | Demo, test nhanh | Phát triển, debug |

---

# PHẦN 4: THÔNG TIN ĐĂNG NHẬP

## Ứng dụng web (http://localhost:8080 hoặc http://localhost:8080)

| Vai trò | Username | Password |
|---------|----------|----------|
| Admin | `admin` | `password` |
| Nhân viên | `staff1` | `password` |
| Khách hàng | `user1` | `password` |
| Khách hàng | `user2` | `password` |

## pgAdmin (http://localhost:5050 - chỉ khi dùng Docker)

| Thông tin | Giá trị |
|-----------|---------|
| Email | `admin@hashiji.cafe` |
| Password | `admin123` |

## Database

| Thông tin | Giá trị |
|-----------|---------|
| Host | `localhost` (thủ công) hoặc `postgres` (Docker) |
| Port | `5432` |
| Database | `cafe_db` |
| Username | `cafe_admin` |
| Password | `123` |
