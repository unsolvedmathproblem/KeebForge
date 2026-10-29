# Chạy KeebForge — P1-01 đến P1-07

**P1 đã được người dùng nghiệm thu và kết thúc ngày 28/10/2026.**
Hướng dẫn dưới đây phục vụ bàn giao/chạy lại; chưa bao gồm triển khai P2/P3.

Project Java Web with Ant, NetBeans 17, JDK 17, **Tomcat 10.1**
(Servlet 6 / JSP 3.1 / JSTL 3), SQL Server 2019 trở lên.
Đã có schema 8 bảng, auth, AuthFilter/CSRF, cập nhật hồ sơ và quản lý tài khoản Admin.
Chưa triển khai catalog/đặt dịch vụ thuộc P2/P3.
Hướng dẫn ghép JSP, form, bảng và tiền VND dùng chung: [ui-guide.md](ui-guide.md).
Kiểm tra cuối và artifact bàn giao: [handover-p1.md](handover-p1.md).

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

1. Copy mẫu SQL login `config/db.properties.example` hoặc mẫu Windows auth
   `config/db-windows.properties.example` thành **TOMCAT_BASE/conf/keebforge-db.properties**.
   Điền giá trị của máy bạn vào file đó, không đặt config thật trong source/web/WAR.
2. Cấu hình TCP/IP và port SQL Server theo máy bạn. Mẫu dùng localhost:1433,
   database mới `KeebForce_PRJ301`; xem các script **P1-02** bên dưới.
   Không chạy script thay schema database cũ.
3. DBContext tự tìm file trong conf của Tomcat đang chạy. Thứ tự ưu tiên:
   system property `keebforge.db.config` → env `KEEBFORGE_DB_CONFIG` →
   `${catalina.base}/conf/keebforge-db.properties`. Hai tùy chọn đầu chỉ cần khi
   muốn dùng vị trí khác hoặc chạy chương trình console ngoài Tomcat.
   File `config/db.properties` local cũ vẫn dùng được nếu chỉ định đường dẫn rõ.
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

P1-01 tạo DBContext; P1-02 thêm Account/PasswordUtil; P1-03 thêm AccountDAO/AuthServlet,
tổng cộng 5 class Java ở P1-03. P1-04 thêm AuthFilter, P1-05 thêm AdminUserServlet,
hiện có **7 class Java**;
không tạo package/class rỗng cho các bước sau.
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

## P1-03 — đăng ký, đăng nhập, đăng xuất và hồ sơ chỉ đọc

### Chạy trên máy hiện tại bằng NetBeans 17

Đã sửa cấu hình của đúng Tomcat NetBeans hiện tại tại `D:/apache-tomcat-10.1.57`:

- `conf/keebforge-db.properties`: Windows auth, database KeebForce_PRJ301.
- `conf/keebforge-native/mssql-jdbc_auth-12.8.0.x64.dll`: DLL Microsoft ký hợp lệ.
- `bin/setenv.bat`: đặt java.library.path tới thư mục DLL trên, được catalina.bat
  gọi tự động khi NetBeans/CLI khởi động server.

DBContext tự đọc file trong conf. Máy này không còn cần nhập VM Option đường dẫn DB
bằng tay. Dừng/khởi động lại Tomcat khi bổ sung setenv/DLL lần đầu.
Không đóng config thật/DLL local vào Git, source ZIP hoặc WAR.

Trên máy khác:

- **SQL login**: copy mẫu SQL vào conf/keebforge-db.properties và điền login/password.
  Không cần DLL hoặc setenv dành cho Windows auth.
- **Windows auth**: copy mẫu Windows vào conf/keebforge-db.properties, đặt DLL x64
  đúng 12.8.0 vào TOMCAT_HOME/conf/keebforge-native, copy setenv.bat.example vào
  TOMCAT_HOME/bin/setenv.bat. Nếu file setenv đã có thì giữ các setting cũ, chỉ thêm
  setting cần thiết. Đường dẫn có khoảng trắng cần đặt trong dấu ngoặc kép phù hợp.
