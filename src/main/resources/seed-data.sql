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

INSERT INTO users (id, created_at, updated_at, full_name, username, password, email, phone, role, active) VALUES 
('11111111-1111-1111-1111-111111111111', NOW(), NOW(), 'Admin Hashiji', 'admin', '$2b$10$.EQgYU6IAaW11El8wk2PouNIKE66JWSOPhctqDYOroYJlfgS1pMWC', 'admin@hashiji.cafe', '0901234567', 'ADMIN', true),
('22222222-2222-2222-2222-222222222222', NOW(), NOW(), 'Nguyễn Văn Khách', 'user1', '$2b$10$.EQgYU6IAaW11El8wk2PouNIKE66JWSOPhctqDYOroYJlfgS1pMWC', 'khachhang@gmail.com', '0988776655', 'USER', true),
('33333333-3333-3333-3333-333333333333', NOW(), NOW(), 'Trần Lệ Xuân', 'user2', '$2b$10$.EQgYU6IAaW11El8wk2PouNIKE66JWSOPhctqDYOroYJlfgS1pMWC', 'lexuan@gmail.com', '0912345678', 'USER', true),
('44444444-4444-4444-4444-444444444444', NOW(), NOW(), 'Nhân viên Phục vụ', 'staff1', '$2b$10$.EQgYU6IAaW11El8wk2PouNIKE66JWSOPhctqDYOroYJlfgS1pMWC', 'staff@hashiji.cafe', '0902223333', 'STAFF', true);

-- 3. Khởi tạo dữ liệu Categories (Danh mục sản phẩm)
INSERT INTO categories (id, created_at, updated_at, name, name_vi, description) VALUES 
('c0000000-0000-0000-0000-000000000001', NOW(), NOW(), 'Coffee', 'Cà phê', 'Các loại cà phê pha máy và pha phin'),
('c0000000-0000-0000-0000-000000000002', NOW(), NOW(), 'Tea', 'Trà', 'Các loại trà thanh mát'),
('c0000000-0000-0000-0000-000000000003', NOW(), NOW(), 'Smoothie', 'Sinh tố', 'Sinh tố trái cây tươi'),
('c0000000-0000-0000-0000-000000000004', NOW(), NOW(), 'Cake', 'Bánh ngọt', 'Các loại bánh tráng miệng');

-- 4. Khởi tạo dữ liệu Products (Sản phẩm)
INSERT INTO products (id, created_at, updated_at, name, name_vi, category_id, base_price, is_available, avg_rating, review_count) VALUES 
('f0000000-0000-0000-0000-000000000001', NOW(), NOW(), 'Espresso', 'Cà phê Espresso', 'c0000000-0000-0000-0000-000000000001', 35000, true, 4.8, 12),
('f0000000-0000-0000-0000-000000000002', NOW(), NOW(), 'Latte', 'Cà phê Sữa tươi', 'c0000000-0000-0000-0000-000000000001', 45000, true, 4.9, 25),
('f0000000-0000-0000-0000-000000000003', NOW(), NOW(), 'Peach Tea', 'Trà Đào Cam Sả', 'c0000000-0000-0000-0000-000000000002', 40000, true, 4.7, 50),
('f0000000-0000-0000-0000-000000000004', NOW(), NOW(), 'Cappuccino', 'Cà phê Cappuccino', 'c0000000-0000-0000-0000-000000000001', 50000, true, 4.6, 30),
('f0000000-0000-0000-0000-000000000005', NOW(), NOW(), 'Americano', 'Cà phê đen', 'c0000000-0000-0000-0000-000000000001', 30000, true, 4.5, 100),
('f0000000-0000-0000-0000-000000000006', NOW(), NOW(), 'Matcha Latte', 'Sữa chua Trà Xanh', 'c0000000-0000-0000-0000-000000000002', 55000, true, 4.9, 85),
('f0000000-0000-0000-0000-000000000007', NOW(), NOW(), 'Mango Smoothie', 'Sinh tố Xoài', 'c0000000-0000-0000-0000-000000000003', 45000, true, 4.8, 15),
('f0000000-0000-0000-0000-000000000008', NOW(), NOW(), 'Tiramisu', 'Bánh Tiramisu', 'c0000000-0000-0000-0000-000000000004', 60000, true, 4.9, 60);

