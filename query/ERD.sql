CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    full_name   VARCHAR(100),
    role        VARCHAR(50)  NOT NULL,
    email       VARCHAR(100),
    phone       VARCHAR(15),
    hourly_rate DOUBLE PRECISION,
    user_code   VARCHAR(20)  UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE categories (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    name_vi     VARCHAR(50),
    description VARCHAR(255)
);

CREATE TABLE ingredients (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    name            VARCHAR(255)    NOT NULL UNIQUE,
    unit            VARCHAR(255)    NOT NULL,
    stock_quantity  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    cost_per_unit   DOUBLE PRECISION DEFAULT 0.0
);

CREATE TABLE toppings (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    name        VARCHAR(50)     NOT NULL UNIQUE,
    price       DOUBLE PRECISION
);

CREATE TABLE promotions (
    id               UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    discount_type    VARCHAR(50),
    discount_value   NUMERIC(12,2),
    min_order_value  NUMERIC(12,2),
    start_date       TIMESTAMP,
    end_date         TIMESTAMP
);

CREATE TABLE expenses (
    id            UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    description   VARCHAR(255)    NOT NULL,
    amount        DOUBLE PRECISION NOT NULL,
    expense_date  DATE            NOT NULL,
    category      VARCHAR(255)
);

CREATE TABLE job_postings (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    title         VARCHAR(100) NOT NULL,
    location      VARCHAR(100) NOT NULL,
    type          VARCHAR(50)  NOT NULL,
    description   TEXT,
    requirements  TEXT,
    is_active     BOOLEAN      DEFAULT TRUE
);

CREATE TABLE user_addresses (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    user_id       UUID         NOT NULL REFERENCES users(id),
    address_line  VARCHAR(500),
    is_default    BOOLEAN
);

CREATE TABLE shopping_sessions (
    id            UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    user_id       UUID            REFERENCES users(id),
    total_amount  NUMERIC(12,2)
);

CREATE TABLE work_shifts (
    id             UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    user_id        UUID            NOT NULL REFERENCES users(id),
    start_time     TIMESTAMP       NOT NULL,
    end_time       TIMESTAMP,
    start_cash     DOUBLE PRECISION,
    end_cash       DOUBLE PRECISION,
    total_revenue  DOUBLE PRECISION,
    cash_variance  DOUBLE PRECISION,
    status         VARCHAR(50)
);

CREATE TABLE products (
    id             UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    category_id    UUID            REFERENCES categories(id),
    name           VARCHAR(100)    NOT NULL UNIQUE,
    name_vi        VARCHAR(100),
    description    TEXT,
    description_vi TEXT,
    tags           TEXT,
    image          VARCHAR(500),
    base_price     NUMERIC(10,2),
    is_available   BOOLEAN         DEFAULT TRUE,
    avg_rating     NUMERIC(3,2),
    review_count   INTEGER         DEFAULT 0
);

CREATE TABLE product_sizes (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    size_name   VARCHAR(255),
    price       DOUBLE PRECISION,
    product_id  UUID            REFERENCES products(id)
);

CREATE TABLE product_recipes (
    id                UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    product_id        UUID            NOT NULL REFERENCES products(id),
    ingredient_id     UUID            NOT NULL REFERENCES ingredients(id),
    quantity_required DOUBLE PRECISION NOT NULL
);

CREATE TABLE product_reviews (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    product_id    UUID         NOT NULL REFERENCES products(id),
    user_id       UUID         NOT NULL REFERENCES users(id),
    rating_score  INTEGER,
    review_text   TEXT
);

CREATE TABLE cart_items (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    session_id        UUID         NOT NULL REFERENCES shopping_sessions(id),
    product_id        UUID         NOT NULL REFERENCES products(id),
    quantity          INTEGER,
    selected_options  JSONB
);

CREATE TABLE orders (
    id               UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    user_id          UUID            REFERENCES users(id),
    address_id       UUID            REFERENCES user_addresses(id),
    promotion_id     UUID            REFERENCES promotions(id),
    sub_total        NUMERIC(12,2),
    discount_amount  NUMERIC(12,2),
    grand_total      NUMERIC(12,2),
    order_status     VARCHAR(50),
    payment_method   VARCHAR(50),
    payment_status   VARCHAR(50),
    customer_name    VARCHAR(255),
    phone            VARCHAR(255),
    address_text     TEXT,
    note             TEXT,
    total_amount     DOUBLE PRECISION,
    status           VARCHAR(50),
    tracking_code    VARCHAR(255)    UNIQUE,
    order_type       VARCHAR(255)
);

CREATE TABLE order_items (
    id                    UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    order_id              UUID            REFERENCES orders(id),
    product_id            UUID            REFERENCES products(id),
    snapshot_product_name VARCHAR(255),
    snapshot_unit_price   NUMERIC(12,2),
    quantity              INTEGER,
    snapshot_options      JSONB,
    sub_total             NUMERIC(12,2)
);

CREATE TABLE user_behavior_logs (
    id            UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    user_id       UUID            NOT NULL REFERENCES users(id),
    product_id    UUID,
    action_type   VARCHAR(50),
    action_weight NUMERIC(5,2)
);

CREATE TABLE job_applications (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(100) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    job_posting_id  UUID         REFERENCES job_postings(id),
    position        VARCHAR(50)  NOT NULL,
    tracking_code   VARCHAR(50)  UNIQUE,
    cv_url          VARCHAR(500),
    status          VARCHAR(50)  DEFAULT 'NEW'
);