- Tài khoản Windows chạy Tomcat cần quyền dùng DB như khi chạy sqlcmd. Không tạo
  SQL login hoặc tăng quyền server trong lần sửa này.
- Nếu muốn giữ cách VM Options cũ, keebforge.db.config vẫn được ưu tiên hơn file conf;
  kiểm tra/bỏ giá trị cũ sai đường dẫn nếu server vẫn báo không đọc được config.

Tùy chọn Windows Authentication là cấu hình bổ sung của DBContext, không đổi cách
Servlet gọi DAO và không thêm thư viện vào WAR.

### Vai trò file và luồng login

- `dao/AccountDAO.java`: SQL PreparedStatement, đọc account theo username/ID,
  kiểm tra username/email đã dùng và insert khách hàng mới. DAO ép role CUSTOMER,
  IsActive=1 trong SQL. P1-04 đã thêm updateProfile; P1-05 đã thêm
  getAllAccounts/updateStatus.
- `controller/AuthServlet.java`: nhận GET/POST, kiểm tra form, gọi DAO trực tiếp,
  kiểm tra PasswordUtil, quản lý session và forward/redirect.
- `login.jsp`, `register.jsp`, `profile.jsp` trong WEB-INF/views: hiển thị form,
  thông báo và hồ sơ bằng JSTL/EL; dùng c:out cho dữ liệu người dùng.
- `common/header.jsp`: menu theo trạng thái đăng nhập và nút logout bằng POST;
  link về phần giới thiệu luôn về đúng /home, kể cả từ trang tài khoản.
- `web.xml`: session chỉ theo cookie, HttpOnly, không đưa JSESSIONID lên URL.
- `style.css`: form có label/hint, thông báo lỗi/thành công, profile và responsive.
  Giữ footer Assignment và tên ba thành viên đã chốt.

Ví dụ `customer1` đăng nhập:

1. Form POST `/login` gửi username/password đến AuthServlet.
2. Servlet kiểm tra đầu vào, tạo AccountDAO cục bộ trong try-with-resources.
3. DAO SELECT bằng `WHERE Username = ?`, map ResultSet thành Account.
4. Servlet dùng PasswordUtil.verifyPassword, kiểm tra active, xóa hash khỏi bean
   đưa vào session; đổi session ID rồi lưu `session.user = Account`.
5. Redirect `/profile`; Servlet đọc lại account bằng ID trong session, sau đó
   forward `profile.jsp`. JSP chỉ hiển thị dữ liệu, không tự query DB.

Tài khoản demo: `admin / Admin@123`, `customer1 / Customer@123`,
`customer2 / Customer@123`. Admin và Customer hiện đều vào hồ sơ;
trang quản trị và đơn sẽ được thêm theo các bước sau.

### Quy tắc form và route hiện có

- Username đăng ký: 3–50 ký tự `[A-Za-z0-9_]`; email tối đa 100 ký tự;
  fullName bắt buộc, tối đa 100; phone có thể bỏ trống hoặc 7–20 ký tự gồm
  số/dấu điện thoại thông dụng; mật khẩu 8–128 ký tự và nhập lại phải khớp.
- Trim các thông tin chữ; không trim mật khẩu. Chỉ giữ username/email/fullName/phone
  khi form lỗi, không đổ password/confirmPassword vào HTML.
- Register không nhận role/active/accountId từ browser. UNIQUE trong DB bảo vệ
  request đồng thời; lỗi trùng trả thông báo tiếng Việt. Thành công redirect về
  `/login?registered=1`, không tự đăng nhập.
- Login thành công đổi session ID, session key `user`, không lưu passwordHash
  trong session. Sai thông tin trả lỗi chung; tài khoản active=false không được login.
- GET `/profile` cần user trong session, lấy lại account bằng ID đó;
  `?accountId=...` không đổi chủ hồ sơ. Account bị khóa/không còn thì hủy session.
- POST `/logout` invalidate session rồi redirect login. GET `/logout` trả 405.
- Ở mốc P1-03, POST `/profile` trả 405; P1-04 bên dưới đã thêm action=update.
- Response tài khoản có Cache-Control: no-store. Lỗi kết nối/SQL trả 503 và
  thông báo chung tiếng Việt, chi tiết chỉ log phía server.
