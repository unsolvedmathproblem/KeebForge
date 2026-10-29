# Dùng chung giao diện KeebForge

Giữ nền sáng, màu xanh lá/cam, font hệ thống và Bootstrap 5.1.3 local.
Các file dưới đây đang được dùng thật ở home, auth, profile và Admin users.
Không có layout framework hoặc class Java giao diện riêng.

## Khung một trang JSP

Trang nội dung đặt trong `web/WEB-INF/views`, Servlet forward bằng đường dẫn
tuyệt đối `/WEB-INF/views/...`. Mẫu cấu trúc:

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="page" value="home" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tên trang — KeebForge</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <h1>Tên trang</h1>
    <jsp:include page="/WEB-INF/views/common/messages.jsp" />
    <!-- Nội dung trang -->
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
```

Đổi `page` và title cho đúng trang. Giá trị hiện có: home/login/register/profile/users/error.
Chỉ một header/footer, một main-content, một cặp link CSS cho mỗi trang.
Styles/messages là JSP partial ngắn, không có html/head/body riêng.
Header chỉ thêm menu khi route thực sự đã có, không đặt link đến chức năng chưa làm.
Footer giữ Assignment và Nguyễn Văn Lê Hào, Nguyễn Anh Tiền, Vũ Hoàng Nguyên.

## Thông báo, form, nút và bảng

| Cần dùng | File/class/quy ước |
| --- | --- |
| CSS chung | common/styles.jsp, nạp Bootstrap local rồi style.css |
| Thông báo thành công | Servlet đặt request attribute `message` |
| Thông báo lỗi | Servlet đặt `errorMessage`; messages.jsp escape và hiển thị role=alert |
| Form có label/hint | .form-field, .form-control, small và aria-describedby |
| Form hai cột | .form-grid; tự về một cột trên màn hình hẹp |
| Nút chính | .btn.btn-workshop |
| Bảng | .table-responsive.data-table-wrap bọc .table.data-table |
| Trạng thái | .status-badge kèm class màu và chữ rõ nghĩa; xem users.jsp |

Đặt messages.jsp một lần ở nội dung chính. Partial đọc đúng request scope,
không lấy thông báo từ query hoặc tự chạy SQL. Các giá trị từ form/DB dùng c:out,
đặc biệt trong value input và cell bảng. Không đổ password/hash vào giao diện.

Mọi form POST dùng chung token do AuthFilter tạo:

```jsp
<input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
```

Khi đổi bố cục, giữ nguyên action, method, name, hidden action/id/active và csrf.
Dùng c:url cho action/href, không viết cứng `/KeebForge/...` trong JSP.
Không tạo token CSRF thứ hai. Không đổi mutation sang GET.

## Tiền VND bằng custom tag

```jsp
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<span class="money"><kf:money value="${product.price}" /></span>
```

Ví dụ trên dùng property BigDecimal của model Product khi P2 triển khai.
P3 truyền tương tự `${order.totalAmount}`. Lượt này không tạo các model đó.

`WEB-INF/tags/money.tag` nhận attribute `value` kiểu java.math.BigDecimal,
dùng fmt locale vi_VN trong phạm vi tag, không đổi locale của JSP cha.
Định dạng VND nguyên đồng: `2050000` → `2.050.000 ₫`; `0` → `0 ₫`; null → `—`.
BigDecimal 18 chữ số được giữ nguyên khi format, không ép tiền qua Double.
Truyền BigDecimal từ model bằng EL; không truyền chuỗi tĩnh vào attribute BigDecimal
hoặc dùng literal số EL lớn thay cho BigDecimal của DAO/model.

Welcome đang dùng các giá mẫu nhỏ `${200000}`, `${150000}`, `${100000}` để minh họa
tag ngay trên trang chạy. Đây là dữ liệu layout tĩnh lấy theo seed, không phải
ServiceServlet/catalog đọc DB. P2 sẽ thay bằng BasePrice từ DB khi ghép home/catalog.

## Kiểm tra khi ghép trang

- Header/footer đúng một lần, các id label/hint/main-content tồn tại.
- Link/form/resource dùng context path thực, kể cả khi đổi tên WAR.
- POST vẫn có csrf, lỗi giữ thông tin an toàn và không lộ stack trace/hash.
- Bảng cuộn ngang, menu/form có CSS ở breakpoint 991px/767px; focus bàn phím rõ.
- Thử trực quan desktop/mobile trên trình duyệt khi có môi trường; kiểm tra HTTP
  và CSS parser không thay thế việc duyệt bố cục bằng mắt.
