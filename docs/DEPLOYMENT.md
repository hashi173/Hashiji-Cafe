# Hướng dẫn Setup Database Thủ Công (Local Deployment)

Tài liệu này hướng dẫn bạn cách thiết lập cơ sở dữ liệu PostgreSQL cục bộ (không dùng Docker) để chạy ứng dụng Hashiji Cafe.

## Yêu cầu hệ thống
- Đã cài đặt **PostgreSQL 15** trở lên (Mac: `brew install postgresql@15`, Windows: Tải từ trang chủ postgresql.org).
- Có công cụ **psql** hoặc giao diện như pgAdmin/DBeaver.
  *(Lưu ý: Nếu chạy lệnh `psql` bị lỗi "command not found", bạn cần thêm vào Environment Variables)*
  - **Mac (Homebrew)**: Chạy lệnh `export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"` (với chip M) hoặc `export PATH="/usr/local/opt/postgresql@15/bin:$PATH"` (với chip Intel). Bạn nên thêm lệnh này vào cuối file `~/.zshrc`.
  - **Windows**: Thêm thư mục `bin` (vd: `C:\Program Files\PostgreSQL\15\bin`) vào biến môi trường **PATH** của hệ thống.

---

## Các bước thực hiện

### Bước 1: Tạo Database và User

Bạn có thể làm theo một trong hai cách dưới đây:

**Cách 1: Dùng lệnh psql (Khuyên dùng)**

Mở terminal (hoặc Command Prompt) và đăng nhập vào PostgreSQL bằng quyền superuser (thường là `postgres`):

```bash
psql -U postgres
```

Sau khi vào giao diện lệnh của PostgreSQL (`postgres=#`), chạy lần lượt các lệnh sau:

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

-- Thoát khỏi psql
\q
```

**Cách 2: Dùng giao diện pgAdmin**

1. Mở **pgAdmin** và kết nối vào server cục bộ của bạn.
2. Tạo User: Chuột phải vào **Login/Group Roles** > **Create** > **Login/Group Role...**:
   - Tab **General**: Name nhập `cafe_admin`.
   - Tab **Definition**: Password nhập `123`.
   - Tab **Privileges**: Bật **Can login?**. Nhấn **Save**.
3. Tạo Database: Chuột phải vào **Databases** > **Create** > **Database...**:
   - Tab **General**: Database nhập `cafe_db`, Owner chọn `cafe_admin`. Nhấn **Save**.
4. Cấp quyền Schema (với Postgres 15+): Mở **Query Tool** trên database `cafe_db` và chạy lệnh:
   ```sql
   GRANT ALL ON SCHEMA public TO cafe_admin;
   ```

### Bước 2: Khởi tạo Schema và Dữ liệu mẫu (Seed Data)

Ứng dụng yêu cầu cấu trúc bảng và một số hàm/trigger đặc biệt.

**Cách 1: Dùng lệnh (Nếu đã cài psql)**

Tại thư mục gốc của project, bạn chạy các lệnh sau để nạp file SQL:
```bash
# Nạp cấu trúc bảng, functions và triggers & data mẫu
psql -U cafe_admin -d cafe_db -f src/main/resources/schema-advanced.sql
psql -U cafe_admin -d cafe_db -f src/main/resources/seed-data.sql
```
*(Nếu hệ thống hỏi mật khẩu, nhập: `123`)*

**Cách 2: Dùng giao diện pgAdmin**

1. Nhấn chọn database `cafe_db` trong cây thư mục bên trái của pgAdmin.
2. Mở công cụ **Query Tool** (biểu tượng hình thùng phi có nút play).
3. Copy toàn bộ nội dung file `src/main/resources/schema-advanced.sql` ở trong code, dán vào Query Tool và nhấn **Execute/Refresh (F5)** (nút hình tam giác).
4. Xóa nội dung cũ trong Query Tool, tiếp tục copy toàn bộ file `src/main/resources/seed-data.sql`, dán vào và nhấn **Execute/Refresh (F5)**.

### Bước 3: Cấu hình Spring Boot kết nối tới Local DB

Đảm bảo file `src/main/resources/application-dev.properties` (hoặc file cấu hình tương ứng) của bạn trỏ đúng về `localhost` thay vì `postgres` (host của Docker):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cafe_db
spring.datasource.username=cafe_admin
spring.datasource.password=123

# Đảm bảo Hibernate không tự động drop bảng làm mất hàm/trigger
spring.jpa.hibernate.ddl-auto=update
```

### Bước 4: Chạy ứng dụng

Bây giờ bạn đã có thể chạy project Spring Boot bình thường thông qua IDE (IntelliJ, Eclipse) hoặc bằng Maven:

```bash
./mvnw spring-boot:run
```

---

## Thông tin đăng nhập web test

Sau khi ứng dụng khởi động thành công (tại `http://localhost:8080`), bạn có thể sử dụng các tài khoản sau (Mật khẩu chung: **`password`**):

- **Admin**: `admin`
- **Nhân viên**: `staff1`
- **Khách hàng**: `user1`, `user2`
