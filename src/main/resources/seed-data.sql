-- =========================================================================
-- SQL SEED DATA CHO BÁO CÁO MÔN DBMS (HASHIJI-CAFE)
-- File này tự động được chạy qua docker-compose (02-seed-data.sql)
-- Tất cả các khóa chính đều sử dụng chuẩn UUID thay cho Long/BigInt.
-- =========================================================================

-- 1. Xóa dữ liệu cũ (để tránh lỗi trùng lặp khi chạy lại)
TRUNCATE TABLE order_items CASCADE;
TRUNCATE TABLE orders CASCADE;
TRUNCATE TABLE products CASCADE;
TRUNCATE TABLE categories CASCADE;
TRUNCATE TABLE users CASCADE;

-- 2. Khởi tạo dữ liệu Users (Khách hàng & Admin)
-- Mật khẩu mặc định: 'password' (đã băm BCrypt)
INSERT INTO users (id, created_at, updated_at, full_name, username, password, email, phone, role) VALUES 
('11111111-1111-1111-1111-111111111111', NOW(), NOW(), 'Admin Hashiji', 'admin', '$2a$10$xyz', 'admin@hashiji.cafe', '0901234567', 'ROLE_ADMIN'),
('22222222-2222-2222-2222-222222222222', NOW(), NOW(), 'Nguyễn Văn Khách', 'user1', '$2a$10$xyz', 'khachhang@gmail.com', '0988776655', 'ROLE_USER');

-- 3. Khởi tạo dữ liệu Categories (Danh mục sản phẩm)
INSERT INTO categories (id, created_at, updated_at, name, name_vi, description, slug) VALUES 
('c0000000-0000-0000-0000-000000000001', NOW(), NOW(), 'Coffee', 'Cà phê', 'Các loại cà phê pha máy và pha phin', 'coffee'),
('c0000000-0000-0000-0000-000000000002', NOW(), NOW(), 'Tea', 'Trà', 'Các loại trà thanh mát', 'tea');

-- 4. Khởi tạo dữ liệu Products (Sản phẩm)
INSERT INTO products (id, created_at, updated_at, name, name_vi, category_id, base_price, is_available, avg_rating, review_count, inventory_count) VALUES 
('p0000000-0000-0000-0000-000000000001', NOW(), NOW(), 'Espresso', 'Cà phê Espresso', 'c0000000-0000-0000-0000-000000000001', 35000, true, 4.8, 12, 100),
('p0000000-0000-0000-0000-000000000002', NOW(), NOW(), 'Latte', 'Cà phê Sữa tươi', 'c0000000-0000-0000-0000-000000000001', 45000, true, 4.9, 25, 50),
('p0000000-0000-0000-0000-000000000003', NOW(), NOW(), 'Peach Tea', 'Trà Đào Cam Sả', 'c0000000-0000-0000-0000-000000000002', 40000, true, 4.7, 50, 200);

-- 5. Khởi tạo dữ liệu Orders (Đơn hàng)
INSERT INTO orders (id, created_at, updated_at, user_id, order_status, grand_total, payment_method, payment_status, tracking_code, customer_name, customer_phone, order_type) VALUES 
('o0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', '22222222-2222-2222-2222-222222222222', 'COMPLETED', 115000, 'CASH', 'PAID', 'TRK-ORDER-001', 'Nguyễn Văn Khách', '0988776655', 'DINE_IN'),
('o0000000-0000-0000-0000-000000000002', NOW(), NOW(), '22222222-2222-2222-2222-222222222222', 'PENDING', 80000, 'BANK_TRANSFER', 'UNPAID', 'TRK-ORDER-002', 'Nguyễn Văn Khách', '0988776655', 'TAKEAWAY');

-- 6. Khởi tạo dữ liệu Order Items (Chi tiết đơn hàng)
-- Đơn 1: 1 Latte (45k) + 2 Espresso (35k * 2 = 70k) -> Tổng 115k
INSERT INTO order_items (id, created_at, updated_at, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total) VALUES 
(uuid_generate_v4(), NOW(), NOW(), 'o0000000-0000-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000002', 'Latte', 45000, 1, 45000),
(uuid_generate_v4(), NOW(), NOW(), 'o0000000-0000-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000001', 'Espresso', 35000, 2, 70000);

-- Đơn 2: 2 Peach Tea (40k * 2 = 80k) -> Tổng 80k
INSERT INTO order_items (id, created_at, updated_at, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total) VALUES 
(uuid_generate_v4(), NOW(), NOW(), 'o0000000-0000-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000003', 'Peach Tea', 40000, 2, 80000);

-- =========================================================================
-- HẾT SCRIPT SEED DATA
-- Hệ thống đã có đủ dữ liệu để Test Triggers & Procedures.
-- =========================================================================
