# Bàn giao P1 — 28/10/2026

**P1 đã được người dùng xác nhận nghiệm thu và kết thúc ngày 28/10/2026.**
Người dùng đã xác nhận đăng ký tài khoản thành công sau bản sửa kết nối.
Nghiệm thu P1 không đồng nghĩa tự tiếp tục P2/P3 hoặc cho phép thao tác Git ghi dữ liệu.

## Thành phần bàn giao

| Thành phần | Vai trò |
| --- | --- |
| DBContext | Config local, connection riêng cho mỗi DAO, AutoCloseable |
| Account | JavaBean 9 property, session key user |
| PasswordUtil | PBKDF2-HMAC-SHA256 + salt, verify bằng constant-time comparison |
| AccountDAO | Đọc/register/update profile/list/update status bằng PreparedStatement |
| AuthServlet | Form/session/validation, profile owner từ session, logout POST |
| AuthFilter | UTF-8, CSRF, role và kiểm tra account đang active trong DB |
| AdminUserServlet | List/lock/unlock, không tự khóa Admin, không xóa account |
| JSP/CSS/tag | Welcome/auth/profile/admin users, partial chung, money.tag VND |
| database/ | 3 script tạo DB/schema/seed an toàn và 1 script verify chỉ đọc |

Tổng 7 class Java production, 3 JAR runtime; giữ Ant Web/NetBeans17/Java17/
Tomcat10.1/SQL Server. Không có class DTO/service/interface/stub hoặc package cũ
cần xóa. P1 không thêm Model/DAO của P2/P3 để lấp đầy cây package.

## Artifact

- `dist/KeebForge.war`: 5.491.786 bytes (đã cập nhật bản sửa cấu hình DB mặc định).
- SHA-256 WAR: `BD133F837E7935F239C0CA3B0EB872146AA76FF1699D73C9FC445B13AB310346`.
- `dist/KeebForge-P1-source.zip`: source/JAR/SQL/docs/metadata chia sẻ,
  không config thật, DLL, Git internals, output build hoặc requirement bên ngoài.

WAR không chứa SQL/docs/config DB thật/native DLL/test fixture hoặc Servlet/JSP API
của Tomcat. JDBC/JSTL dùng đường dẫn tương đối trong web/WEB-INF/lib; version ở setup.
WAR đọc DB local từ TOMCAT_BASE/conf/keebforge-db.properties khi không có override;
không tự tạo database hay SQL login.

`.gitignore` trước đây bỏ qua toàn bộ nbproject và build-impl.xml.
P1-07 đã bỏ hai rule đó để metadata dùng chung có thể được chia sẻ, vẫn ignore
nbproject/private và config local. Chưa stage/commit/push bất kỳ file nào.

## Kiểm chứng cuối trên máy hiện tại

| Nhóm | Kết quả thực tế |
| --- | --- |
| Build | Ant clean dist thành công với JDK17 |
| Kiểu Java | 7 class biên dịch, classfile major 61 (Java17) |
| Source không private | Bản sao không nbproject/private/config DB build được khi truyền server home và CopyLibs của NetBeans |
| Đối chiếu WAR | Nội dung 27 file trong WAR của source chính và bản sao khớp từng SHA-256, bỏ qua timestamp ZIP |
| Deploy | Chạy chính WAR build từ bản sao source trên Tomcat10.1.57 |
| Hồi quy | 75 kiểm tra HTTP/SQL đạt trên source đã ghép |
| DAO/hash | 16 kiểm tra seed hash/salt/Unicode/malformed/NULL hash trong list đạt |
| Timeout thật | 5 kiểm tra đạt trên bản deploy riêng có timeout 1 phút |
| CSS | Parser NetBeans17: style.css/Bootstrap5.1.3 đều 0 lỗi |
| Cú pháp JSP | Tomcat compile/render các trang và custom tag qua request thật |
| Rà source | Không SQL/scriptlet nghiệp vụ trong JSP, javax.servlet, debug print hoặc reference package cũ |
| Cấu hình nhạy cảm | Config thật/DLL/private bị ignore và không nằm trong WAR/source zip |
| Diff | git diff --check không lỗi whitespace; thông báo LF→CRLF theo cấu hình Git |

Không đo phần trăm code coverage hoặc thêm JUnit/coverage framework.
Kiểm tra kiểu dùng javac của Java17; kiểm tra cú pháp CSS dùng parser NetBeans17,
không coi build Ant đơn thuần là đủ xác nhận JSP/CSS hợp lệ.

### Hồi quy auth/profile/admin

Đã kiểm tra login đúng/sai/user không tồn tại và username kiểu SQL injection;
register validate/username-email trùng, hash, ép CUSTOMER/active và PRG;
session ID/token đổi khi login; token thiếu/sai/khác phiên/cũ bị chặn;
profile ID từ session, không đổi role/email/password qua form, Unicode/phone NULL,
escape dữ liệu HTML; admin/customer/anonymous đúng quyền; tự khóa Admin bị chặn;
GET không lock/logout, POST có CSRF; lock/unlock ảnh hưởng session đang dùng,
role thay đổi được kiểm tra lại; DB mất kết nối trả 503 an toàn rồi phục hồi.

