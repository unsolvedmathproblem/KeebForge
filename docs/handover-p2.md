# Bàn giao P2 — Linh kiện và dịch vụ — 29/10/2026

**P2 đã được người dùng xác nhận nghiệm thu và kết thúc ngày 29/10/2026.**
Nghiệm thu P2 không đồng nghĩa tự tiếp tục sang module khác hoặc cho phép thao tác Git ghi dữ liệu (`git add`/`commit`/`push`/PR).

## 1. Thành phần bàn giao của P2

| Thành phần | Đường dẫn | Vai trò |
| --- | --- | --- |
| `Brand` | `src/java/model/Brand.java` | JavaBean bảng `dbo.Brand` (`brandId`, `brandName`) |
| `Category` | `src/java/model/Category.java` | JavaBean bảng `dbo.Category` (`categoryId`, `categoryName`) |
| `Product` | `src/java/model/Product.java` | JavaBean bảng `dbo.Product` kèm `categoryName`, `brandName` từ JOIN |
| `WorkshopService` | `src/java/model/WorkshopService.java` | JavaBean bảng `dbo.WorkshopService` (`serviceId`, `serviceName`, `description`, `basePrice`, `active`) |
| `ServiceReview` | `src/java/model/ServiceReview.java` | JavaBean dùng chung giữa P2 (đọc) và P3 (ghi), kèm `fullName` từ JOIN `UserAccount` |
| `ProductDAO` | `src/java/dao/ProductDAO.java` | Truy vấn danh mục, lọc/tìm kiếm/phân trang public, danh sách active cho P3 và CRUD Admin |
| `ServiceDAO` | `src/java/dao/ServiceDAO.java` | Truy vấn dịch vụ active, chi tiết dịch vụ cho P3, đọc đánh giá đơn `COMPLETED` và CRUD Admin |
| `ProductServlet` | `src/java/controller/ProductServlet.java` | Xử lý `GET /home` và `GET /parts` (tìm kiếm, lọc `categoryId`/`brandId`, phân trang) |
| `ServiceServlet` | `src/java/controller/ServiceServlet.java` | Xử lý `GET /services` và `GET /services?id=` (hiển thị bảng giá dịch vụ và đánh giá) |
| `AdminProductServlet` | `src/java/controller/admin/AdminProductServlet.java` | Xử lý `/admin/products` (`GET action=list/new/edit`, `POST action=save/toggle`) |
| `AdminServiceServlet` | `src/java/controller/admin/AdminServiceServlet.java` | Xử lý `/admin/services` (`GET` danh sách + form inline `?id=`, `POST action=save/toggle`) |
| Public JSP | `web/WEB-INF/views/home.jsp`, `parts.jsp`, `services.jsp` | Giao diện trang chủ, catalog linh kiện và dịch vụ workshop đọc dữ liệu thật từ DB |
| Admin JSP | `web/WEB-INF/views/admin/products.jsp`, `product-form.jsp`, `services.jsp` | Giao diện quản trị linh kiện và quản trị dịch vụ (tách biệt với JSP public) |
| Ảnh dự phòng | `web/images/placeholder.png` | Ảnh mặc định local khi `Product.ImageURL` là `NULL` |

Tổng project hiện có **18 class Java production** (7 class P1 + 11 class P2, classfile major 61 — Java 17) và **3 JAR runtime** trong `web/WEB-INF/lib`. Không dùng Maven, tầng service, DTO, repository interface, enum hay JSON specs.

## 2. Hợp đồng Model / Getter / DAO bàn giao cho P3

### 2.1. Getter JavaBean đã chốt

- `model.WorkshopService`:
  - `getServiceId()` (`int`), `getServiceName()` (`String`), `getDescription()` (`String`), `getBasePrice()` (`BigDecimal`), `isActive()` (`boolean`).
- `model.Product`:
  - `getProductId()` (`int`), `getProductName()` (`String`), `getCategoryId()` (`int`), `getCategoryName()` (`String`), `getBrandId()` (`int`), `getBrandName()` (`String`), `getDescription()` (`String`), `getPrice()` (`BigDecimal`), `getImageUrl()` (`String`), `isActive()` (`boolean`), `getCreatedAt()` (`LocalDateTime`).
- `model.ServiceReview` *(dùng chung P2 & P3, không tạo model/DAO review thứ hai)*:
  - `getReviewId()` (`int`), `getOrderId()` (`int`), `getRating()` (`int`), `getComment()` (`String`), `getCreatedAt()` (`LocalDateTime`), `getFullName()` (`String`).

### 2.2. Phương thức DAO P3 gọi trực tiếp hoặc phối hợp