- Mốc P1-03 chưa có AuthFilter/CSRF và profile update; phần P1-04 dưới đây
  ghi kết quả triển khai các chức năng này.

### Kết quả kiểm tra P1-03

- Ant build/deploy Tomcat 10.1.57 + JDK17 + SQL Server thật, cấu hình Windows auth
  local. Đã kiểm tra qua chính AuthServlet → AccountDAO → DBContext, không chỉ
  chương trình JDBC console.
- **55 kiểm tra HTTP/SQL đạt**: trang public/JSP WEB-INF, validate server,
  form giữ giá trị an toàn, login đúng/sai/user không có, cả ba seed account,
  hash đăng ký, khóa/mở account thử, session ID/HttpOnly/logout/owner,
  HTML injection và hai request register trùng đồng thời.
- Giả lập mất kết nối bằng config riêng của server thử: UI trả 503 tiếng Việt,
  không stack trace/JDBC URL; khôi phục config thì login lại được.
- Tài khoản tạo bởi bộ kiểm tra được xóa đúng username/email riêng; dữ liệu seed
  vẫn 3 account active, đơn vẫn 0. Không đổi schema hoặc database cũ.
- NetBeans 17 CSS parser: style.css và Bootstrap 5.1.3 đều 0 lỗi parser.
- Chưa kiểm tra bố cục trực quan desktop/mobile: computer-use không có browser
  và native pipe không khả dụng. Chưa thực hiện Run qua UI NetBeans.
- Bản cuối `ant clean dist` thành công, WAR 5.464.542 bytes, đã deploy và kiểm tra
  lại login/profile/menu/logout/register từ chính WAR vừa build. Server thử đã dừng.
  WAR có đúng 5 class production và 3 JAR, không chứa config local/DLL/SQL/harness.

## P1-04 — Filter, CSRF và cập nhật hồ sơ

`filter/AuthFilter.java` là một filter duy nhất map `/*`, mặc định chỉ chạy
ở request từ trình duyệt, không chạy lặp khi Servlet forward/include JSP.
Filter đặt UTF-8 trước khi đọc form và xử lý theo thứ tự:

1. GET/HEAD tới `/css/`, `/js/`, `/images/`, favicon đi thẳng tới tài nguyên,
   không tạo session hoặc truy vấn DB.
2. Các request khác dùng token ngẫu nhiên 32 byte ở `session.csrfToken`.
   Mọi form POST gửi hidden input tên `csrf`. Response trang đặt no-store.
3. `/profile` cần login; `/request`, `/orders` chỉ CUSTOMER;
   `/admin` và `/admin/*` chỉ ADMIN. Filter lấy lại Account từ DB trước mỗi
   request bảo vệ, cập nhật role/session và bỏ hash khỏi bean trong session.
4. User không login được redirect `/login`; account không còn/đã khóa thì hủy
   session. Sai role hoặc token CSRF trả 403. DB không truy cập được thì trả 503,
   không cho request bảo vệ tiếp tục với dữ liệu session cũ.
5. Token POST được so sánh bằng MessageDigest.isEqual. Token ổn định trong cùng
   session để dùng ở nhiều form/tab; sau login token cũ bị bỏ, GET tiếp theo tạo
   token mới. Logout hủy session nên token cũ không còn dùng được.

Form JSP về sau của P2/P3 chỉ cần thêm:

```jsp
<input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
```

Không tự tạo token CSRF thứ hai trong Servlet/helper. CSRF không thay cho token
chống gửi đơn trùng của P3. Khi không còn session, POST tới route bảo vệ bị
redirect login trước khi xử lý form; POST public có token cũ/không token trả 403.

`common/error.jsp` là trang thực dùng cho lỗi Filter, có thông báo tiếng Việt,
header/footer chung và link về home; không hiển thị stack trace hoặc dữ liệu SQL.
Menu hiện có profile/logout cho user, nhãn Quản trị viên cho ADMIN; link tới
quản lý tài khoản/đơn sẽ chỉ thêm khi các trang đó được triển khai.