Chỉ thay đổi/dọn các account thử có username/email riêng của lượt kiểm tra.
Kết thúc DB giữ nguyên 3 seed account active, 3 brand, 5 category, 10 product,
4 service và 0 order/detail/review. Không DROP/chỉnh database cũ.

### Timeout tự nhiên

WAR chính và source giữ session-timeout=30 phút. Một bản copy deploy riêng trên
loopback 18081 dùng 1 phút để kiểm chứng nhanh, không sửa descriptor source.
Account thử vẫn tồn tại và active trong DB. Sau 150 giây không truy cập session:
profile bị redirect login, POST không cập nhật được, token cũ trả 403;
account vẫn login lại được với phiên mới. Đây là kiểm tra timeout tự nhiên
ở cấu hình 1 phút, không phải xác nhận đã đợi hết 30 phút trên bản chính.

## Phần chưa kiểm tra được

- Chưa có máy thành viên khác: bản sao source sạch vẫn chạy trên cùng máy,
  không thay thế việc chạy trên Windows/SQL Server của thành viên khác.
- Công cụ computer-use không có browser/native pipe: chưa duyệt trực quan
  desktop/mobile, kiểm tra pixel/overflow thực tế hoặc tạo screenshot nghiệm thu.
- Chưa thao tác Run/Clean and Build qua UI NetBeans hoặc Execute qua UI SSMS;
  đã chạy Ant của NetBeans, Tomcat và SQL Server thật qua CLI/HTTP.
- Kết nối live được kiểm tra bằng Windows Authentication. Chưa có credential
  SQL login thật để xác minh nhánh SQL authentication trên máy thành viên.
- P2/P3 chưa triển khai: không báo catalog/đơn/dashboard/review đã hoàn thành.
  Các route tương lai có Filter nhưng có thể 404 sau khi được phép đi qua.

## Thành viên nhận source kiểm tra nhanh

1. Mở project từ source zip, chọn JDK17/Tomcat10.1 trong NetBeans17.
2. SQL Server mới: chạy database/01→02→03; DB đã có thì chỉ chạy 04_verify.
3. Đặt config vào conf của Tomcat, cài DLL/setenv nếu dùng Windows auth theo setup,
   restart Tomcat rồi Run. DBContext tự tìm config, không bắt buộc VM Option DB path.
4. Dùng customer1: login → đổi họ tên/phone → logout; thử sai password và CSRF.
5. Dùng admin: mở Tài khoản, thử khóa/mở một account demo do mình tạo,
   xác nhận không tự khóa được và không có xóa vật lý.
6. Duyệt desktop/mobile, kiểm tra menu/bảng/form/footer; chụp home/login/profile/users
   cho báo cáo nhóm khi có trình duyệt. Không dùng ảnh giả thay cho kiểm chứng.

Thông tin chạy, tài khoản demo/JAR ở [setup.md](setup.md), quy ước ghép trang P2/P3
ở [ui-guide.md](ui-guide.md). Progress P1 nằm ngoài repository trong folder requirement.

## Bổ sung sửa lỗi thực tế trên NetBeans

Đã xác định log ở server 8080 thiếu đường dẫn config DB. DBContext được bổ sung
đọc mặc định file trong conf; giữ Properties/JDBC, AutoCloseable và lỗi SQLException,
không copy nguyên DBContext Q3 để lại connection null hoặc thiếu close().
Đã cấu hình đúng server NetBeans, restart và chạy 18 kiểm tra HTTP/SQL đạt ngay
trên cổng 8080, không truyền keebforge.db.config từ lệnh chạy thử.

Theo yêu cầu người dùng, hai DB cũ KeebForgeDB/KeebForge_P102_Verify được backup
COPY_ONLY/CHECKSUM, VERIFYONLY rồi xóa; KeebForce_PRJ301 được giữ nguyên.
Đường dẫn backup và cách cấu hình mới trong setup.md. Không đưa DROP vào script app,
không thực hiện Git ghi dữ liệu. Các kết quả P1-07 ở trên là lịch sử trước lần sửa;
artifact hiện tại đã được cập nhật code/cấu hình mẫu/tài liệu này.

## Xác nhận kết thúc P1

Người dùng đã thử đăng ký thành công, yêu cầu dừng server 8080 để tự Run từ NetBeans,
và xác nhận “ok bàn giao kết thúc P1 đi”. P1 được chốt theo xác nhận đó.
Các giới hạn kiểm chứng (máy thứ hai, quan sát UI/screenshot và SQL login thật)
vẫn giữ nguyên như phần báo cáo, không được coi là đã chạy thêm.
Lần chốt này chỉ cập nhật tài liệu/source ZIP, không sửa nghiệp vụ, DB hoặc server.

**Trạng thái: đã nghiệm thu và kết thúc P1. Dừng tại đây, chưa triển khai P2/P3.**