- `ServiceDAO` (mở cục bộ trong `try-with-resources`):
  - `getActiveServices()`: trả về `List<WorkshopService>` đang hoạt động (`IsActive = 1`) để hiển thị dropdown chọn dịch vụ trên `/request`.
  - `getById(int serviceId)`: trả về `WorkshopService` (kiểm tra `service != null && service.isActive()` trước khi nhận đơn mới).
  - `getReviews(int serviceId)`: P2 dùng để hiển thị đánh giá của các đơn `COMPLETED` trên `/services?id=<serviceId>#reviews`; P3 ghi đánh giá vào `dbo.ServiceReview` qua `OrderDAO.insertReview`.
- `ProductDAO` (mở cục bộ trong `try-with-resources`):
  - `getActiveProducts()`: trả về `List<Product>` đang hoạt động (`IsActive = 1`) để khách chọn kèm linh kiện (0..n món, số lượng 1..200) trên `/request`.
  - `getProductById(int productId)`: đọc thông tin một linh kiện theo `ProductID`.
- Liên kết từ trang `/home` và `/services` sang P3:
  - Mọi nút gửi yêu cầu trỏ thống nhất tới `<contextPath>/request?serviceId=<serviceId>`.

## 3. Artifact và kiểm chứng cuối

- `dist/KeebForce.war`: **5.625.336 bytes**
- SHA-256 WAR: `1ADD9566E9A8FD5AFA6EE3F5389749B4A387B14ADEE20F7CC47CFC893524955A`

| Hạng mục kiểm tra | Kết quả thực tế |
| --- | --- |
| Build Ant (`clean dist`) | `BUILD SUCCESSFUL` với JDK 17.0.12, biên dịch cả Java và `compile-jsps` |
| Rà soát tĩnh kiến trúc | 0 `javax.servlet`, 0 SQL/scriptlet trong JSP/tag, 0 field DAO trong Servlet, 0 `System.out/err` |
| WAR đóng gói | 18 class (major 61), 3 JAR, đủ JSP & `placeholder.png`; 0 file `.properties`/`.sql`/`.dll` |
| P2-01 → P2-07 tổng hợp | **166 lượt kiểm tra tự động DAO/HTTP/SQL** đạt trên Tomcat 10.1.59 & SQL Server 2025 |
| Phân quyền & CSRF | Anonymous → 302 `/login`; `CUSTOMER` vào `/admin/products`, `/admin/services` → 403; thiếu `csrf` → 403 |
| Bảo toàn lịch sử đơn | Sửa `Product.Price` / `WorkshopService.BasePrice` hoặc ẩn `IsActive = 0` không làm đổi hay mất snapshot `OrderDetail.UnitPrice` / `ServiceOrder.ServicePrice` |
| DB mất kết nối | `/home`, `/parts`, `/services`, `/admin/products` trả HTTP 503 tiếng Việt, không trả list giả, không lộ stack trace/credential, tự phục hồi HTTP 200 khi DB có lại |
| Trạng thái DB sau kiểm tra | Giữ nguyên dữ liệu seed sạch: 3 account, 3 brand, 5 category, 10 product, 4 service, 0 order/detail/review; tổng kịch bản demo = `2.050.000 ₫` |

## 4. Giới hạn và hướng dẫn chụp ảnh báo cáo

- **Chưa chụp ảnh màn hình trình duyệt tự động**: môi trường CLI không gắn công cụ điều khiển trình duyệt đồ họa, nên chưa chụp screenshot trực quan desktop/mobile. Thành viên mở dự án trên trình duyệt (`http://localhost:8080/KeebForce`) để chụp ảnh báo cáo cho các màn hình P2:
  1. Trang chủ `/home` (danh sách dịch vụ & linh kiện tiêu biểu từ DB).
  2. Catalog linh kiện `/parts` (tìm kiếm, lọc danh mục/thương hiệu, phân trang).
  3. Trang dịch vụ `/services` và khu vực đánh giá `#reviews`.
  4. Quản trị linh kiện `/admin/products` và biểu mẫu `/admin/products?action=new` / `edit` (đăng nhập `admin` / `Admin@123`).
  5. Quản trị dịch vụ `/admin/services` và biểu mẫu sửa inline `/admin/services?id=1`.
- **Phụ thuộc P3 chưa ghép**: các route `/request`, `/orders`, `/admin/orders`, `/admin/dashboard` và `OrderDAO.insertReview` thuộc phạm vi P3, chưa có trong nhánh source hiện tại. Không chèn bình luận giả vào DB để thay cho chức năng của P3.
