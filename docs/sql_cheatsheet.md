# 📘 SQL Cheatsheet – Transaction, Trigger, Procedure
> Tài liệu nội bộ cho **tất cả thành viên Nhóm 5** hiểu rõ 3 đối tượng DBMS nâng cao đã triển khai trong dự án Hashiji-Cafe.

---

## 1. Transaction (Giao dịch)

### 1.1. Transaction là gì?

Transaction là **một nhóm câu lệnh SQL** được thực thi **nguyên tử** (atomic) – nghĩa là hoặc TẤT CẢ thành công, hoặc KHÔNG CÂU NÀO có hiệu lực.

```
BEGIN;          ← Mở transaction
  lệnh 1;
  lệnh 2;      ← Nếu lệnh 2 lỗi → mọi thứ bị hủy
  lệnh 3;
COMMIT;         ← Xác nhận lưu tất cả
-- hoặc --
ROLLBACK;       ← Hủy toàn bộ, DB trở về trạng thái trước BEGIN
```

### 1.2. Tính chất ACID

| Tính chất | Ý nghĩa | Ví dụ trong Hashiji |
|-----------|---------|---------------------|
| **A**tomicity (Nguyên tử) | Tất cả hoặc không gì cả | Đặt hàng + xóa giỏ: nếu xóa giỏ lỗi → đơn hàng cũng bị hủy |
| **C**onsistency (Nhất quán) | DB luôn ở trạng thái hợp lệ | Tổng `grand_total` luôn = `sub_total - discount_amount` |
| **I**solation (Cô lập) | Các transaction không ảnh hưởng lẫn nhau | 2 người đặt cùng lúc không đọc được dữ liệu dở dang của nhau |
| **D**urability (Bền vững) | Dữ liệu đã COMMIT thì không bao giờ mất | Mất điện sau COMMIT → dữ liệu vẫn còn |

### 1.3. Code trong dự án (Transaction 1 – Đặt hàng)

```sql
BEGIN;
-- Bước 1: Tạo đơn hàng qua stored procedure
SELECT place_order(
    '550e8400-e29b-41d4-a716-446655440000'::UUID,  -- user_id
    '6ba7b810-9dad-11d1-80b4-00c04fd430c8'::UUID,  -- address_id
    NULL,
    '[{"product_id": "abc123", "quantity": 2,
      "selected_options": {"size": "L", "sugar": "50%"}}]'::JSONB
);

-- Bước 2: Xóa giỏ hàng sau khi đặt hàng thành công
DELETE FROM cart_items
WHERE session_id = '7c9e6679-7425-40de-944b-e07fc1f90ae7'::UUID;

-- Bước 3: Cập nhật trạng thái phiên mua hàng
UPDATE shopping_sessions SET total_amount = 0
WHERE id = '7c9e6679-7425-40de-944b-e07fc1f90ae7'::UUID;

COMMIT;
-- Nếu có lỗi bất kỳ trong các bước trên: ROLLBACK;
```

**Giải thích flow:**
1. `BEGIN;` → PostgreSQL bắt đầu ghi tạm, chưa lưu thật.
2. `place_order(...)` → Gọi procedure tạo đơn + tạo order_items.
3. `DELETE` + `UPDATE` → Dọn giỏ hàng.
4. `COMMIT;` → Lưu tất cả vào đĩa. Nếu giữa chừng lỗi (ví dụ sản phẩm không tồn tại) → `ROLLBACK;` sẽ xóa sạch mọi thay đổi.

### 1.4. Cách test ROLLBACK (demo cho thầy)

```sql
BEGIN;
-- Cố tình chèn sản phẩm không tồn tại
INSERT INTO order_items (id, order_id, product_id, snapshot_product_name,
    snapshot_unit_price, quantity, sub_total)
VALUES (uuid_generate_v4(), uuid_generate_v4(),
    '00000000-0000-0000-0000-ffffffffffff', 'FakeProduct', 0, 1, 0);
-- → Lỗi FK vì product_id không tồn tại
ROLLBACK;
-- Kiểm tra: SELECT count(*) FROM order_items; → không đổi!
```

