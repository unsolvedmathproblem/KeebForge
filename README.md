<<<<<<< HEAD
# ⌨️ KeebForge — Keyboard Workshop
=======
# KeebForce — Assignment
>>>>>>> 1c05161 (change project name)

<p align="center">
  <strong>PROJECT MÔN HỌC PRJ301 · FPT UNIVERSITY</strong>
  <br />
  <em>A web application for a mechanical keyboard workshop</em>
  <br /><br />
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Apache_Tomcat-10.1-f8dc75?style=for-the-badge&logo=apachetomcat&logoColor=black" alt="Tomcat 10.1" />
  <img src="https://img.shields.io/badge/SQL_Server-2019%2B-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white" alt="SQL Server" />
  <img src="https://img.shields.io/badge/NetBeans-17-1B6AC6?style=for-the-badge&logo=apachenetbeanside&logoColor=white" alt="NetBeans 17" />
</p>

---

## 📖 Giới thiệu

<<<<<<< HEAD
**KeebForge** là dự án website workshop bàn phím cơ được phát triển trong khuôn khổ môn học **PRJ301 — Java Web Application Development** tại **Đại học FPT**.
=======
1. Mở thư mục này bằng **NetBeans 17**, chọn **JDK 17** và **Tomcat 10.1**.
2. Trên SQL Server 2019+, nếu chưa có database KeebForce_PRJ301, chạy lần lượt
   `database/01_create_database.sql`, `02_schema.sql`, `03_seed.sql`.
   Máy đã có DB/dữ liệu thì không chạy lại các script tạo/seed; dùng `04_verify.sql`.
3. Copy `config/db.properties.example` (SQL login) hoặc `db-windows.properties.example`
   thành **TOMCAT_BASE/conf/keebforce-db.properties** rồi điền cấu hình máy bạn.
   Windows auth cần DLL/setenv theo [docs/setup.md](docs/setup.md).
   DBContext tự đọc file này; không cần đặt VM Option cho đường dẫn DB.
4. Clean and Build rồi Run Project. Mở `http://localhost:8080/KeebForce/`.
>>>>>>> 1c05161 (change project name)

Dự án hướng đến việc xây dựng nền tảng web có cấu trúc rõ ràng, áp dụng kiến thức Java Web, Servlet, JSP, JDBC và SQL Server vào một ứng dụng thực tế.

Phiên bản hiện tại tập trung hoàn thiện hệ thống tài khoản, xác thực người dùng, quản lý hồ sơ cá nhân và chức năng quản trị tài khoản. Các tính năng catalog sản phẩm và luồng đơn hàng được định hướng phát triển trong các giai đoạn tiếp theo.

## 👥 Thành viên nhóm

| STT | Họ và tên |
|:---:|---|
| 01 | **Vũ Hoàng Nguyên** |
| 02 | **Nguyễn Anh Tiền** |
| 03 | **Nguyễn Văn Lê Hào** |

## 🚀 Tiến độ dự án

| Giai đoạn | Nội dung | Trạng thái |
|---|---|---|
| P1 | Đăng ký, đăng nhập, đăng xuất, hồ sơ cá nhân, phân quyền, CSRF và quản lý tài khoản Admin | ✅ Đã nghiệm thu |
| P2 | Catalog sản phẩm | ⏳ Chưa triển khai |
| P3 | Luồng đơn hàng | ⏳ Chưa triển khai |

> **Phạm vi phiên bản hiện tại:** P1 đã được người dùng nghiệm thu và kết thúc. Không xem các chức năng P2/P3 là tính năng đã hoàn thành.

## ✨ Tính năng P1

### 🔐 Xác thực và tài khoản
- Đăng ký tài khoản khách hàng.
- Đăng nhập và đăng xuất.
- Quản lý phiên đăng nhập.
- Xem và cập nhật hồ sơ cá nhân.

### 🛡️ Bảo mật và phân quyền
- Phân quyền giữa `ADMIN` và `CUSTOMER`.
- Kiểm soát quyền truy cập các chức năng quản trị.
- Bảo vệ các thao tác thay đổi dữ liệu bằng CSRF token.
- Tách biệt cấu hình kết nối cơ sở dữ liệu khỏi mã nguồn.

### ⚙️ Quản trị hệ thống
- Quản lý tài khoản người dùng thông qua khu vực Admin.
- Sử dụng SQL Server để lưu trữ dữ liệu.
- Cung cấp script khởi tạo, dữ liệu mẫu và kiểm tra cơ sở dữ liệu.

