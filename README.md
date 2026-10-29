# KeebForge — Assignment

Workshop bàn phím cho đồ án PRJ301.
- **P1 (đã nghiệm thu):** đăng ký/login/logout, hồ sơ cá nhân, phân quyền/CSRF và quản lý tài khoản Admin.
- **P2 (đã nghiệm thu):** trang chủ đọc DB, catalog linh kiện (tìm kiếm/lọc/phân trang/ảnh dự phòng), danh sách dịch vụ & đọc đánh giá, quản trị linh kiện và quản trị dịch vụ Admin.
- **P3:** luồng gửi yêu cầu dịch vụ, lịch sử/hủy/đánh giá đơn, quản trị đơn và dashboard (chưa ghép vào nhánh này).

Thành viên nhóm: **Nguyễn Văn Lê Hào, Nguyễn Anh Tiền, Vũ Hoàng Nguyên**.

## Chạy project

1. Mở thư mục này bằng **NetBeans 17**, chọn **JDK 17** và **Tomcat 10.1**.
2. Trên SQL Server 2019+, nếu chưa có database KeebForce_PRJ301, chạy lần lượt
   `database/01_create_database.sql`, `02_schema.sql`, `03_seed.sql`.
   Máy đã có DB/dữ liệu thì không chạy lại các script tạo/seed; dùng `04_verify.sql`.
3. Copy `config/db.properties.example` (SQL login) hoặc `db-windows.properties.example`
   thành **TOMCAT_BASE/conf/keebforge-db.properties** rồi điền cấu hình máy bạn.
   Windows auth cần DLL/setenv theo [docs/setup.md](docs/setup.md).
   DBContext tự đọc file này; không cần đặt VM Option cho đường dẫn DB.
4. Clean and Build rồi Run Project. Mở `http://localhost:8080/KeebForge/`.

Ant đi kèm NetBeans trên máy hiện tại:

```powershell
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' clean dist
```

Giữ metadata `nbproject` dùng chung để thành viên khác mở/build project;
`nbproject/private`, `build`, `dist`, config DB thật và DLL local được ignore.

## Tài khoản demo

| Username | Password demo | Role |
| --- | --- | --- |
| admin | Admin@123 | ADMIN |
| customer1 | Customer@123 | CUSTOMER |
| customer2 | Customer@123 | CUSTOMER |

Đây là tài khoản website giả lập, không phải SQL Server login.
Các route hiện có: `/home`, `/parts`, `/services`, `/login`, `/register`, `/profile`,
POST `/logout`, `/admin/products`, `/admin/services`, `/admin/users`.
Mọi path ở sau context `/KeebForge`.

## Tài liệu và bản bàn giao

- [Setup](docs/setup.md): môi trường, JAR, cấu hình DB/Tomcat và cách chạy SQL.
- [UI dùng chung](docs/ui-guide.md): JSP include, CSS, message, CSRF và money.tag.
- [Báo cáo bàn giao P1](docs/handover-p1.md): kiểm tra cuối, artifact và giới hạn P1.
- [Báo cáo bàn giao P2](docs/handover-p2.md): model/getter/DAO bàn giao cho P3, kết quả kiểm tra và hướng dẫn chụp ảnh P2.
- `dist/KeebForge.war`: bản deploy.
- `dist/KeebForge-P1-source.zip`: source P1 kèm metadata/SQL/JAR/docs,
  không chứa cấu hình máy hoặc mật khẩu SQL thật.

Kiến trúc: Servlet gọi DAO trực tiếp, Account JavaBean và JSP/JSTL.
Không Maven, service layer, DTO/interface hoặc framework CRUD.
Requirement và progress được quản lý ở thư mục bên ngoài repository;
không đóng requirement vào source/WAR.
