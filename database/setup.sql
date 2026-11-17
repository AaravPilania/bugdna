-- Run once as root in MySQL Command Line Client. Change the password locally; do not commit real values.
CREATE DATABASE IF NOT EXISTS bugdna CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'bugdna'@'localhost' IDENTIFIED BY 'CHANGE_ME_LOCAL_ONLY';
GRANT ALL PRIVILEGES ON bugdna.* TO 'bugdna'@'localhost';
-- MySQL 8 has binary logging on by default; without this, a non-root user gets
-- ERROR 1419 when Flyway creates triggers.
SET PERSIST log_bin_trust_function_creators = 1;
FLUSH PRIVILEGES;