-- 5. Khởi tạo dữ liệu Orders (Đơn hàng)
INSERT INTO orders (id, created_at, updated_at, user_id, order_status, grand_total, payment_method, payment_status, tracking_code, customer_name, phone, order_type) VALUES 
('d0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', '22222222-2222-2222-2222-222222222222', 'COMPLETED', 115000, 'CASH', 'PAID', 'TRK-ORDER-001', 'Nguyễn Văn Khách', '0988776655', 'DINE_IN'),
('d0000000-0000-0000-0000-000000000002', NOW(), NOW(), '22222222-2222-2222-2222-222222222222', 'PENDING', 80000, 'BANK_TRANSFER', 'UNPAID', 'TRK-ORDER-002', 'Nguyễn Văn Khách', '0988776655', 'TAKEAWAY'),
('d0000000-0000-0000-0000-000000000003', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', '33333333-3333-3333-3333-333333333333', 'COMPLETED', 105000, 'MOMO', 'PAID', 'TRK-ORDER-003', 'Trần Lệ Xuân', '0912345678', 'DELIVERY'),
('d0000000-0000-0000-0000-000000000004', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', '33333333-3333-3333-3333-333333333333', 'COMPLETED', 165000, 'CREDIT_CARD', 'PAID', 'TRK-ORDER-004', 'Trần Lệ Xuân', '0912345678', 'TAKEAWAY'),
('d0000000-0000-0000-0000-000000000005', NOW() - INTERVAL '1 hour', NOW(), '22222222-2222-2222-2222-222222222222', 'CANCELLED', 50000, 'CASH', 'UNPAID', 'TRK-ORDER-005', 'Nguyễn Văn Khách', '0988776655', 'DINE_IN');

-- 6. Khởi tạo dữ liệu Order Items (Chi tiết đơn hàng)
-- Đơn 1: 1 Latte (45k) + 2 Espresso (35k * 2 = 70k) -> Tổng 115k
INSERT INTO order_items (id, created_at, updated_at, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total) VALUES 
(uuid_generate_v4(), NOW(), NOW(), 'd0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000002', 'Latte', 45000, 1, 45000),
(uuid_generate_v4(), NOW(), NOW(), 'd0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'Espresso', 35000, 2, 70000);

-- Đơn 2: 2 Peach Tea (40k * 2 = 80k) -> Tổng 80k
INSERT INTO order_items (id, created_at, updated_at, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total) VALUES 
(uuid_generate_v4(), NOW(), NOW(), 'd0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003', 'Peach Tea', 40000, 2, 80000);

-- Đơn 3: 1 Mango Smoothie (45k) + 1 Tiramisu (60k) -> Tổng 105k
INSERT INTO order_items (id, created_at, updated_at, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total) VALUES 
(uuid_generate_v4(), NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'd0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000007', 'Mango Smoothie', 45000, 1, 45000),
(uuid_generate_v4(), NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'd0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000008', 'Tiramisu', 60000, 1, 60000);

-- Đơn 4: 3 Matcha Latte (55k * 3 = 165k)
INSERT INTO order_items (id, created_at, updated_at, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total) VALUES 
(uuid_generate_v4(), NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', 'd0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000006', 'Matcha Latte', 55000, 3, 165000);

-- Đơn 5: 1 Cappuccino (50k)
INSERT INTO order_items (id, created_at, updated_at, order_id, product_id, snapshot_product_name, snapshot_unit_price, quantity, sub_total) VALUES 
(uuid_generate_v4(), NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', 'd0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000004', 'Cappuccino', 50000, 1, 50000);

-- =========================================================================
-- HẾT SCRIPT SEED DATA
-- Hệ thống đã có đủ dữ liệu để Test Triggers & Procedures.
-- =========================================================================
