truncate table products;


INSERT INTO products (code, name, author, isbn, description, price, genre, format, stock_quantity, reserved_quantity, status)
VALUES
    ('BK-TH001', 'The Hobbit', 'J.R.R. Tolkien', '9780261102217', 'A classic high-fantasy novel.', 10.99, 'Fantasy', 'PAPERBACK', 50, 0, 'AVAILABLE'),
    ('BK-EJ002', 'Effective Java', 'Joshua Bloch', '9780134685991', 'Best practices for the Java platform.', 45.00, 'Technology', 'HARDCOVER', 20, 0, 'AVAILABLE'),
    ('BK-HP003', 'Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', '9780747532699', 'The first book in the Harry Potter series.', 8.99, 'Fantasy', 'PAPERBACK', 100, 0, 'AVAILABLE'),
    ('BK-1984', '1984', 'George Orwell', '9780451524935', 'A dystopian social science fiction novel.', 7.50, 'Dystopian', 'PAPERBACK', 0, 0, 'AVAILABLE'),
    ('BK-GTM', 'Green Eggs and Ham', 'Dr. Seuss', '9780394800165', 'A children''s book by Dr. Seuss.', 5.99, 'Children', 'HARDCOVER', 15, 0, 'AVAILABLE');