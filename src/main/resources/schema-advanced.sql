-- =============================================================
-- Hashiji Cafe – Đối tượng dữ liệu nâng cao
-- Stored Procedures + Triggers + Transaction Demo
-- Trích nguyên bản từ Báo cáo Thực hành HQTCSDL – Nhóm 5
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -------------------------------------------------------------
-- Stored Procedure 1: Tạo đơn hàng (place_order) 
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION place_order( 
    p_user_id UUID, 
    p_address_id UUID, 
    p_promotion_id UUID DEFAULT NULL, 
    p_items JSONB DEFAULT '[]' 
) 
RETURNS UUID AS $$ 
DECLARE 
    v_order_id UUID; 
    v_item JSONB; 
    v_product RECORD; 
    v_sub_total NUMERIC(12,2) := 0; 
    v_discount NUMERIC(12,2) := 0; 
    v_promo RECORD; 
BEGIN 
    -- Kiểm tra tồn tại của address 
    IF NOT EXISTS (SELECT 1 FROM user_addresses WHERE id = p_address_id AND user_id = p_user_id) THEN 
        RAISE EXCEPTION 'Địa chỉ giao hàng không hợp lệ'; 
    END IF; 
     
    -- Tạo order_id 
    v_order_id := uuid_generate_v4(); 
     
    -- Vòng lặp xử lý từng item trong đơn hàng 
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items) LOOP  
        -- Lấy thông tin sản phẩm và kiểm tra tồn kho 
        SELECT p.id, p.name, p.base_price, p.is_available 
        INTO v_product 
        FROM products p 
        WHERE p.id = (v_item->>'product_id')::UUID; 
         
        IF NOT FOUND OR NOT v_product.is_available THEN 
            RAISE EXCEPTION 'Sản phẩm % không khả dụng', v_item->>'product_id'; 
        END IF; 
         
        -- Tính sub_total cho item này 
        v_sub_total := v_sub_total + v_product.base_price * (v_item->>'quantity')::INT; 
         
        -- Ghi vào order_items 
        INSERT INTO order_items (id, order_id, product_id, snapshot_product_name,  
            snapshot_unit_price, quantity, snapshot_options, sub_total) 
        VALUES ( 
            uuid_generate_v4(), v_order_id, v_product.id, 
            v_product.name, v_product.base_price, 
            (v_item->>'quantity')::INT, v_item->'selected_options', 
            v_product.base_price * (v_item->>'quantity')::INT 
        ); 
    END LOOP; 
     
    -- Áp dụng khuyến mãi nếu có 
    IF p_promotion_id IS NOT NULL THEN 
        SELECT discount_type, discount_value, min_order_value 
        INTO v_promo 
        FROM promotions 
        WHERE id = p_promotion_id 
          AND CURRENT_TIMESTAMP BETWEEN start_date AND end_date; 
         
        IF FOUND AND v_sub_total >= v_promo.min_order_value THEN 
            IF v_promo.discount_type = 'PERCENTAGE' THEN 
                v_discount := v_sub_total * v_promo.discount_value / 100; 
            ELSE 
                v_discount := v_promo.discount_value; 
            END IF; 
        END IF; 
    END IF; 
     
    -- Tạo đơn hàng chính 
    INSERT INTO orders (id, user_id, address_id, promotion_id, 
        sub_total, discount_amount, grand_total, 
        order_status, payment_method, payment_status) 
    VALUES ( 
        v_order_id, p_user_id, p_address_id, p_promotion_id, 
        v_sub_total, v_discount, v_sub_total - v_discount, 
        'PENDING', 'COD', 'UNPAID' 
    ); 
     
    RETURN v_order_id; 
END; 
$$ LANGUAGE plpgsql; 

-- -------------------------------------------------------------
-- Stored Procedure 2: Lấy thống kê doanh thu (get_revenue_report) 
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION get_revenue_report( 
    p_from_date DATE, 
    p_to_date   DATE 
) 
RETURNS TABLE ( 
    report_date   DATE, 
    total_orders  BIGINT, 
    total_revenue NUMERIC(14,2), 
    avg_order_val NUMERIC(10,2) 
) AS $$ 
BEGIN 
    RETURN QUERY 
    SELECT 
        DATE(o.created_at)        AS report_date, 
        COUNT(*)                  AS total_orders, 
        SUM(o.grand_total)        AS total_revenue, 
        AVG(o.grand_total)        AS avg_order_val 
    FROM orders o 
    WHERE o.order_status = 'COMPLETED' 
      AND DATE(o.created_at) BETWEEN p_from_date AND p_to_date 
    GROUP BY DATE(o.created_at) 
    ORDER BY report_date; 
