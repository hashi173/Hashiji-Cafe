# Kịch bản Demo Hệ quản trị Cơ sở dữ liệu & Báo cáo Audit Dự án Hashiji-Cafe

## 2. Kịch Bản Demo Trực Tiếp CSDL (Database Demo Script)

Do Backend đang trong quá trình refactor lớn, cách tốt nhất và an toàn nhất để bảo vệ điểm số môn DBMS của nhóm là **thực hiện Demo trực tiếp trên PostgreSQL / pgAdmin**.

### Bước 1: Khởi tạo dữ liệu mẫu (Chuẩn bị môi trường)
Chạy script để tạo các bảng (nếu không dùng Spring Boot để gen) và chèn dữ liệu mẫu.

```sql
-- Tạo các bản ghi cơ bản cần thiết cho Demo
INSERT INTO products (id, name, base_price, is_available, avg_rating, review_count) 
VALUES ('11111111-1111-1111-1111-111111111111', 'Cà phê Đen', 25000, true, 0, 0);

INSERT INTO users (id, username, role) 
VALUES ('22222222-2222-2222-2222-222222222222', 'khach_hang_1', 'CUSTOMER');

INSERT INTO user_addresses (id, user_id, address_line, is_default)
VALUES ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', '123 PTIT', true);
```

### Bước 2: Demo Trigger 1 - Cập nhật điểm đánh giá sản phẩm tự động
**Mục tiêu:** Chứng minh khi có người dùng đánh giá, `avg_rating` của bảng `products` tự thay đổi.

1.  **Mở bảng Products**, cho giảng viên xem sản phẩm 'Cà phê Đen' đang có `avg_rating = 0` và `review_count = 0`.
2.  **Chạy lệnh chèn đánh giá:**
    ```sql
    INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text) 
    VALUES (uuid_generate_v4(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 5, 'Rất ngon!');
    
    INSERT INTO product_reviews (id, product_id, user_id, rating_score, review_text) 
    VALUES (uuid_generate_v4(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 4, 'Hơi nhạt');
    ```
3.  **Kiểm tra lại bảng Products:**
    ```sql
    SELECT name, avg_rating, review_count FROM products WHERE id = '11111111-1111-1111-1111-111111111111';
    ```
    *Kỳ vọng:* `avg_rating` = 4.5, `review_count` = 2.

### Bước 3: Demo Transaction & Stored Procedure - Đặt hàng Atomic
**Mục tiêu:** Chứng minh Procedure `place_order` đảm bảo tính toàn vẹn dữ liệu (Rollback nếu lỗi, Commit nếu đúng).

1.  **Chạy Transaction Đặt hàng hợp lệ:**
    ```sql
    BEGIN; 
    SELECT place_order( 
        '22222222-2222-2222-2222-222222222222',  -- user_id 
        '33333333-3333-3333-3333-333333333333',  -- address_id 
        NULL,                                    -- promotion_id 
        '[{"product_id": "11111111-1111-1111-1111-111111111111", "quantity": 2}]'::JSONB 
    ); 
    COMMIT;
    ```
2.  **Kiểm tra:**
    ```sql
    SELECT * FROM orders;
    SELECT * FROM order_items;
    ```
    *Kỳ vọng:* Có 1 đơn hàng mới với `grand_total` = 50000 (2 x 25000), trạng thái `PENDING`.

3.  **Chạy Transaction Đặt hàng LỖI (Để xem Rollback):**
    ```sql
    BEGIN; 
    -- Gọi một sản phẩm KHÔNG TỒN TẠI (ID ảo)
    SELECT place_order( 
        '22222222-2222-2222-2222-222222222222', 
        '33333333-3333-3333-3333-333333333333', 
        NULL, 
        '[{"product_id": "99999999-9999-9999-9999-999999999999", "quantity": 1}]'::JSONB 
    ); 
    -- Hệ thống sẽ báo lỗi: "Sản phẩm không khả dụng"
    ROLLBACK;
    ```
    *Kỳ vọng:* Chứng minh Database đã từ chối lưu bất kỳ dữ liệu nào của transaction này do vi phạm.

### Bước 4: Demo Stored Procedure - Thống kê doanh thu
**Mục tiêu:** Chạy hàm Report để xem tổng kết kinh doanh.
1. Cập nhật đơn hàng vừa tạo thành `COMPLETED`:
   ```sql
   UPDATE orders SET order_status = 'COMPLETED' WHERE user_id = '22222222-2222-2222-2222-222222222222';
   ```
2. Chạy báo cáo:
   ```sql
   SELECT * FROM get_revenue_report('2026-04-01', '2026-04-30');
   ```
   *Kỳ vọng:* Hiển thị bảng tổng kết doanh thu (tổng đơn, tổng tiền) theo từng ngày.
