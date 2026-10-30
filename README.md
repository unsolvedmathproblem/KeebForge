# KeebForce — Assignment

Workshop bàn phím cho đồ án PRJ301.
- **P1 (đã nghiệm thu):** đăng ký/login/logout, hồ sơ cá nhân, phân quyền/CSRF và quản lý tài khoản Admin.
- **P2 (đã nghiệm thu):** trang chủ đọc DB, catalog linh kiện (tìm kiếm/lọc/phân trang/ảnh dự phòng), danh sách dịch vụ & đọc đánh giá, quản trị linh kiện và quản trị dịch vụ Admin.
- **P3 (đến P3-06):** gửi yêu cầu với giá/snapshot từ DB, transaction/chống gửi trùng; lịch sử/hủy PENDING; Admin quản lý đơn/dashboard; khách đánh giá một lần cho đơn hoàn tất và xem review trên trang dịch vụ. Chờ nghiệm thu toàn luồng P3-07.

Thành viên nhóm: **Nguyễn Văn Lê Hào, Nguyễn Anh Tiền, Vũ Hoàng Nguyên**.

## Chạy project

1. Mở thư mục này bằng **NetBeans 17**, chọn **JDK 17** và **Tomcat 10.1**.
2. Trên SQL Server 2019+, nếu chưa có database KeebForce_PRJ301, chạy lần lượt
   `database/01_create_database.sql`, `02_schema.sql`, `03_seed.sql`.
   Máy đã có DB/dữ liệu thì không chạy lại các script tạo/seed; dùng `04_verify.sql`.
3. Copy `config/db.properties.example` (SQL login) hoặc `db-windows.properties.example`
   thành **TOMCAT_BASE/conf/keebforce-db.properties** rồi điền cấu hình máy bạn.
   Windows auth cần DLL/setenv theo [docs/setup.md](docs/setup.md).
   DBContext tự đọc file này; không cần đặt VM Option cho đường dẫn DB.
4. Clean and Build rồi Run Project. Mở `http://localhost:8080/KeebForce/`.

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
P3 thêm GET/POST `/request?serviceId=...`, GET `/orders` (lịch sử cá nhân), POST `/orders action=cancel/review`.
Admin thêm GET/POST `/admin/orders` (action=status/note), GET `/admin/dashboard`.
Mọi path ở sau context `/KeebForce`.

## Tài liệu và bản bàn giao

- [Setup](docs/setup.md): môi trường, JAR, cấu hình DB/Tomcat và cách chạy SQL.
- [UI dùng chung](docs/ui-guide.md): JSP include, CSS, message, CSRF và money.tag.
- [Báo cáo bàn giao P1](docs/handover-p1.md): kiểm tra cuối, artifact và giới hạn P1.
- [Báo cáo bàn giao P2](docs/handover-p2.md): model/getter/DAO bàn giao cho P3, kết quả kiểm tra và hướng dẫn chụp ảnh P2.
- [P3-03](docs/p3-03.md): gửi yêu cầu, transaction/giá từ DB, token một lần và kiểm tra JDBC/HTTP.
- [P3-04](docs/p3-04.md): lịch sử/chi tiết theo chủ đơn, hủy PENDING và kiểm tra quyền.
- [P3-05](docs/p3-05.md): Admin đơn/trạng thái/ghi chú, dashboard và kiểm tra đồng thời.
- [P3-06](docs/p3-06.md): review đúng owner/completed/một lần và ghép trang dịch vụ P2.
- `dist/KeebForce.war`: bản deploy.
- `dist/KeebForce-P1-source.zip`: source P1 kèm metadata/SQL/JAR/docs,
  không chứa cấu hình máy hoặc mật khẩu SQL thật.

Kiến trúc: Servlet gọi DAO trực tiếp, Account JavaBean và JSP/JSTL.
Không Maven, service layer, DTO/interface hoặc framework CRUD.
Requirement và progress được quản lý ở thư mục bên ngoài repository;
không đóng requirement vào source/WAR.