## 🧰 Công nghệ sử dụng

| Công nghệ | Vai trò |
|---|---|
| Java 17 | Ngôn ngữ lập trình chính |
| Java Servlet | Xử lý request và điều hướng |
| JSP / JSTL | Xây dựng giao diện động |
| JDBC | Kết nối và truy vấn dữ liệu |
| SQL Server 2019+ | Hệ quản trị cơ sở dữ liệu |
| Apache Tomcat 10.1 | Môi trường triển khai ứng dụng web |
| Apache Ant | Biên dịch và đóng gói dự án |
| NetBeans 17 | IDE phát triển |

### Kiến trúc ứng dụng

Dự án sử dụng kiến trúc Java Web truyền thống, phù hợp với phạm vi môn học PRJ301:

- **Servlet:** tiếp nhận request và xử lý luồng điều khiển.
- **DAO:** thực hiện truy vấn và thao tác dữ liệu.
- **JavaBean:** biểu diễn dữ liệu tài khoản.
- **JSP / JSTL:** hiển thị giao diện người dùng.

Ứng dụng không sử dụng Maven, service layer, DTO/interface hoặc framework CRUD. Các thành phần được tổ chức theo cấu trúc hiện có của dự án.

## 🖥️ Yêu cầu môi trường

Trước khi chạy dự án, hãy chuẩn bị:

