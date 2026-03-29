-- Drop old constraint first, then migrate data, then add correct constraint
ALTER TABLE leave_requests DROP CONSTRAINT IF EXISTS leave_type_check;

UPDATE leave_requests SET type = 'PAID'   WHERE type = 'VACATION';
UPDATE leave_requests SET type = 'UNPAID' WHERE type = 'SICK';

ALTER TABLE leave_requests ADD CONSTRAINT leave_type_check
    CHECK (type IN ('PAID', 'UNPAID'));

-- Work requests table
CREATE TABLE IF NOT EXISTS work_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_user_id BIGINT NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    work_type VARCHAR(50) NOT NULL,
    shift_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMPTZ NULL,

    decided_by_user_id BIGINT NULL,
    manager_note VARCHAR(1000) NULL,

    CONSTRAINT fk_work_request_employee FOREIGN KEY (employee_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_work_request_decided_by FOREIGN KEY (decided_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT work_type_check CHECK (work_type IN ('SINGLE', 'MULTIPLE')),
    CONSTRAINT work_shift_type_check CHECK (shift_type IN ('MORNING', 'AFTERNOON', 'NIGHT')),
    CONSTRAINT work_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);