# Chạy KeebForge — nền tảng P1-01 và P1-02

Project Java Web with Ant, NetBeans 17, JDK 17, **Tomcat 10.1**
(Servlet 6 / JSP 3.1 / JSTL 3), SQL Server 2019 trở lên.
Đã có schema 8 bảng và tài khoản demo trong database ở P1-02.
Chức năng đăng nhập/đặt dịch vụ chưa được triển khai ở bước này.

## Mở và build

1. NetBeans 17 → Open Project → chọn thư mục KeebForge.
2. Project Properties → Libraries/Java Platform: JDK 17.
3. Project Properties → Run → chọn Tomcat 10.1 đã đăng ký ở máy bạn,
   context path `/KeebForge`. Các đường dẫn server local nằm trong
   `nbproject/private`, không chia sẻ cùng source.
4. Clean and Build hoặc chạy `ant clean dist` khi Ant có trong PATH.
   WAR tạo tại `dist/KeebForge.war`.
5. Run Project hoặc deploy WAR vào Tomcat 10.1, mở
   `http://localhost:8080/KeebForge/`. Trang gốc redirect đến `/home`.

Máy hiện tại có Ant tại
`C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat`.
Ví dụ PowerShell:

```powershell
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' clean dist
```

Giữ `build.xml`, metadata `nbproject` và cấu trúc `src/java`, `web`.
`nbproject/build-impl.xml` đã đồng bộ reference thư viện mới với project.xml;
NetBeans có thể sinh lại file này từ metadata khi đổi Libraries.
Không cần Maven hoặc thư viện bên ngoài thư mục project để build nghiệp vụ.
Servlet/JSP/EL API lấy từ Tomcat để compile, không đóng vào WAR.

## Thư viện đã kèm

| File | Vai trò |
| --- | --- |
| `web/WEB-INF/lib/mssql-jdbc-12.8.0.jre11.jar` | Microsoft JDBC, dùng được trên Java 17 |
| `web/WEB-INF/lib/jakarta.servlet.jsp.jstl-api-3.0.2.jar` | API JSTL 3 |
| `web/WEB-INF/lib/jakarta.servlet.jsp.jstl-3.0.1.jar` | Triển khai JSTL 3 |
| `web/css/bootstrap.min.css` | Bootstrap 5.1.3, có license ở đầu file; parser CSS NetBeans 17 đọc được |

