CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS shifts (
    id BIGSERIAL PRIMARY KEY,
    employee_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    start_at_date DATE NOT NULL,
    start_at_time TIME NOT NULL,
    end_at_date DATE NOT NULL,
    end_at_time TIME NOT NULL,
    user_id BIGINT NOT NULL,

    CONSTRAINT fk_shifts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);