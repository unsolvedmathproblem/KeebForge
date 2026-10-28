-- Chạy sau 01_create_database.sql. Toàn bộ schema nằm trong một transaction.
-- Nếu database đã có bảng, dừng; không tự migration hoặc xóa dữ liệu.
USE KeebForce_PRJ301;
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)
        THROW 51001, N'Database đã có bảng. Không thay đổi schema đang tồn tại.', 1;

    CREATE TABLE dbo.UserAccount (
        AccountID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_UserAccount PRIMARY KEY,
        Username VARCHAR(50) NOT NULL CONSTRAINT UQ_UserAccount_Username UNIQUE,
        Email NVARCHAR(100) NOT NULL CONSTRAINT UQ_UserAccount_Email UNIQUE,
        PasswordHash NVARCHAR(255) NOT NULL,
        FullName NVARCHAR(100) NOT NULL,
        Phone VARCHAR(20) NULL,
        Role VARCHAR(10) NOT NULL CONSTRAINT DF_UserAccount_Role DEFAULT 'CUSTOMER',
        IsActive BIT NOT NULL CONSTRAINT DF_UserAccount_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_UserAccount_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_UserAccount_Role CHECK (Role IN ('ADMIN', 'CUSTOMER'))
    );

    CREATE TABLE dbo.Brand (
        BrandID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Brand PRIMARY KEY,
        BrandName NVARCHAR(80) NOT NULL CONSTRAINT UQ_Brand_BrandName UNIQUE
    );

    CREATE TABLE dbo.Category (
        CategoryID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Category PRIMARY KEY,
        CategoryName NVARCHAR(80) NOT NULL CONSTRAINT UQ_Category_CategoryName UNIQUE
    );

    CREATE TABLE dbo.Product (
        ProductID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Product PRIMARY KEY,
        ProductName NVARCHAR(120) NOT NULL,
        CategoryID INT NOT NULL,
        BrandID INT NOT NULL,
        Description NVARCHAR(1000) NULL,
        Price DECIMAL(18,0) NOT NULL,
        ImageURL NVARCHAR(500) NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_Product_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Product_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Product_Category FOREIGN KEY (CategoryID) REFERENCES dbo.Category(CategoryID),
        CONSTRAINT FK_Product_Brand FOREIGN KEY (BrandID) REFERENCES dbo.Brand(BrandID),
        CONSTRAINT CK_Product_Price CHECK (Price >= 0)
    );

    CREATE TABLE dbo.WorkshopService (
        ServiceID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_WorkshopService PRIMARY KEY,
        ServiceName NVARCHAR(120) NOT NULL,
        Description NVARCHAR(1000) NOT NULL,
        BasePrice DECIMAL(18,0) NOT NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_WorkshopService_IsActive DEFAULT 1,
        CONSTRAINT CK_WorkshopService_BasePrice CHECK (BasePrice >= 0)
    );

    CREATE TABLE dbo.ServiceOrder (
        OrderID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ServiceOrder PRIMARY KEY,
        AccountID INT NOT NULL,
        ServiceID INT NOT NULL,
        ServiceNameSnapshot NVARCHAR(120) NOT NULL,
        ServicePrice DECIMAL(18,0) NOT NULL,
        ContactName NVARCHAR(100) NOT NULL,
        ContactPhone VARCHAR(20) NOT NULL,
        DeviceDescription NVARCHAR(1000) NOT NULL,
        PreferredDate DATE NULL,
        CustomerNote NVARCHAR(1000) NULL,
        OrderStatus VARCHAR(12) NOT NULL CONSTRAINT DF_ServiceOrder_OrderStatus DEFAULT 'PENDING',
        AdminNote NVARCHAR(1000) NULL,
        TotalAmount DECIMAL(18,0) NOT NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_ServiceOrder_CreatedAt DEFAULT SYSDATETIME(),
        CompletedAt DATETIME2 NULL,
        CONSTRAINT FK_ServiceOrder_Account FOREIGN KEY (AccountID) REFERENCES dbo.UserAccount(AccountID),
        CONSTRAINT FK_ServiceOrder_Service FOREIGN KEY (ServiceID) REFERENCES dbo.WorkshopService(ServiceID),
        CONSTRAINT CK_ServiceOrder_Status CHECK (OrderStatus IN ('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED')),
        CONSTRAINT CK_ServiceOrder_ServicePrice CHECK (ServicePrice >= 0),
        CONSTRAINT CK_ServiceOrder_TotalAmount CHECK (TotalAmount >= 0)
    );

    CREATE TABLE dbo.OrderDetail (
        OrderDetailID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OrderDetail PRIMARY KEY,
        OrderID INT NOT NULL,
        ProductID INT NOT NULL,
        ProductNameSnapshot NVARCHAR(120) NOT NULL,
        Quantity INT NOT NULL,
        UnitPrice DECIMAL(18,0) NOT NULL,
        CONSTRAINT FK_OrderDetail_Order FOREIGN KEY (OrderID) REFERENCES dbo.ServiceOrder(OrderID),
        CONSTRAINT FK_OrderDetail_Product FOREIGN KEY (ProductID) REFERENCES dbo.Product(ProductID),
        CONSTRAINT UQ_OrderDetail_Order_Product UNIQUE (OrderID, ProductID),
        CONSTRAINT CK_OrderDetail_Quantity CHECK (Quantity BETWEEN 1 AND 200),
        CONSTRAINT CK_OrderDetail_UnitPrice CHECK (UnitPrice >= 0)
    );

    CREATE TABLE dbo.ServiceReview (
        ReviewID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ServiceReview PRIMARY KEY,
        OrderID INT NOT NULL CONSTRAINT UQ_ServiceReview_Order UNIQUE,
        Rating INT NOT NULL,
        Comment NVARCHAR(500) NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_ServiceReview_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ServiceReview_Order FOREIGN KEY (OrderID) REFERENCES dbo.ServiceOrder(OrderID),
        CONSTRAINT CK_ServiceReview_Rating CHECK (Rating BETWEEN 1 AND 5)
    );

    COMMIT TRANSACTION;
    PRINT N'Đã tạo đủ 8 bảng. Tiếp theo chạy 03_seed.sql.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
