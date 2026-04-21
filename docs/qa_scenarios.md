# 🎯 Dự Trù Kịch Bản Hỏi Vấn Đáp – Transaction, Trigger, Procedure
> Tài liệu chuẩn bị cho **buổi demo DBMS – Nhóm 5**. Mỗi kịch bản bao gồm câu hỏi thầy có thể đặt ra và câu trả lời mẫu kèm code SQL sẵn sàng chạy.

---

## Các lệnh SQL "cứu nguy" (copy-paste nhanh)

```sql
-- Xem tất cả trigger
SELECT trigger_name, event_object_table FROM information_schema.triggers WHERE trigger_schema='public';

-- Xem tất cả function
SELECT routine_name FROM information_schema.routines WHERE routine_schema='public' AND routine_type='FUNCTION';

-- Xem mã nguồn function
SELECT prosrc FROM pg_proc WHERE proname = 'place_order';

-- Xóa trigger
DROP TRIGGER IF EXISTS tên_trigger ON tên_bảng;

-- Xóa function
DROP FUNCTION IF EXISTS tên_hàm();

-- Tạo lại function (dùng CREATE OR REPLACE để không cần xóa trước)
CREATE OR REPLACE FUNCTION tên_hàm() ...
```

## Phần A: Câu hỏi về TRANSACTION

### A1. "Hãy tạo một transaction mới để chuyển trạng thái đơn hàng"
**Tình huống:** Thầy yêu cầu viết transaction chuyển đơn hàng từ PENDING → COMPLETED và đồng thời cập nhật payment_status.

```sql
BEGIN;

-- Bước 1: Cập nhật trạng thái đơn
UPDATE orders
SET order_status = 'COMPLETED',
    payment_status = 'PAID',
    updated_at = NOW()
WHERE id = 'd0000000-0000-0000-0000-000000000005';

-- Bước 2: Kiểm tra kết quả
SELECT id, order_status, payment_status, grand_total
FROM orders
WHERE id = 'd0000000-0000-0000-0000-000000000005';

COMMIT;
```

**Giải thích cho thầy:** "Em sử dụng transaction để đảm bảo cả 2 cột `order_status` và `payment_status` được cập nhật đồng thời. Nếu 1 trong 2 lệnh lỗi, ROLLBACK sẽ đưa toàn bộ dữ liệu về trạng thái ban đầu."

---

### A2. "Demo cho thầy xem ROLLBACK hoạt động như thế nào?"
**Tình huống:** Thầy muốn thấy khi transaction bị lỗi giữa chừng, dữ liệu không bị thay đổi.

```sql
-- Xem trạng thái TRƯỚC
SELECT id, order_status, grand_total FROM orders
WHERE id = 'd0000000-0000-0000-0000-000000000001';

BEGIN;

-- Bước 1: Cập nhật giá (thành công)
UPDATE orders SET grand_total = 999999
WHERE id = 'd0000000-0000-0000-0000-000000000001';

-- Bước 2: Cố tình gây lỗi (FK vi phạm)
UPDATE orders SET user_id = '00000000-0000-0000-0000-ffffffffffff'
WHERE id = 'd0000000-0000-0000-0000-000000000001';
-- → LỖI: user_id không tồn tại trong bảng users

ROLLBACK;

-- Xem trạng thái SAU → grand_total vẫn giữ nguyên giá trị cũ!
SELECT id, order_status, grand_total FROM orders
WHERE id = 'd0000000-0000-0000-0000-000000000001';
```

**Giải thích:** "Mặc dù Bước 1 đã chạy thành công, nhưng vì Bước 2 gây lỗi nên toàn bộ transaction bị hủy. `grand_total` vẫn giữ nguyên giá trị 115000, chứng minh tính Atomicity."

---

### A3. "Tại sao không dùng từng câu UPDATE riêng lẻ mà phải gom vào transaction?"

**Trả lời mẫu:**
> "Nếu chạy riêng lẻ, giả sử câu UPDATE đầu thành công (đổi `order_status = COMPLETED`) nhưng câu UPDATE thứ 2 bị lỗi (không cập nhật được `payment_status`), thì dữ liệu sẽ rơi vào trạng thái không nhất quán: đơn hàng đã hoàn thành nhưng chưa thanh toán. Transaction đảm bảo điều này không xảy ra."