- [NetBeans IDE 17](https://netbeans.apache.org/)
- [JDK 17](https://adoptium.net/)
- [Apache Tomcat 10.1](https://tomcat.apache.org/)
- Microsoft SQL Server 2019 trở lên
- SQL Server Management Studio (SSMS) hoặc công cụ quản lý SQL tương đương

## 📦 Cài đặt và chạy dự án

### Bước 1. Mở project

Mở thư mục repository bằng NetBeans 17, sử dụng JDK 17 và cấu hình Apache Tomcat 10.1 làm server chạy ứng dụng.

### Bước 2. Chuẩn bị cơ sở dữ liệu

Mở SQL Server và tạo cơ sở dữ liệu theo các script trong thư mục `database/`.

Nếu chưa có database `KeebForce_PRJ301`, hãy chạy lần lượt:

1. `database/01_create_database.sql`
2. `database/02_schema.sql`
3. `database/03_seed.sql`

Nếu database đã tồn tại và có dữ liệu, **không chạy lại các script tạo hoặc seed dữ liệu**. Sử dụng `database/04_verify.sql` để kiểm tra trạng thái.

### Bước 3. Cấu hình kết nối database

Sao chép một trong các file cấu hình mẫu:

- `config/db.properties.example` — sử dụng SQL Server login.
- `config/db-windows.properties.example` — dành cho cấu hình Windows Authentication tương ứng.

Đặt file cấu hình đã chỉnh sửa tại:

`TOMCAT_BASE/conf/keebforge-db.properties`

Điền thông tin kết nối SQL Server phù hợp với máy của bạn.

Nếu sử dụng Windows Authentication, hãy tham khảo hướng dẫn DLL và `setenv` trong [`docs/setup.md`](docs/setup.md).

> Không commit mật khẩu database thật, file cấu hình cá nhân hoặc DLL cục bộ lên GitHub.

### Bước 4. Build và chạy ứng dụng

Trong NetBeans:

1. Chọn **Clean and Build**.
2. Chọn **Run Project**.
3. Mở trình duyệt tại địa chỉ:

**http://localhost:8080/KeebForge/**

Nếu cổng hoặc context path đã được thay đổi, hãy sử dụng URL tương ứng với cấu hình Tomcat của bạn.

### Build bằng Apache Ant trên Windows

Có thể build dự án bằng lệnh sau nếu Ant được cài kèm NetBeans tại đường dẫn mặc định:

```powershell
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' clean dist
```

Lệnh trên thực hiện clean và đóng gói dự án. Để triển khai, sử dụng file WAR được tạo ra trong thư mục `dist/`.

## 🔑 Tài khoản demo

Các tài khoản website giả lập phục vụ kiểm thử:

<<<<<<< HEAD
| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | `ADMIN` |
| `customer1` | `Customer@123` | `CUSTOMER` |
| `customer2` | `Customer@123` | `CUSTOMER` |
=======
Đây là tài khoản website giả lập, không phải SQL Server login.
Các route hiện có: `/home`, `/login`, `/register`, `/profile`, POST `/logout`,
`/admin/users`. Mọi path ở sau context `/KeebForce`.
>>>>>>> 1c05161 (change project name)

**Lưu ý:** Đây là tài khoản của ứng dụng KeebForge, không phải tài khoản đăng nhập SQL Server. Các tài khoản chỉ khả dụng khi dữ liệu mẫu tương ứng đã được khởi tạo và mật khẩu khớp với cấu hình xác thực của ứng dụng.

<<<<<<< HEAD
## 🧭 Các endpoint hiện có
=======
- [Setup](docs/setup.md): môi trường, JAR, cấu hình DB/Tomcat và cách chạy SQL.
- [UI dùng chung](docs/ui-guide.md): JSP include, CSS, message, CSRF và money.tag.
- [Báo cáo bàn giao P1](docs/handover-p1.md): kiểm tra cuối, artifact và giới hạn.
- `dist/KeebForce.war`: bản deploy.
- `dist/KeebForce-P1-source.zip`: source P1 kèm metadata/SQL/JAR/docs,
  không chứa cấu hình máy hoặc mật khẩu SQL thật.
>>>>>>> 1c05161 (change project name)

Các đường dẫn dưới đây nằm sau context path `/KeebForge`.

| Endpoint | Mục đích |
|---|---|
| `/home` | Trang chủ |
| `/login` | Đăng nhập |
| `/register` | Đăng ký |
| `/profile` | Hồ sơ cá nhân |
| `/logout` | Đăng xuất bằng HTTP POST |
| `/admin/users` | Quản lý tài khoản Admin |

Ví dụ: `http://localhost:8080/KeebForge/login`

## 📁 Cấu trúc repository

```text
KeebForge/
├── database/
│   ├── 01_create_database.sql
│   ├── 02_schema.sql
│   ├── 03_seed.sql
│   └── 04_verify.sql
├── config/
│   ├── db.properties.example
│   └── db-windows.properties.example
├── docs/
│   ├── setup.md
│   ├── ui-guide.md
│   └── handover-p1.md
├── nbproject/
├── dist/
│   ├── KeebForge.war
│   └── KeebForge-P1-source.zip
└── README.md
```

*Đây là cấu trúc tham chiếu cho các thành phần chính; vị trí một số file có thể thay đổi theo cấu hình repository.*

### Quy ước quản lý mã nguồn

- Giữ metadata `nbproject` cần thiết để các thành viên có thể mở và build project.
- Không commit `nbproject/private`, thư mục `build`, cấu hình database thật hoặc DLL riêng của máy.
- Requirement và tiến độ chi tiết được quản lý bên ngoài repository, không đóng gói vào source hoặc WAR.

## 📚 Tài liệu và bản bàn giao

| Tài liệu / Artifact | Nội dung |
|---|---|
| [`docs/setup.md`](docs/setup.md) | Môi trường, JAR, cấu hình database/Tomcat và hướng dẫn chạy SQL |
| [`docs/ui-guide.md`](docs/ui-guide.md) | JSP include, CSS, message, CSRF và `money.tag` |
| [`docs/handover-p1.md`](docs/handover-p1.md) | Kiểm tra cuối, artifact và giới hạn của P1 |
| `dist/KeebForge.war` | Gói triển khai ứng dụng web |
| `dist/KeebForge-P1-source.zip` | Source P1 kèm metadata, SQL, JAR và tài liệu bàn giao |

Các artifact trong `dist/` chỉ có thể sử dụng sau khi đã được tạo thành công bằng quy trình build của dự án.

## 🎯 Định hướng phát triển

Các giai đoạn tiếp theo được định hướng như sau:

- **P2 — Product Catalog:** phát triển chức năng catalog sản phẩm.
- **P3 — Order Flow:** phát triển luồng xử lý đơn hàng.

Đây là định hướng phát triển, không phải danh sách tính năng đã có trong phiên bản P1.

## 🎓 Mục tiêu học tập

Thông qua dự án KeebForge, nhóm hướng đến việc vận dụng kiến thức Java Web vào một sản phẩm có thể build và triển khai, thực hành kết nối cơ sở dữ liệu, tổ chức mã nguồn, quản lý phiên và phân quyền, đồng thời phối hợp nhóm thông qua Git và GitHub.

---

<p align="center">
  <strong>KEEBFORGE</strong>
  <br />
  <em>Built with Java · Developed for PRJ301 at FPT University</em>
  <br /><br />
  <sub>Project by Vũ Hoàng Nguyên · Nguyễn Anh Tiền · Nguyễn Văn Lê Hào</sub>
</p>
