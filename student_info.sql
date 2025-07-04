DROP DATABASE IF EXISTS student_info;
CREATE DATABASE student_info;
USE student_info;
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role ENUM('Admin', 'Staff', 'Student') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students (
    roll_no VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    dob DATE,
    email VARCHAR(255) UNIQUE NOT NULL,
    address VARCHAR(255),
    gender VARCHAR(10),
    phone VARCHAR(15),
    dept VARCHAR(50)
);

CREATE TABLE attendance (
    student_id VARCHAR(20),
    month VARCHAR(20),
    year INT,
    working_days INT,
    attended_days INT,
    percentage DECIMAL(5,2),
    PRIMARY KEY (student_id, month, year),
    FOREIGN KEY (student_id) REFERENCES students(roll_no) ON DELETE CASCADE
);

CREATE TABLE marks (
    student_id VARCHAR(20),
    semester INT,
    subject_code VARCHAR(20),
    subject_name VARCHAR(100),
    grade VARCHAR(2),
    grade_point DECIMAL(3,1),
    credit DECIMAL(3,1),
    PRIMARY KEY (student_id, semester, subject_code),
    FOREIGN KEY (student_id) REFERENCES students(roll_no) ON DELETE CASCADE
);

CREATE TABLE fees (
    roll_no VARCHAR(20) PRIMARY KEY,
    amount_due DECIMAL(10,2),
    amount_paid DECIMAL(10,2),
    balance DECIMAL(10,2),
    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE
);

SELECT * FROM users;
SELECT * FROM students;
SELECT * FROM attendance;
SELECT * FROM marks;
SELECT * FROM fees;