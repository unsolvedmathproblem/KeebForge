-- Chạy một lần bằng SSMS hoặc sqlcmd, với quyền tạo database.
-- Không xóa, thay schema hay dùng lại database đã có.
USE master;

IF DB_ID(N'KeebForce_PRJ301') IS NOT NULL
    THROW 51000, N'Database KeebForce_PRJ301 đã tồn tại. Dừng để bảo vệ dữ liệu; không chạy lại bước tạo DB.', 1;

EXEC(N'CREATE DATABASE KeebForce_PRJ301');
PRINT N'Đã tạo database mới KeebForce_PRJ301. Tiếp theo chạy 02_schema.sql.';
