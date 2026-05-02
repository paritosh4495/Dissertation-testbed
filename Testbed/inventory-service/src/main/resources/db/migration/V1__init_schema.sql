CREATE SEQUENCE IF NOT EXISTS products_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE products (
    id BIGINT NOT NULL DEFAULT nextval('products_id_seq') PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    author TEXT NOT NULL,
    isbn TEXT UNIQUE,
    description TEXT,
    image_url TEXT,
    price NUMERIC NOT NULL CHECK (price >= 0.01),
    genre TEXT,
    format TEXT,
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'DISCONTINUED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_products_code ON products(code);
CREATE INDEX idx_products_isbn ON products(isbn);
