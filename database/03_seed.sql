-- Dữ liệu GIẢ LẬP cho đồ án; giá không phải giá thị trường.
-- Hash sinh bằng util.PasswordUtil (PBKDF2-HMAC-SHA256, salt riêng mỗi tài khoản).
-- admin / Admin@123; customer1, customer2 / Customer@123.
-- Đây là tài khoản WEBSITE, không phải SQL Server login.
USE KeebForce_PRJ301;
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (SELECT 1 FROM dbo.UserAccount)
        OR EXISTS (SELECT 1 FROM dbo.Brand)
        OR EXISTS (SELECT 1 FROM dbo.Category)
        OR EXISTS (SELECT 1 FROM dbo.Product)
        OR EXISTS (SELECT 1 FROM dbo.WorkshopService)
        OR EXISTS (SELECT 1 FROM dbo.ServiceOrder)
        OR EXISTS (SELECT 1 FROM dbo.OrderDetail)
        OR EXISTS (SELECT 1 FROM dbo.ServiceReview)
        THROW 51002, N'Database đã có dữ liệu. Dừng seed để không chèn trùng hoặc ghi đè.', 1;

    INSERT INTO dbo.UserAccount (Username, Email, PasswordHash, FullName, Phone, Role)
    VALUES
        ('admin', N'admin@example.com', N'pbkdf2_sha256$600000$xPklXsohiwV+vOnMDIEHCw==$G8fGkVjcPj+i7+Y3aIdxjl+o3oVFjgfTG1Eu6zPQyKE=', N'Quản trị workshop', '0900000001', 'ADMIN'),
        ('customer1', N'customer1@example.com', N'pbkdf2_sha256$600000$ReklB2cRIULS3oAEYmwNNg==$5lyYCBvCF2u8+2MVLz4oEYP5ZQKpmcHNZ7y2SPdgqDg=', N'Nguyễn Minh An', '0900000002', 'CUSTOMER'),
        ('customer2', N'customer2@example.com', N'pbkdf2_sha256$600000$w7X8wKvcvYk2z6ryKIdIyA==$GhEsvmXv6nvsKpAJ7v/jaCDqqT4Y9TNih9ucmsyHXMU=', N'Trần Hà Linh', NULL, 'CUSTOMER');

    INSERT INTO dbo.Brand (BrandName)
    VALUES (N'Keychron'), (N'Akko'), (N'Gateron');

    INSERT INTO dbo.Category (CategoryName)
    VALUES (N'Keyboard/Kit'), (N'Switch'), (N'Keycap'), (N'Stabilizer'), (N'Accessory');

    DECLARE @Keychron INT = (SELECT BrandID FROM dbo.Brand WHERE BrandName = N'Keychron');
    DECLARE @Akko INT = (SELECT BrandID FROM dbo.Brand WHERE BrandName = N'Akko');
    DECLARE @Gateron INT = (SELECT BrandID FROM dbo.Brand WHERE BrandName = N'Gateron');
    DECLARE @Kit INT = (SELECT CategoryID FROM dbo.Category WHERE CategoryName = N'Keyboard/Kit');
    DECLARE @Switch INT = (SELECT CategoryID FROM dbo.Category WHERE CategoryName = N'Switch');
    DECLARE @Keycap INT = (SELECT CategoryID FROM dbo.Category WHERE CategoryName = N'Keycap');
    DECLARE @Stabilizer INT = (SELECT CategoryID FROM dbo.Category WHERE CategoryName = N'Stabilizer');
    DECLARE @Accessory INT = (SELECT CategoryID FROM dbo.Category WHERE CategoryName = N'Accessory');

    -- ImageURL để NULL; P2 dùng ảnh placeholder local khi bổ sung catalog.
    INSERT INTO dbo.Product (ProductName, CategoryID, BrandID, Description, Price)
    VALUES
        (N'Kit bàn phím 75% demo', @Kit, @Keychron, N'Kit dùng để thực hành chọn linh kiện và tính tổng đơn. Chưa bao gồm switch và keycap.', 1200000),
        (N'Switch linear demo', @Switch, @Gateron, N'Switch mẫu, giá tính theo một switch. Khách tự chọn số lượng cần dùng.', 5000),
        (N'Bộ keycap PBT demo', @Keycap, @Akko, N'Một bộ keycap mẫu cho tình huống lắp ráp trong bài. Mô tả nhu cầu bố cục khi gửi yêu cầu.', 300000),
        (N'Kit bàn phím 65% demo', @Kit, @Akko, N'Kit nhỏ gọn phục vụ dữ liệu catalog minh họa, chưa bao gồm switch và keycap.', 1000000),
        (N'Bàn phím cơ có dây demo', @Kit, @Keychron, N'Bàn phím hoàn chỉnh dùng để tham khảo trong catalog workshop.', 1500000),
        (N'Switch tactile demo', @Switch, @Akko, N'Switch có khấc phản hồi, giá tính theo một switch.', 6000),
        (N'Bộ keycap sáng màu demo', @Keycap, @Keychron, N'Keycap mẫu tông sáng; mô tả bố cục bàn phím trước khi lựa chọn.', 450000),
        (N'Bộ stabilizer demo', @Stabilizer, @Gateron, N'Bộ cân bằng phím dài dùng trong dữ liệu lắp ráp mẫu.', 180000),
        (N'Cáp USB-C demo', @Accessory, @Keychron, N'Cáp kết nối có dây, dữ liệu phụ kiện minh họa.', 120000),
        (N'Dụng cụ nhổ switch demo', @Accessory, @Gateron, N'Dụng cụ hỗ trợ tháo switch cho bàn phím phù hợp.', 50000);

    INSERT INTO dbo.WorkshopService (ServiceName, Description, BasePrice)
    VALUES
        (N'Lắp ráp bàn phím', N'Lắp ráp một bộ bàn phím từ kit, switch và keycap khách lựa chọn. Công cố định cho một bộ; linh kiện tính riêng. Khách mô tả rõ thiết bị và nhu cầu.', 200000),
        (N'Vệ sinh bàn phím', N'Vệ sinh cơ bản một bàn phím, làm sạch keycap và bề mặt. Không bao gồm sửa bo mạch hoặc phục hồi thiết bị vào nước.', 150000),
        (N'Lube switch theo bộ', N'Lube một bộ switch tối đa 110 chiếc có thể tháo rời. Khách ghi loại và số lượng switch; công cố định theo gói.', 150000),
        (N'Thay switch cơ bản', N'Thay switch trên một bàn phím hot-swap theo nhu cầu. Không bao gồm hàn bo mạch; switch thay thế tính riêng nếu chọn của workshop.', 100000);

    -- Chưa tạo đơn/chi tiết/đánh giá: các bảng này sẽ có dữ liệu khi làm P3.
    COMMIT TRANSACTION;
    PRINT N'Đã seed: 3 tài khoản, 3 brand, 5 category, 10 product, 4 dịch vụ.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
