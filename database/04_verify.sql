-- Chỉ đọc. Chạy sau seed để kiểm tra nhanh trên SSMS hoặc sqlcmd.
USE KeebForce_PRJ301;
SET NOCOUNT ON;

SELECT N'UserAccount' AS TableName, COUNT(*) AS [RowCount] FROM dbo.UserAccount
UNION ALL SELECT N'Brand', COUNT(*) FROM dbo.Brand
UNION ALL SELECT N'Category', COUNT(*) FROM dbo.Category
UNION ALL SELECT N'Product', COUNT(*) FROM dbo.Product
UNION ALL SELECT N'WorkshopService', COUNT(*) FROM dbo.WorkshopService
UNION ALL SELECT N'ServiceOrder', COUNT(*) FROM dbo.ServiceOrder
UNION ALL SELECT N'OrderDetail', COUNT(*) FROM dbo.OrderDetail
UNION ALL SELECT N'ServiceReview', COUNT(*) FROM dbo.ServiceReview;

-- Không SELECT PasswordHash ra màn hình hoặc trang web.
SELECT AccountID, Username, Email, FullName, Phone, Role, IsActive, CreatedAt
FROM dbo.UserAccount ORDER BY AccountID;

SELECT t.name AS TableName, c.name AS ConstraintName, c.type_desc AS ConstraintType
FROM sys.objects AS c
JOIN sys.tables AS t ON t.object_id = c.parent_object_id
WHERE c.type IN ('PK', 'UQ', 'F', 'C', 'D')
ORDER BY t.name, c.type_desc, c.name;

-- Tình huống demo của requirement: 200000 + 1200000 + 5000*70 + 300000.
SELECT s.BasePrice + kit.Price + sw.Price * 70 + cap.Price AS DemoTotalVND
FROM dbo.WorkshopService AS s
CROSS JOIN dbo.Product AS kit
CROSS JOIN dbo.Product AS sw
CROSS JOIN dbo.Product AS cap
WHERE s.ServiceName = N'Lắp ráp bàn phím'
    AND kit.ProductName = N'Kit bàn phím 75% demo'
    AND sw.ProductName = N'Switch linear demo'
    AND cap.ProductName = N'Bộ keycap PBT demo';