---

### A4. "Viết transaction với SAVEPOINT"
**Tình huống nâng cao:** Thầy hỏi cách rollback 1 phần thay vì toàn bộ.

```sql
BEGIN;

-- Bước 1: Cập nhật đơn hàng 1
UPDATE orders SET order_status = 'COMPLETED'
WHERE id = 'd0000000-0000-0000-0000-000000000001';

SAVEPOINT sp_before_order2;

-- Bước 2: Cố tình gây lỗi ở đơn hàng 2
UPDATE orders SET grand_total = -100
WHERE id = 'd0000000-0000-0000-0000-000000000002';
-- Giả sử ta phát hiện giá trị không hợp lệ

ROLLBACK TO sp_before_order2;
-- → Chỉ hủy Bước 2, Bước 1 vẫn giữ nguyên

COMMIT;
-- → Đơn hàng 1 đã COMPLETED, đơn hàng 2 không bị ảnh hưởng
```

---

## Phần B: Câu hỏi về TRIGGER

### B1. "Sửa trigger rating để xử lý cả trường hợp DELETE"
**Tình huống:** Thầy chỉ ra rằng khi DELETE một review, `NEW.product_id` sẽ bị NULL vì không có dòng mới. Yêu cầu sửa lại.

```sql
CREATE OR REPLACE FUNCTION update_product_rating()
RETURNS TRIGGER AS $$
DECLARE
    v_product_id UUID;
BEGIN
    -- Khi DELETE, dùng OLD; khi INSERT/UPDATE, dùng NEW
    IF TG_OP = 'DELETE' THEN
        v_product_id := OLD.product_id;
    ELSE
        v_product_id := NEW.product_id;
    END IF;

    UPDATE products SET
        avg_rating = (
            SELECT COALESCE(AVG(rating_score), 0)::NUMERIC(3,2)
            FROM product_reviews
            WHERE product_id = v_product_id
        ),
        review_count = (
            SELECT COUNT(*)
            FROM product_reviews
            WHERE product_id = v_product_id
        )
    WHERE id = v_product_id;

    -- DELETE phải RETURN OLD, INSERT/UPDATE phải RETURN NEW
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

**Giải thích:** "Em sử dụng biến đặc biệt `TG_OP` để phân biệt loại sự kiện. Khi DELETE, dòng bị xóa nằm trong biến `OLD` (không có `NEW`). Em cũng thêm `COALESCE` để tránh lỗi NULL khi sản phẩm không còn review nào."

---

### B2. "Tạo trigger mới: Tự động cập nhật inventory khi đơn hàng hoàn thành"
**Tình huống:** Thầy yêu cầu viết trigger mới từ đầu.

```sql
-- Bước 1: Tạo hàm trigger
CREATE OR REPLACE FUNCTION deduct_inventory_on_complete()
RETURNS TRIGGER AS $$
BEGIN
    -- Chỉ chạy khi trạng thái chuyển sang COMPLETED
    IF NEW.order_status = 'COMPLETED' AND OLD.order_status <> 'COMPLETED' THEN
        UPDATE products p
        SET inventory_count = p.inventory_count - oi.quantity
        FROM order_items oi
        WHERE oi.order_id = NEW.id
          AND oi.product_id = p.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Bước 2: Gắn trigger vào bảng orders
CREATE TRIGGER trg_deduct_inventory
    AFTER UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION deduct_inventory_on_complete();
```

**Giải thích:** "Trigger này kích hoạt khi `UPDATE orders`, nhưng chỉ thực sự trừ inventory khi trạng thái thay đổi từ khác → `COMPLETED`. Điều kiện `OLD.order_status <> 'COMPLETED'` đảm bảo không bị trừ kho 2 lần."

---

### B3. "Tạo trigger BEFORE INSERT để validate dữ liệu"
**Tình huống:** Thầy yêu cầu chặn đơn hàng có `grand_total < 0`.

```sql
CREATE OR REPLACE FUNCTION validate_order_total()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.grand_total < 0 THEN
        RAISE EXCEPTION 'Tổng tiền đơn hàng không được âm! Giá trị nhận: %', NEW.grand_total;
    END IF;
    RETURN NEW;  -- Cho phép INSERT/UPDATE tiếp tục
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_order_total
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION validate_order_total();
```

**Demo:**
```sql
-- Test: thử insert đơn hàng với giá âm
INSERT INTO orders (id, user_id, order_status, grand_total, payment_method, payment_status)
VALUES (uuid_generate_v4(), '22222222-2222-2222-2222-222222222222',
        'PENDING', -50000, 'CASH', 'UNPAID');