JAR tải từ [Maven Central](https://repo.maven.apache.org/maven2/),
Bootstrap từ [gói Bootstrap 5.1.3](https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css).
JSP dùng URI `jakarta.tags.core`. Trang chạy không cần CDN/font/ảnh online.
JDBC được chọn theo [hướng dẫn Java được hỗ trợ của Microsoft](https://learn.microsoft.com/en-us/sql/connect/jdbc/system-requirements-for-the-jdbc-driver).
JSTL theo [artifact chính thức trên Central](https://central.sonatype.com/artifact/org.glassfish.web/jakarta.servlet.jsp.jstl/3.0.1).

## Cấu hình SQL Server local

1. Tạo `config/db.properties` từ `config/db.properties.example` và điền
   SQL login/password của máy bạn. File thật đã được bỏ qua trong `.gitignore`.
   Không đặt config thật trong `src`, `web`, `WEB-INF/classes` hoặc WAR.
2. Cấu hình TCP/IP và port SQL Server theo máy bạn. Mẫu dùng localhost:1433,
   database mới `KeebForce_PRJ301`; xem các script **P1-02** bên dưới.
   Không chạy script thay schema database cũ.
3. Đặt biến môi trường `KEEBFORGE_DB_CONFIG` bằng đường dẫn tuyệt đối đến file
   thật, trước khi khởi động NetBeans/Tomcat. Hoặc thêm Java VM option cho
   Tomcat: `-Dkeebforge.db.config=D:/source_code/PRJ301/KeebForge/config/db.properties`.
   System property được ưu tiên nếu có cả hai. Khởi động lại server sau khi đổi.
4. Mẫu bật TLS với `trustServerCertificate=true` để làm bài local bằng chứng chỉ
   tự ký; khi dùng chứng chỉ hợp lệ thì chuyển thành `false`.

`DBContext` chỉ mở kết nối, không tạo DB/bảng và không tự chạy SQL.
Thiếu cấu hình/driver hoặc kết nối lỗi sẽ ném `SQLException`, không giữ connection null.
Mỗi DAO về sau `extends DBContext`, tạo cục bộ trong Servlet bằng
try-with-resources; DAO tự đóng PreparedStatement/ResultSet và DBContext đóng
connection. Không giữ DAO/connection trong field dùng chung của Servlet.
Không đưa lỗi SQL/config hoặc mật khẩu lên JSP.

## Các file để học từ bước này

- `src/java/dal/DBContext.java`: đọc cấu hình local, mở/đóng kết nối SQL Server.
- `config/db.properties.example`: mẫu để mỗi thành viên tự cấu hình máy mình.
- `web/index.jsp`: chuyển trình duyệt đến `/home`.
- `web/WEB-INF/web.xml`: cấu hình UTF-8, welcome và mapping `/home` tạm thời
  vào JSP. P2 sẽ bỏ mapping `WelcomePage` và dùng ProductServlet cho `/home`.
- `web/WEB-INF/views/home.jsp`: trang giới thiệu workshop, chưa đọc DB.
- `web/WEB-INF/views/common/header.jsp`, `footer.jsp`: phần đầu/cuối tái sử dụng.
- `web/css/style.css`: màu, chữ, thẻ nội dung và bố cục theo kích thước màn hình.
  Bàn phím minh họa được vẽ bằng HTML/CSS, không cần file ảnh.
- `.gitignore`: bỏ qua output build, cấu hình máy và mật khẩu local.

P1-01 tạo DBContext; P1-02 thêm Account và PasswordUtil, tổng cộng **3 class Java**.
Chưa tạo DAO/Servlet/Filter hay package rỗng để chờ các bước sau.
Nav chỉ trỏ đến nội dung đã có.
`web/index.html` cũ chỉ là trang TODO của NetBeans, đã được thay bằng index.jsp;
không có class nghiệp vụ cũ cần chuyển đổi.

## Kết quả kiểm tra P1-01 ngày 28/10/2026

- `ant clean dist`: BUILD SUCCESSFUL với JDK 17.0.12.
- WAR chỉ có DBContext và ba JAR nêu trên; không có Servlet API hoặc config thật.
- Deploy thử WAR trên Tomcat 10.1.57 với CATALINA_BASE riêng trong `build/tomcat`;
  không thay cấu hình cài đặt Tomcat dùng chung.
- `/KeebForge/`, `/KeebForge/home`, hai file CSS: HTTP 200; HTML UTF-8 có tiếng Việt,
  JSTL được xử lý; JSP trong WEB-INF trả 404 khi truy cập trực tiếp.
- DBContext: thiếu đường dẫn cấu hình ném SQLException như dự kiến.
- SQL Server local: `sqlcmd -S tcp:localhost,1433 -E -C` chạy SELECT được;
  database mới chưa có. Không chạy CREATE/DROP/ALTER/INSERT/UPDATE/DELETE.
- Chưa xác nhận kết nối JDBC thành công: chưa có SQL login/config của ứng dụng.
  Windows login của sqlcmd không tự trở thành SQL login cho JDBC.
- Chưa kiểm tra trực quan desktop/mobile: công cụ browser/native UI không khả dụng.
  CSS đã có breakpoint 991px/767px, cần duyệt trên trình duyệt thật.
- Chưa thao tác Run/Clean and Build qua UI NetBeans; build bằng Ant đi kèm NetBeans.
- Thư mục hiện tại không có Git repository; chỉ thử đọc git status,
  không thực hiện thao tác Git ghi dữ liệu.
- Đã dừng Tomcat chạy thử và chạy lại `ant clean dist` thành công để bàn giao
  WAR cuối cùng. Server thử nghiệm hiện không còn chạy; dùng Run Project để xem.

Requirement và progress được đọc/ghi ở thư mục ngoài project theo yêu cầu.

## Sửa báo đỏ CSS trong NetBeans 17

- Sửa selector sai `a: hover` và `a: focus-visible` thành `a:hover` và
  `a:focus-visible` trong style.css. Khoảng trắng sau dấu `:` ở selector
  khiến CSS không hợp lệ; khoảng trắng sau dấu `:` ở thuộc tính vẫn đúng.
- Dùng Bootstrap **5.1.3** chính thức thay cho 5.3.3. Bootstrap 5.3.3 có
  16 khai báo biến CSS rỗng hợp lệ với trình duyệt nhưng parser NetBeans 17
  báo `Unexpected token SEMI`. Bản 5.1.3 không gặp lỗi parser này và có đầy đủ
  các class Bootstrap trang hiện tại sử dụng. Không chỉnh nội dung thư viện,
  không tắt kiểm tra lỗi CSS của IDE.
- Đã chạy trực tiếp ExtCss3Lexer/ExtCss3Parser trong các JAR cài đặt NetBeans 17:
  cả style.css và bootstrap.min.css đều **0 lỗi parser**. Ant clean dist thành công.
  Build Ant không tự kiểm tra cú pháp CSS nên cần kiểm tra parser riêng này.
- Nếu IDE đang giữ dấu đỏ từ bản trước, đóng/mở lại hai tab CSS hoặc mở lại
  project để NetBeans cập nhật. Chưa quan sát được trạng thái icon qua UI
  vì công cụ computer-use không khả dụng trong phiên này.

## P1-02 — tạo database và seed

Các script trong `database/` là source SQL của project, không phải bản sao
requirement. Chạy lần lượt, dừng ngay nếu một script báo lỗi:

| Script | Vai trò |
| --- | --- |
| `01_create_database.sql` | Tạo riêng DB mới `KeebForce_PRJ301`; dừng nếu tên này đã có |
| `02_schema.sql` | Tạo 8 bảng và PK/FK/UNIQUE/CHECK/DEFAULT trong một transaction; dừng nếu DB đã có bảng |
| `03_seed.sql` | Thêm dữ liệu demo trong một transaction; dừng nếu bất kỳ bảng nào đã có dữ liệu |
| `04_verify.sql` | Chỉ đọc: đếm dữ liệu, xem account không có hash, xem constraints và tổng tiền demo |

Trong SSMS: kết nối SQL Server của máy bạn, mở từng file theo thứ tự trên rồi
Execute. Không cần bật SQLCMD Mode; các script không dùng lệnh riêng của sqlcmd.
Hoặc từ thư mục project, dùng Windows Authentication:

```powershell
sqlcmd -S tcp:localhost,1433 -E -C -b -f 65001 -i database/01_create_database.sql
sqlcmd -S tcp:localhost,1433 -E -C -b -f 65001 -i database/02_schema.sql
sqlcmd -S tcp:localhost,1433 -E -C -b -f 65001 -i database/03_seed.sql
sqlcmd -S tcp:localhost,1433 -E -C -b -f 65001 -i database/04_verify.sql
```

`-E` dùng tài khoản Windows hiện tại, `-C` tin chứng chỉ local, `-b` trả lỗi
khi script thất bại, `-f 65001` đọc file UTF-8 để giữ tiếng Việt.
Máy hiện tại đã chạy xong cả bốn script. Không chạy lại bước 01–03 trên DB này;
script sẽ báo lỗi bảo vệ 51000/51001/51002. Nếu tạo DB được nhưng bước schema
thất bại, transaction schema rollback; sửa lỗi rồi chạy lại bước 02 trên DB trống.
Nếu seed thất bại, seed rollback; sửa lỗi rồi chạy lại bước 03.
Không có script DROP, tự đổi schema cũ hoặc reset dữ liệu.

Seed gồm 3 tài khoản, 3 brand (Keychron/Akko/Gateron), 5 category,
10 product và 4 WorkshopService. Giá chỉ để làm bài.
ServiceOrder, OrderDetail, ServiceReview ban đầu có 0 dòng.
ImageURL là NULL; P2 sẽ bổ sung placeholder local khi làm catalog.
Tình huống demo: công lắp ráp 200.000 + kit 1.200.000 + 70 switch × 5.000
+ keycap 300.000 = **2.050.000đ**.

| Tài khoản website demo | Mật khẩu demo | Role |
| --- | --- | --- |
| `admin` | `Admin@123` | ADMIN |
| `customer1` | `Customer@123` | CUSTOMER |
| `customer2` | `Customer@123` | CUSTOMER |

Đây là tài khoản giả lập của **website**, không phải SQL Server login.
Chỉ đăng nhập được vào website sau khi làm P1-03.
SQL Server login thật vẫn do từng thành viên điền vào config local;
không tạo login SQL hoặc đổi quyền SQL Server trong bước này.

### PasswordUtil và hash seed

`src/java/util/PasswordUtil.java` dùng API có sẵn của Java 17:
PBKDF2WithHmacSHA256, 600.000 vòng, salt ngẫu nhiên 16 byte, hash 32 byte.
Mức vòng lặp tham khảo [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
Không thêm JAR hash hoặc tự viết thuật toán mã hóa.

```java
String hash = PasswordUtil.hashPassword(password);
boolean correct = PasswordUtil.verifyPassword(password, hash);
```

Chuỗi lưu theo định dạng `pbkdf2_sha256$iterations$Base64(salt)$Base64(hash)`
vừa cột NVARCHAR(255). Hash seed được sinh trực tiếp bằng class này;
hai customer có cùng mật khẩu demo nhưng salt/hash khác nhau.
Verifier trả false khi đầu vào null/rỗng, hash sai cấu trúc/Base64/độ dài,
hoặc số vòng ngoài khoảng 600.000–1.200.000; so sánh hash bằng
MessageDigest.isEqual. Chính sách độ dài mật khẩu nhập ở form sẽ làm tại P1-03.
Không hiển thị PasswordHash trên JSP hoặc log mật khẩu.

### Account và quy ước bàn giao P2/P3

`src/java/model/Account.java` chỉ là JavaBean, không truy vấn DB hoặc xử lý đăng nhập.

| Cột SQL | Property Java | Getter |
| --- | --- | --- |
| AccountID | int accountId | getAccountId() |
| Username | String username | getUsername() |
| Email | String email | getEmail() |
| PasswordHash | String passwordHash | getPasswordHash() |
| FullName | String fullName | getFullName() |
| Phone | String phone, có thể null | getPhone() |
| Role | String role | getRole() |
| IsActive | boolean active | isActive() |
| CreatedAt | LocalDateTime createdAt | getCreatedAt() |

DAO về sau map DATETIME2 bằng `rs.getTimestamp("CreatedAt").toLocalDateTime()`.
Session đăng nhập thống nhất `user` là `model.Account`, role là chuỗi
`ADMIN`/`CUSTOMER`, không thêm enum/DTO. P1-03 mới lưu session và thực hiện auth.
P2/P3 có thể dùng schema/catalog seed, DBContext và Account ngay; tự thêm Model/DAO
của phần mình khi triển khai chức năng, không cần đợi toàn bộ P1.
Mỗi DAO tạo cục bộ theo request, đóng bằng try-with-resources như phần DBContext.

SQL chỉ kiểm tra tập giá trị trạng thái và các ràng buộc bảng cơ bản.
P3 vẫn cần kiểm tra owner, đơn COMPLETED trước khi review, chuyển trạng thái hợp lệ
và tính tổng trong OrderDAO. Không có trigger hay engine thực hiện thay DAO.
ID không cần liên tiếp: transaction rollback có thể tiêu thụ số IDENTITY.

### Kiểm tra thực tế P1-02

- SQL Server local 17.0.1000.7 (SQL Server 2025, đáp ứng 2019+), chạy script
  thật bằng sqlcmd qua TCP localhost:1433; chưa chạy qua giao diện SSMS.
- Đúng 8 bảng, số dòng seed 3/3/5/10/4/0/0/0, tổng demo 2.050.000đ.
- JDBC Microsoft 12.8.0 trên Java 17 đọc được tiếng Việt và map account từ DB;
  xác minh 3 hash seed đúng, mật khẩu sai bị từ chối, salt ngẫu nhiên hoạt động,
  hash hỏng/null/rỗng bị từ chối.
- SQL thực sự chặn dữ liệu trùng tên/email, role/status sai, FK sai, giá âm,
  quantity ngoài 1–200, rating ngoài 1–5 và review/product trùng trong một đơn.
  Các dòng kiểm tra được rollback; ba bảng đơn/chi tiết/review vẫn 0 dòng.
- Chạy lại script 01/02/03 bị chặn lần lượt bởi 51000/51001/51002; không đổi seed.
- JDBC kiểm tra dùng Windows Authentication và DLL Microsoft ký hợp lệ
  `mssql-jdbc_auth-12.8.0.x64.dll` tải từ
  [artifact chính thức trên Maven Central](https://central.sonatype.com/artifact/com.microsoft.sqlserver/mssql-jdbc_auth/12.8.0.x64).
  DLL và chương trình kiểm tra chỉ nằm tạm trong build, không thêm vào WAR/source
  runtime, không thay config của ứng dụng.
- DBContext của ứng dụng vẫn giữ hướng SQL login trong mẫu config; chưa có config
  thật để kiểm tra login bằng DBContext hoặc từ Tomcat. Không đồng nhất lần kiểm tra
  JDBC Windows trên với việc đã cấu hình kết nối cho web app.
- Chưa test đăng nhập website; chức năng đó thuộc P1-03. Không đổi UI/CSS ở P1-02.
- Ant `clean dist` cuối bước thành công. Deploy WAR cuối trên Tomcat 10.1.57
  với CATALINA_BASE riêng trong build, port thử 127.0.0.1:18080:
  welcome/home và hai CSS trả HTTP 200, tiếng Việt đúng. Đã dừng server thử.
  WAR chỉ có DBContext/Account/PasswordUtil và 3 JAR runtime; không có SQL,
  config thật, DLL hoặc chương trình kiểm tra trong WAR.
