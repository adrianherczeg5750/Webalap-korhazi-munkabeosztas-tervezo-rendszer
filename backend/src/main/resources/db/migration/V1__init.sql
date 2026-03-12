DROP TABLE IF EXISTS leave_request_dates CASCADE;
DROP TABLE IF EXISTS leave_requests CASCADE;
DROP TABLE IF EXISTS shifts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,

    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'EMPLOYEE'))
);

CREATE TABLE IF NOT EXISTS shifts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shift_date DATE NOT NULL,
    shift_type VARCHAR(50) NOT NULL,

    CONSTRAINT fk_shifts_user_id FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_shifts_user_day UNIQUE (user_id, shift_date),

    CONSTRAINT shifts_type_check CHECK (shift_type IN ('MORNING', 'AFTERNOON', 'NIGHT'))
);

CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_user_id BIGINT NOT NULL,

    start_date DATE NULL,
    end_date DATE NULL,

    type VARCHAR(50) NOT NULL DEFAULT 'VACATION',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMPTZ NULL,

    decided_by_user_id BIGINT NULL,
    manager_note VARCHAR(1000) NULL,

    CONSTRAINT fk_leave_employee FOREIGN KEY (employee_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_leave_decided_by FOREIGN KEY (decided_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT leave_type_check CHECK (type IN ('VACATION', 'SICK', 'UNPAID')),
    CONSTRAINT leave_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS leave_request_dates (
    leave_request_id BIGINT NOT NULL,
    leave_date DATE NOT NULL,

    CONSTRAINT pk_leave_request_dates PRIMARY KEY (leave_request_id, leave_date),

    CONSTRAINT fk_leave_request_dates_request FOREIGN KEY (leave_request_id)
        REFERENCES leave_requests(id)
        ON DELETE CASCADE
);