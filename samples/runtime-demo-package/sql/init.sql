CREATE TABLE IF NOT EXISTS demo_user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

INSERT INTO demo_user (id, name)
VALUES (1, 'runtime-demo')
ON DUPLICATE KEY UPDATE name = VALUES(name);