END; 
$$ LANGUAGE plpgsql; 

-- -------------------------------------------------------------
-- Trigger 1: Tự động cập nhật điểm đánh giá trung bình của sản phẩm 
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_product_rating() 
RETURNS TRIGGER AS $$ 
BEGIN 
    -- Cập nhật điểm trung bình và số lượng đánh giá cho sản phẩm 
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
 
-- Gắn trigger vào bảng product_reviews 
CREATE OR REPLACE TRIGGER trg_update_product_rating 
    AFTER INSERT OR UPDATE OR DELETE ON product_reviews 
    FOR EACH ROW EXECUTE FUNCTION update_product_rating(); 

-- -------------------------------------------------------------
-- Trigger 2: Tự động ghi log hành vi người dùng khi thêm vào giỏ hàng 
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION log_cart_behavior() 
RETURNS TRIGGER AS $$ 
DECLARE 
    v_user_id UUID; 
BEGIN 
    -- Lấy user_id từ session 
    SELECT user_id INTO v_user_id 
    FROM shopping_sessions 
    WHERE id = NEW.session_id; 
     
    -- Ghi log hành vi 'ADD_TO_CART' với trọng số 0.5 
    IF v_user_id IS NOT NULL THEN 
        INSERT INTO user_behavior_logs 
            (id, user_id, product_id, action_type, action_weight) 
        VALUES 
            (uuid_generate_v4(), v_user_id, NEW.product_id, 'ADD_TO_CART', 0.5);  
    END IF; 
    RETURN NEW; 
END; 
$$ LANGUAGE plpgsql; 
 
CREATE OR REPLACE TRIGGER trg_log_cart_behavior 
    AFTER INSERT ON cart_items 
    FOR EACH ROW EXECUTE FUNCTION log_cart_behavior(); 

-- -------------------------------------------------------------
-- Trigger 3: Kiểm soát địa chỉ mặc định (chỉ có 1 địa chỉ is_default = TRUE) 
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION enforce_single_default_address() 
RETURNS TRIGGER AS $$ 
BEGIN 
    -- Nếu địa chỉ mới được đặt là mặc định, bỏ mặc định của tất cả địa chỉ cũ 
    IF NEW.is_default = TRUE THEN 
        UPDATE user_addresses 
        SET is_default = FALSE 
        WHERE user_id = NEW.user_id 
          AND id <> NEW.id; 
    END IF; 
    RETURN NEW; 
END; 
$$ LANGUAGE plpgsql; 
 
CREATE OR REPLACE TRIGGER trg_single_default_address 
    BEFORE INSERT OR UPDATE ON user_addresses 
    FOR EACH ROW EXECUTE FUNCTION enforce_single_default_address();  

-- -------------------------------------------------------------
-- Transaction 1: Đặt hàng và cập nhật giỏ hàng 
-- -------------------------------------------------------------
/*
BEGIN; 
 
-- Bước 1: Tạo đơn hàng thông qua stored procedure 
SELECT place_order( 
    '550e8400-e29b-41d4-a716-446655440000'::UUID,  -- user_id 
    '6ba7b810-9dad-11d1-80b4-00c04fd430c8'::UUID,  -- address_id 
    NULL,                                           -- promotion_id 
    '[ 
        {"product_id": "abc123", "quantity": 2, "selected_options": {"size": "L", "sugar": "50%"}}, 
        {"product_id": "def456", "quantity": 1, "selected_options": {"size": "M", "ice": "100%"}} 
    ]'::JSONB 
); 
 
-- Bước 2: Xóa giỏ hàng sau khi đặt hàng thành công 
DELETE FROM cart_items 
WHERE session_id = '7c9e6679-7425-40de-944b-e07fc1f90ae7'::UUID; 
 
-- Bước 3: Cập nhật trạng thái phiên mua hàng 
UPDATE shopping_sessions 
SET total_amount = 0 
WHERE id = '7c9e6679-7425-40de-944b-e07fc1f90ae7'::UUID; 
 
COMMIT; 
-- Nếu có lỗi bất kỳ: ROLLBACK;
*/