### Sửa hồ sơ

- `AccountDAO.updateProfile(accountId, fullName, phone)` dùng UPDATE chỉ hai cột
  FullName/Phone, WHERE AccountID và IsActive=1, kiểm tra số dòng cập nhật.
- `AuthServlet` nhận POST `/profile`, `action=update`. AccountID lấy từ session;
  bỏ qua các giá trị accountId/role/email/password/active do browser tự gửi.
- Họ tên bắt buộc, tối đa 100 ký tự; phone có thể trống hoặc phải theo cùng
  quy tắc đăng ký. Phone trống lưu NULL. Mọi giá trị chữ được trim.
- Lỗi form giữ lại giá trị và escape khi hiển thị; không đổi Account trong session
  trước khi SQL thành công. Thành công cập nhật session rồi redirect
  `/profile?updated=1`; GET tiếp theo hiển thị thông báo và dữ liệu mới từ Filter.
- AuthFilter đã đọc lại account nên GET profile không query DB lần thứ hai.
- GET `/logout` vẫn trả 405. POST logout cần CSRF hợp lệ.

### Kết quả kiểm tra P1-04

- Ant build/deploy thật trên Tomcat 10.1.57, JDK17 và SQL Server local.
- **76 kiểm tra HTTP/SQL đạt**: login/register regression với CSRF; token
  thiếu/sai/khác session/cũ sau login và logout; role/customer/admin; account
  bị khóa và role đổi có hiệu lực ở request bảo vệ kế tiếp; sửa hồ sơ tiếng Việt,
  NULL phone, validate và escape HTML; IDOR/tamper accountId/role/email/password.
- Kiểm tra cookie phiên đã bị hủy/phát lại để mô phỏng session không còn hợp lệ;
  chưa đợi đủ 30 phút để kiểm tra timeout tự nhiên.
- Đã kiểm tra trực tiếp /admin và /admin/users, gồm path có dấu `;`: CUSTOMER
  nhận 403. ADMIN đi qua Filter và nhận 404 vì chức năng Admin chưa làm ở P1-05.
  Tương tự CUSTOMER được đi qua Filter /request,/orders nhưng chức năng P3 chưa có.
- Giả lập DB mất kết nối: protected route trả 503, trang login/static vẫn mở;
  khôi phục DB thì profile hoạt động lại.
- Các tài khoản test được dọn đúng username/email, 3 account seed vẫn active,
  tên/role không đổi, đơn vẫn 0. Không thay schema/database cũ.
- Cả bốn form hiện có đều chứa csrf. NetBeans 17 parser: style.css và Bootstrap
  5.1.3 đều 0 lỗi. Chưa kiểm tra trực quan desktop/mobile hoặc Run qua UI NetBeans
  do computer-use không có browser/native pipe khả dụng.
- Sau kiểm tra đã dừng Tomcat thử, chạy lại Ant clean dist thành công.
  WAR cuối 5.475.488 bytes, 6 class production + 3 JAR, không config local/DLL/SQL/test.

## P1-05 — Quản lý tài khoản Admin

Đăng nhập `admin / Admin@123`, chọn **Tài khoản** trên menu hoặc mở
`/KeebForge/admin/users`. Trang dùng header/footer/style chung, bảng cuộn ngang
trên màn hình hẹp; trạng thái có cả màu và chữ.

- `controller/admin/AdminUserServlet.java`: GET danh sách, POST `action=toggle`.
  Servlet kiểm tra session ADMIN, accountId là số nguyên dương, active chỉ nhận
  đúng `true` hoặc `false`. Không nhận role/email/password/fullName để cập nhật.
- `AccountDAO.getAllAccounts()`: List<Account> theo AccountID, map bằng JavaBean
  đã có. SQL trả NULL thay cho hash trong kết quả danh sách; không lấy hash thật.
- `AccountDAO.updateStatus(accountId, active)`: chỉ UPDATE IsActive bằng
  PreparedStatement, kiểm tra affected rows. Không có DELETE hoặc đổi role.