---

## 2. Trigger (Bẫy sự kiện)

### 2.1. Trigger là gì?

Trigger là **hàm tự động chạy** khi có sự kiện xảy ra trên bảng (INSERT, UPDATE, DELETE). Không cần gọi bằng tay – database tự kích hoạt.

### 2.2. Cấu trúc cơ bản

```sql
-- Bước 1: Tạo hàm trigger (RETURNS TRIGGER)
CREATE OR REPLACE FUNCTION tên_hàm()
RETURNS TRIGGER AS $$
BEGIN
    -- Logic xử lý ở đây
    -- NEW = dòng mới (INSERT/UPDATE)
    -- OLD = dòng cũ  (UPDATE/DELETE)
    RETURN NEW;  -- hoặc RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Bước 2: Gắn trigger vào bảng
CREATE TRIGGER tên_trigger
    AFTER INSERT OR UPDATE ON tên_bảng   -- BEFORE hoặc AFTER
    FOR EACH ROW                          -- Chạy cho TỪNG dòng bị ảnh hưởng
    EXECUTE FUNCTION tên_hàm();
```

### 2.3. BEFORE vs AFTER

| Loại | Khi nào chạy | Dùng khi |
|------|--------------|----------|
| `BEFORE` | **Trước** khi dữ liệu được ghi vào bảng | Muốn **sửa đổi** hoặc **chặn** dữ liệu trước khi lưu |
| `AFTER` | **Sau** khi dữ liệu đã ghi xong | Muốn **cập nhật bảng khác** dựa trên dữ liệu vừa thay đổi |

### 2.4. NEW vs OLD

| Biến | Có trong sự kiện | Ý nghĩa |
|------|-------------------|---------|
| `NEW` | INSERT, UPDATE | Giá trị **mới** của dòng (sắp/vừa được ghi) |
| `OLD` | UPDATE, DELETE | Giá trị **cũ** của dòng (trước khi thay đổi) |

### 2.5. Ba Trigger trong dự án

#### Trigger 1: Tự động cập nhật rating sản phẩm
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

**Giải thích từng dòng:**
- Khi ai đó **thêm / sửa / xóa** 1 review trong bảng `product_reviews` →
- Trigger tự chạy hàm `update_product_rating()`.
- Hàm tính lại `AVG(rating_score)` và `COUNT(*)` cho sản phẩm liên quan.
- Kết quả: cột `avg_rating` và `review_count` trong bảng `products` luôn chính xác mà không cần tính tay.

#### Trigger 2: Ghi log hành vi thêm giỏ hàng
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

**Giải thích:** Mỗi khi khách hàng thêm sản phẩm vào giỏ (`INSERT ON cart_items`), hệ thống tự ghi 1 dòng log vào `user_behavior_logs` để AI Recommendation Engine dùng cho thuật toán gợi ý sản phẩm.

#### Trigger 3: Giữ duy nhất 1 địa chỉ mặc định
```sql
CREATE OR REPLACE FUNCTION enforce_single_default_address()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE user_addresses
        SET is_default = FALSE
        WHERE user_id = NEW.user_id
          AND id <> NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_single_default_address
    BEFORE INSERT OR UPDATE ON user_addresses
    FOR EACH ROW EXECUTE FUNCTION enforce_single_default_address();
```

**Giải thích:** Khi user đặt 1 địa chỉ mới là `is_default = TRUE`, trigger tự tắt `is_default` của tất cả địa chỉ cũ. Dùng `BEFORE` vì ta cần chỉnh dữ liệu **trước** khi nó được ghi.

---

## 3. Stored Procedure / Function (Hàm lưu trữ)

### 3.1. Là gì?

