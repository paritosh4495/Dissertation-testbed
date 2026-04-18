DELETE FROM products;

INSERT INTO products (code, name, author, isbn, price, genre, format, stock_quantity, reserved_quantity, status, created_at, version)
VALUES 
('BK-TH001', 'The Hobbit', 'J.R.R. Tolkien', '9780261102217', 10.99, 'Fantasy', 'PAPERBACK', 50, 0, 'AVAILABLE', CURRENT_TIMESTAMP, 0),
('BK-EJ002', 'Effective Java', 'Joshua Bloch', '9780134685991', 45.00, 'Technology', 'HARDCOVER', 20, 0, 'AVAILABLE', CURRENT_TIMESTAMP, 0),
('BK-HP003', 'Harry Potter', 'J.K. Rowling', '9780747532699', 8.99, 'Fantasy', 'PAPERBACK', 100, 0, 'AVAILABLE', CURRENT_TIMESTAMP, 0),
('BK-EMPTY', 'Empty Book', 'No One', '0000000000', 1.00, 'Drama', 'PAPERBACK', 0, 0, 'AVAILABLE', CURRENT_TIMESTAMP, 0);