- `admin/users.jsp`: thông tin liên hệ, vai trò, trạng thái và ngày tham gia;
  dữ liệu được c:out escape. Mỗi thao tác có csrf, action, accountId và active.
- Menu **Tài khoản** chỉ có ở ADMIN. AuthFilter vẫn bảo vệ server, nên tự gõ URL
  hoặc gửi POST từ CUSTOMER đều bị chặn dù có token CSRF của chính họ.

Admin đang đăng nhập không có nút tự khóa; Servlet cũng chặn nếu sửa form thủ công,
trả 400 và thông báo tiếng Việt. Tài khoản không tồn tại trả 404. Sai ID/action/active
trả 400. Lỗi DB trả 503; chi tiết chỉ log server. Lưu thành công redirect
`/admin/users?updated=1` để refresh không gửi lại POST.

`active` là **trạng thái đích**, không phải lệnh đảo BIT. Gửi lại cùng yêu cầu khóa
vẫn giữ trạng thái khóa, gửi lại mở vẫn giữ trạng thái mở. GET không đổi trạng thái,
kể cả khi tự thêm query `action=toggle`. Không có action xóa tài khoản.

Khóa tài khoản giữ toàn bộ thông tin/đơn lịch sử. AuthFilter từ P1-04 đọc lại
IsActive ở request bảo vệ tiếp theo và hủy session của user bị khóa.
Mở tài khoản cho phép login lại; không tự phục hồi phiên đã bị hủy.

### Kết quả kiểm tra P1-05

- Ant build/deploy Tomcat 10.1.57, Java17 và SQL Server thật: **53 kiểm tra
  HTTP/SQL đạt**. Admin xem được danh sách; anonymous/customer không truy cập được.
- Thiếu/sai CSRF không đổi DB; admin không tự khóa bằng form giả; validate ID,
  action, active và target không tồn tại đúng status/thông báo.
- Lock/unlock qua chính AdminUserServlet có hiệu lực trong DB và trên session
  customer đang mở; locked account không login được, unlock thì login lại được.
- Lặp lại cùng thao tác không đảo ngược trạng thái. Chỉ IsActive đổi, các field
  còn lại không đổi dù form gửi thêm dữ liệu giả. Action delete bị từ chối;
  account vẫn tồn tại. Source production không có câu DELETE tài khoản.
- Tiếng Việt/HTML được escape, phone NULL rõ ràng; HTML không có mật khẩu/hash.
  Menu đúng role, footer Assignment và ba thành viên giữ nguyên.
- DB mất kết nối bị chặn an toàn; khôi phục config thì danh sách hoạt động lại.
  Hết session/đã logout không còn đổi trạng thái được.
- Đã dọn đúng account thử của lượt này; seed vẫn 3 tài khoản active, không đổi
  tên/role, đơn vẫn 0. Không tạo/chỉnh schema hoặc database cũ.
- NetBeans 17 parser: style.css và Bootstrap 5.1.3 đều 0 lỗi. Chưa kiểm tra
  trực quan desktop/mobile hay Run qua UI NetBeans vì công cụ không khả dụng.
- Đã dừng Tomcat thử, `ant clean dist` cuối bước thành công; WAR 5.489.816 bytes,
  7 class + 3 JAR, không config local/DLL/SQL/test. Không thực hiện Git ghi dữ liệu.

## P1-06 — Layout chung và ghép trang

- Header/footer giữ phong cách đã chốt và thông tin nhóm. Thêm common/styles.jsp
  để nạp hai CSS local một lần, common/messages.jsp cho thông báo thống nhất.
  Sáu trang home/login/register/profile/admin-users/error dùng cùng khung page-body;
  không đổi Servlet, DAO hoặc cách gọi database trong bước này.
- Form vẫn giữ action/method/name/hidden/CSRF, thêm hint liên kết bằng aria-describedby
  cho phone. Focus bàn phím rõ cho link/nút/vùng bảng. Bảng dùng data-table để P2/P3
  có thể dùng lại; breakpoint 991px/767px và Bootstrap local được giữ.