-- → LỖI: "Tổng tiền đơn hàng không được âm!"
```

---

### B4. "Liệt kê tất cả trigger đang có trong database?"

```sql
SELECT trigger_name, event_manipulation, event_object_table, action_timing
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table;
```

| trigger_name | event_manipulation | event_object_table | action_timing |
|---|---|---|---|
| trg_update_product_rating | INSERT | product_reviews | AFTER |
| trg_update_product_rating | UPDATE | product_reviews | AFTER |
| trg_update_product_rating | DELETE | product_reviews | AFTER |
| trg_log_cart_behavior | INSERT | cart_items | AFTER |
| trg_single_default_address | INSERT | user_addresses | BEFORE |
| trg_single_default_address | UPDATE | user_addresses | BEFORE |

---

### B5. "Xóa rồi tạo lại trigger thì làm sao?"

```sql
-- Xóa trigger (chỉ xóa phần gắn, hàm vẫn còn)
DROP TRIGGER IF EXISTS trg_update_product_rating ON product_reviews;

-- Xóa cả hàm trigger
DROP FUNCTION IF EXISTS update_product_rating();

-- Tạo lại từ đầu
CREATE OR REPLACE FUNCTION update_product_rating() ...
CREATE TRIGGER trg_update_product_rating ...
```

---

## Phần C: Câu hỏi về STORED PROCEDURE / FUNCTION

### C1. "Sửa procedure `place_order` để thêm kiểm tra tồn kho"
**Tình huống:** Thầy yêu cầu kiểm tra `inventory_count` trước khi đặt hàng.

```sql
-- Thêm đoạn này VÀO TRONG vòng lặp FOR, ngay sau "IF NOT FOUND..."
-- (dòng 43-45 trong schema-advanced.sql)

-- Kiểm tra tồn kho
IF v_product.inventory_count < (v_item->>'quantity')::INT THEN
    RAISE EXCEPTION 'Sản phẩm "%" chỉ còn % trong kho, không đủ % món',
        v_product.name, v_product.inventory_count, (v_item->>'quantity')::INT;
