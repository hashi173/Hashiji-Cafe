# Magic SQL Solver

Auto-solve SQL exercises using OpenRouter API.

## Setup

```bash
cd scripts
cp .env.example .env
# Edit .env, add your OpenRouter API key
```

## Run

```bash
cd /path/to/Hashiji-Cafe
source .venv/bin/activate
python scripts/magic_solver.py
```

## Input format (`magic.sql`)

Each block has 2 lines, separated by `----`:

```
viết trigger tự động cập nhật giá khi thay đổi
SELECT * FROM products WHERE id = 'xxx';
----
viết function tính tổng đơn hàng
SELECT get_order_total('uuid-here');
----
```

## Output format (3 lines per block)

```
viết trigger tự động cập nhật giá khi thay đổi
CREATE OR REPLACE FUNCTION ... $$ LANGUAGE plpgsql;
SELECT * FROM products WHERE id = 'xxx';
----
```

## Rules

- PostgreSQL syntax only (NOT T-SQL)
- Trigger functions MUST use NEW/OLD variables
- No comments explaining code meaning
- Each query pair separated by `----`
