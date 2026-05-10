CREATE OR REPLACE FUNCTION fn_show_total_customers_generic()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_total bigint;
BEGIN
    EXECUTE format('SELECT COUNT(*) FROM %I.%I', TG_TABLE_SCHEMA, TG_TABLE_NAME)
    INTO v_total;

    RAISE NOTICE 'Tổng khách hàng hiện tại (%I.%I): %', TG_TABLE_SCHEMA, TG_TABLE_NAME, v_total;
    RETURN NULL;
END;
$$;

DO $$
DECLARE
    v_table text;
BEGIN
    SELECT t.table_name
    INTO v_table
    FROM information_schema.tables t
    WHERE t.table_schema = 'public'
      AND t.table_type = 'BASE TABLE'
      AND t.table_name IN ('customers', 'customer', 'khach_hang', 'khachhang')
    ORDER BY CASE t.table_name
        WHEN 'customers'  THEN 1
        WHEN 'customer'   THEN 2
        WHEN 'khach_hang' THEN 3
        WHEN 'khachhang'  THEN 4
        ELSE 99
    END
    LIMIT 1;

    IF v_table IS NULL THEN
        RAISE EXCEPTION 'Không tìm thấy bảng khách hàng trong public (customers/customer/khach_hang/khachhang)';
    END IF;

    EXECUTE format('DROP TRIGGER IF EXISTS trg_show_total_customers ON public.%I', v_table);

    EXECUTE format(
        'CREATE TRIGGER trg_show_total_customers
         AFTER INSERT ON public.%I
         FOR EACH STATEMENT
         EXECUTE FUNCTION fn_show_total_customers_generic()',
        v_table
    );

    RAISE NOTICE 'Đã gắn trigger vào bảng: public.%', v_table;
END;
$$;

