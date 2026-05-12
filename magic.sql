-- "Transaction hủy đơn và hoàn tiền, cập nhật payment_status"
-- **Đề bài:** Transaction hủy đơn và hoàn tiền, cập nhật payment_status
-- **Yêu cầu:**
-- Viết transaction hủy đơn hàng (CANCELLED) và cập nhật trạng thái thanh toán (REFUNDED)
-- Đảm bảo tính nguyên tử: nếu một bước thất bại thì rollback toàn bộ

BEGIN;

  -- Giả sử hủy đơn có id = 'đặt order_id cụ thể'
  -- Bước 1: Cập nhật trạng thái đơn hàng thành CANCELLED
  UPDATE orders
  SET order_status = 'CANCELLED',
      updated_at = NOW()
  WHERE id = 'order_id_here'
    AND order_status IN ('PENDING', 'CONFIRMED');

  -- Bước 2: Cập nhật trạng thái thanh toán thành REFUNDED nếu đã thanh toán
  UPDATE orders
  SET payment_status = 'REFUNDED',
      updated_at = NOW()
  WHERE id = 'order_id_here'
    AND payment_status = 'PAID';

  -- Kiểm tra nếu không có dòng nào bị ảnh hưởng → rollback
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Không thể hủy đơn: đơn không tồn tại hoặc đã ở trạng thái không hợp lệ';
  END IF;

COMMIT;
----

-- **Phiên bản đóng gói thành Function với xử lý lỗi đầy đủ:**

CREATE OR REPLACE FUNCTION cancel_order_and_refund(p_order_id uuid)
RETURNS void AS $$
BEGIN
  -- Kiểm tra đơn hàng có tồn tại và ở trạng thái hợp lệ không
  IF NOT EXISTS (
    SELECT 1 FROM orders
    WHERE id = p_order_id
      AND order_status IN ('PENDING', 'CONFIRMED')
  ) THEN
    RAISE EXCEPTION 'Đơn hàng không tồn tại hoặc không thể hủy (trạng thái: %)',
      (SELECT order_status FROM orders WHERE id = p_order_id);
  END IF;

  -- Cập nhật trạng thái đơn hàng thành CANCELLED
  UPDATE orders
  SET order_status = 'CANCELLED',
      updated_at = NOW()
  WHERE id = p_order_id;

  -- Cập nhật trạng thái thanh toán thành REFUNDED nếu đã thanh toán
  UPDATE orders
  SET payment_status = 'REFUNDED',
      updated_at = NOW()
  WHERE id = p_order_id
    AND payment_status = 'PAID';

END;
$$ LANGUAGE plpgsql;
----

-- **Gọi function trong transaction:**

BEGIN;
  SELECT cancel_order_and_refund('order-uuid-here');
COMMIT;
----