Stored Procedure là **khối mã SQL được lưu sẵn trong database**, có thể nhận tham số đầu vào và trả về kết quả. Lợi ích:
- **Tái sử dụng**: viết 1 lần, gọi nhiều lần.
- **Hiệu năng**: chạy trực tiếp trong DB, không cần gửi nhiều câu SQL qua mạng.
- **Bảo mật**: ứng dụng chỉ cần gọi tên hàm, không cần biết cấu trúc bảng.

> ⚠️ PostgreSQL gọi là **Function** (dùng `CREATE FUNCTION`), khác MySQL dùng `CREATE PROCEDURE`. Về bản chất là giống nhau.

### 3.2. Cấu trúc cơ bản

```sql
CREATE OR REPLACE FUNCTION tên_hàm(
    tham_số_1 kiểu_dữ_liệu,
    tham_số_2 kiểu_dữ_liệu DEFAULT giá_trị_mặc_định
)
RETURNS kiểu_trả_về AS $$
DECLARE
    biến_cục_bộ kiểu_dữ_liệu;
BEGIN
    -- Logic xử lý
    RETURN kết_quả;
END;
$$ LANGUAGE plpgsql;
```

**Gọi hàm:**
```sql
SELECT tên_hàm(giá_trị_1, giá_trị_2);
```

### 3.3. Hai Procedure trong dự án

#### Procedure 1: `place_order` – Đặt hàng
```
Đầu vào:  user_id, address_id, promotion_id, items (JSONB array)
Đầu ra:   UUID của đơn hàng vừa tạo
```

**Flow xử lý:**
```
1. Kiểm tra address_id có thuộc user_id không → Nếu không → RAISE EXCEPTION
2. Tạo UUID cho đơn hàng mới
3. Lặp qua từng item trong JSONB:
   a. Tìm sản phẩm theo product_id
   b. Kiểm tra is_available
   c. Tính sub_total = base_price × quantity
   d. INSERT vào order_items (snapshot tên + giá tại thời điểm đặt)
4. Nếu có promotion_id → tính discount
5. INSERT vào orders: grand_total = sub_total - discount
6. RETURN order_id
```

#### Procedure 2: `get_revenue_report` – Báo cáo doanh thu
```
Đầu vào:  from_date, to_date
Đầu ra:   Bảng gồm (ngày, tổng đơn, tổng doanh thu, giá trị TB mỗi đơn)
```

```sql
SELECT * FROM get_revenue_report('2026-04-01', '2026-04-30');
```

| report_date | total_orders | total_revenue | avg_order_val |
|-------------|-------------|---------------|---------------|
| 2026-04-20  | 3           | 345000        | 115000.00     |
| 2026-04-21  | 5           | 520000        | 104000.00     |

---

## 4. Tổng kết nhanh

| Đối tượng | Khi nào chạy | Ai gọi | Ví dụ trong Hashiji |
|-----------|-------------|--------|---------------------|
| **Transaction** | Khi cần nhóm nhiều lệnh thành 1 đơn vị | Lập trình viên (BEGIN/COMMIT) | Đặt hàng + xóa giỏ + reset session |
| **Trigger** | Tự động khi có INSERT/UPDATE/DELETE | Database tự kích hoạt | Cập nhật rating, ghi log giỏ hàng |
| **Procedure** | Khi được gọi bằng `SELECT func()` | Lập trình viên hoặc trigger | `place_order()`, `get_revenue_report()` |

---

## 5. Các lệnh kiểm tra hữu ích

```sql
-- Xem danh sách tất cả trigger trong DB
SELECT trigger_name, event_manipulation, event_object_table
FROM information_schema.triggers
WHERE trigger_schema = 'public';

-- Xem danh sách tất cả function/procedure
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema = 'public';

-- Xem mã nguồn của 1 function
SELECT prosrc FROM pg_proc WHERE proname = 'place_order';

-- Xóa trigger (nếu cần sửa lại)
DROP TRIGGER IF EXISTS trg_update_product_rating ON product_reviews;

-- Xóa function (nếu cần sửa lại)
DROP FUNCTION IF EXISTS update_product_rating();
```
