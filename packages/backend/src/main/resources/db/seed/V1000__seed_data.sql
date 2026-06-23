-- Phase B: 初期シードデータ
-- パスワードは BCrypt ハッシュ済み

-- 部署
INSERT INTO departments (id, name, version, created_at, updated_at) VALUES
    ('a0000000-0000-0000-0000-000000000001', '開発部', 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000002', '営業部', 0, NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000003', '人事部', 0, NOW(), NOW());

-- 社員
-- 1. 管理者 (ADMIN) — admin@example.com / demo1234
INSERT INTO employees (id, name, email, password, department_id, role, is_manager, hire_date, version, created_at, updated_at) VALUES
    ('b0000000-0000-0000-0000-000000000001',
     '佐藤 管理',
     'admin@example.com',
     '$2a$10$lbVBVNeRC5o4zif4NMIiIukuiKiZW4eVPwvniwqouezFO32o0gYgm',
     'a0000000-0000-0000-0000-000000000001',
     'ADMIN', true, '2020-04-01', 0, NOW(), NOW());

-- 2. 一般社員 (開発部) — tanaka@example.com / password123
INSERT INTO employees (id, name, email, password, department_id, role, is_manager, hire_date, version, created_at, updated_at) VALUES
    ('b0000000-0000-0000-0000-000000000002',
     '田中 太郎',
     'tanaka@example.com',
     '$2a$10$P91f7KJ5T0sYc9r9WsXfOu26BUz/Rv2FSU7mgl/9A.78s9Gd2090e',
     'a0000000-0000-0000-0000-000000000001',
     'EMPLOYEE', false, '2023-04-01', 0, NOW(), NOW());

-- 3. 営業部上長 — suzuki@example.com / password123
INSERT INTO employees (id, name, email, password, department_id, role, is_manager, hire_date, version, created_at, updated_at) VALUES
    ('b0000000-0000-0000-0000-000000000003',
     '鈴木 花子',
     'suzuki@example.com',
     '$2a$10$P91f7KJ5T0sYc9r9WsXfOu26BUz/Rv2FSU7mgl/9A.78s9Gd2090e',
     'a0000000-0000-0000-0000-000000000002',
     'EMPLOYEE', true, '2021-04-01', 0, NOW(), NOW());

-- 4. 営業部一般社員 — yamada@example.com / password123
INSERT INTO employees (id, name, email, password, department_id, role, is_manager, hire_date, version, created_at, updated_at) VALUES
    ('b0000000-0000-0000-0000-000000000004',
     '山田 一郎',
     'yamada@example.com',
     '$2a$10$P91f7KJ5T0sYc9r9WsXfOu26BUz/Rv2FSU7mgl/9A.78s9Gd2090e',
     'a0000000-0000-0000-0000-000000000002',
     'EMPLOYEE', false, '2024-04-01', 0, NOW(), NOW());