END IF;
```

**Giải thích:** "Em thêm điều kiện kiểm tra `inventory_count` ngay trước khi INSERT vào `order_items`. Nếu số lượng tồn kho không đủ, hàm sẽ `RAISE EXCEPTION` và toàn bộ transaction bị rollback, đảm bảo không có đơn hàng nào được tạo dở dang."

---

### C2. "Viết procedure mới: Thống kê sản phẩm bán chạy nhất"
**Tình huống:** Thầy yêu cầu viết procedure hoàn toàn mới.

```sql
CREATE OR REPLACE FUNCTION get_top_products(
    p_limit INT DEFAULT 5,
    p_from_date DATE DEFAULT CURRENT_DATE - INTERVAL '30 days',
    p_to_date DATE DEFAULT CURRENT_DATE
)
RETURNS TABLE (
    product_name VARCHAR,
    total_quantity BIGINT,
    total_revenue NUMERIC(14,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        oi.snapshot_product_name::VARCHAR  AS product_name,
        SUM(oi.quantity)                   AS total_quantity,
        SUM(oi.sub_total)                 AS total_revenue
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE o.order_status = 'COMPLETED'
      AND DATE(o.created_at) BETWEEN p_from_date AND p_to_date
    GROUP BY oi.snapshot_product_name
    ORDER BY total_quantity DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;
```

**Demo:**
```sql
-- Top 5 sản phẩm bán chạy trong tháng 4
SELECT * FROM get_top_products(5, '2026-04-01', '2026-04-30');

-- Top 3 sản phẩm bán chạy (dùng giá trị mặc định)
SELECT * FROM get_top_products(3);
```

---

### C3. "Viết procedure tính lương nhân viên theo ca làm"

```sql
CREATE OR REPLACE FUNCTION calculate_employee_payroll(
    p_from_date DATE,
    p_to_date DATE
)
RETURNS TABLE (
    employee_name VARCHAR,
    total_hours NUMERIC(6,2),
    hourly_rate NUMERIC(10,2),
    total_pay NUMERIC(12,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.full_name::VARCHAR                                   AS employee_name,
        SUM(
            EXTRACT(EPOCH FROM (ws.end_time - ws.start_time)) / 3600
        )::NUMERIC(6,2)                                        AS total_hours,
        ws.hourly_rate                                         AS hourly_rate,
        (SUM(
            EXTRACT(EPOCH FROM (ws.end_time - ws.start_time)) / 3600
        ) * ws.hourly_rate)::NUMERIC(12,2)                     AS total_pay
    FROM work_shifts ws
    JOIN users u ON u.id = ws.user_id
    WHERE DATE(ws.start_time) BETWEEN p_from_date AND p_to_date
      AND ws.end_time IS NOT NULL
    GROUP BY u.full_name, ws.hourly_rate
    ORDER BY total_pay DESC;
END;
$$ LANGUAGE plpgsql;
```

---

### C4. "Sự khác nhau giữa FUNCTION và PROCEDURE trong PostgreSQL?"

**Trả lời mẫu:**

| | FUNCTION | PROCEDURE (PG 11+) |
|---|---|---|
| Gọi bằng | `SELECT func()` | `CALL proc()` |
| Trả về giá trị | ✅ Bắt buộc (RETURNS ...) | ❌ Không bắt buộc |
| Dùng trong SELECT | ✅ Có thể | ❌ Không thể |
| Transaction control | ❌ Không thể COMMIT/ROLLBACK bên trong | ✅ Có thể |

> "Trong dự án em sử dụng FUNCTION vì PostgreSQL trước phiên bản 11 không hỗ trợ PROCEDURE, và FUNCTION linh hoạt hơn khi cần trả về kết quả (ví dụ trả về UUID đơn hàng hay bảng báo cáo)."

---

### C5. "Xem mã nguồn của procedure đang có trong DB?"

```sql
-- Cách 1: Dùng pg_proc
SELECT proname, prosrc
FROM pg_proc
WHERE proname IN ('place_order', 'get_revenue_report');

-- Cách 2: Dùng psql shortcut
\df+ place_order
```

---

## Phần D: Câu hỏi tổng hợp / Lý thuyết

### D1. "Trigger và Procedure khác nhau thế nào?"

> "**Procedure** phải được gọi bằng tay (`SELECT func()` hoặc `CALL proc()`), còn **Trigger** tự động kích hoạt khi có thay đổi dữ liệu (INSERT/UPDATE/DELETE). Trigger thực chất là procedure nhưng được database gọi tự động thông qua sự kiện."

### D2. "Transaction có liên quan gì đến Trigger?"

> "Trigger chạy **bên trong** transaction hiện tại. Nếu trigger gây lỗi (RAISE EXCEPTION), toàn bộ transaction sẽ bị rollback, bao gồm cả lệnh INSERT/UPDATE đã kích hoạt trigger đó."

### D3. "Nếu trigger chạy chậm thì ảnh hưởng gì?"

> "Trigger chạy **đồng bộ** (synchronous) nên nó sẽ làm chậm câu INSERT/UPDATE/DELETE gốc. Ví dụ nếu `trg_update_product_rating` phải tính AVG trên 1 triệu review, mỗi lần insert review sẽ rất chậm. Giải pháp: dùng bảng aggregate cache hoặc chuyển sang batch job."

### D4. "Tại sao snapshot giá sản phẩm trong order_items thay vì JOIN?"

> "Vì giá sản phẩm có thể thay đổi theo thời gian. Nếu chỉ lưu `product_id` và JOIN khi cần hiển thị, thì đơn hàng cũ sẽ hiển thị sai giá. Snapshot (`snapshot_unit_price`, `snapshot_product_name`) đảm bảo hóa đơn phản ánh chính xác giá tại thời điểm mua."

---