- Thêm WEB-INF/tags/money.tag nhận BigDecimal, fmt locale vi_VN, tiền nguyên đồng,
  hậu tố ₫. Đang dùng thật ở 3 mức phí công tham khảo trên welcome; chưa thêm
  ServiceDAO/Servlet/catalog của P2 hoặc model/luồng đơn của P3.
- `docs/ui-guide.md` có cấu trúc JSP, class CSS, quy ước thông báo/CSRF và ví dụ
  money.tag cho BigDecimal của P2/P3; không sao chép requirement vào source.
- **105 kiểm tra render/HTTP đạt** trên /KeebForge và /KeebForge-preview:
  mỗi trang một shell/CSS, link đúng context, header/footer, active menu, label/hint,
  JSP/partial/tag được bảo vệ bằng WEB-INF, form đăng ký/login/logout/profile/status,
  thông báo success/error và escape dữ liệu nhập.
- Tag được kiểm tra số 0, 2.050.000, BigDecimal 123456789012345678, null và locale
  JSP cha en_US: kết quả đúng, không mất chữ số với BigDecimal, không đổi locale cha.
- Hai context được test trên hai Tomcat riêng (loopback 18080/18081) vì DLL Windows
  auth không nạp được vào hai webapp classloader riêng trong cùng JVM. Không thay
  loader/JAR/server dùng chung trên máy; chỉ tạo CATALINA_BASE thử trong build.
- Account test riêng được dọn, seed còn 3 active, không thay schema/dữ liệu cũ.
- NetBeans 17 CSS parser: style.css và Bootstrap 5.1.3 đều 0 lỗi. Năm form template
  vẫn có CSRF; production JSP/tag không có SQL/scriptlet nghiệp vụ.
- Công cụ browser/native pipe không khả dụng: chưa duyệt trực quan desktop/mobile.
  Kiểm tra render/HTTP/CSS không thay cho việc nhìn bố cục trên trình duyệt.
- Đã dừng cả hai Tomcat thử, build sạch thành công. WAR cuối 5.491.591 bytes,
  vẫn 7 class production, có money.tag/styles.jsp/messages.jsp; không test fixture,
  config thật, DLL hoặc SQL trong WAR. Không thực hiện Git ghi dữ liệu.

## Sửa lỗi đăng ký trên Tomcat NetBeans và dọn DB cũ

Log thật ở Tomcat cổng 8080 ghi SQLException: chưa cấu hình keebforge.db.config
hoặc KEEBFORGE_DB_CONFIG. DB chính đã có đủ 8 bảng; việc thiếu cấu hình server
là nguyên nhân của lỗi đăng ký, không phải cần tạo lại database.

Đã thêm vị trí config mặc định trong DBContext như hướng dẫn trên và cấu hình
Windows auth của đúng server NetBeans. Mở lại http://localhost:8080/KeebForge/register
để đăng ký. Đã chạy 18 kiểm tra HTTP/SQL trên cổng 8080, gồm đăng ký/lưu Unicode/hash,
trùng tài khoản, login/profile/update, CSRF/quyền, Admin list/lock/unlock/logout;
tất cả đạt. Không chỉ kiểm tra trên Tomcat tạm như các mốc đầu.

Theo yêu cầu dọn DB thừa của người dùng, đã giữ KeebForce_PRJ301 và xóa hai DB schema
cũ KeebForgeDB, KeebForge_P102_Verify sau COPY_ONLY/CHECKSUM và RESTORE VERIFYONLY.
Không tìm thấy source hiện tại trỏ tới hai DB cũ hoặc phiên SQL đang dùng chúng.
Backup tại thư mục SQL Server:

```text
C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\Backup
KeebForgeDB_before_cleanup_20261028_210938.bak
KeebForge_P102_Verify_before_cleanup_20261028_210938.bak
```

Không sửa script để tự DROP/reset DB khi chạy app. Giữ dữ liệu seed/DB chính,
không chạy lại script 01–03 trên DB đã có. Account thử đã dọn, seed vẫn 3 active.
