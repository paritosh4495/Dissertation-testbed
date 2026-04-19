CREATE SEQUENCE IF NOT EXISTS orders_id_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS order_items_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE orders (
    id BIGINT NOT NULL DEFAULT nextval('orders_id_seq') PRIMARY KEY,
    order_number TEXT NOT NULL UNIQUE,
    customer_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'PAYMENT_FAILED', 'INVENTORY_REJECTED', 'CANCELLED')),
    total_amount NUMERIC NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE order_items (
    id BIGINT NOT NULL DEFAULT nextval('order_items_id_seq') PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_code TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price NUMERIC NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);
