<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="value" required="true" rtexprvalue="true" type="java.math.BigDecimal" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="page" />
<c:choose>
    <c:when test="${empty value}">—</c:when>
    <c:otherwise><fmt:formatNumber value="${value}" pattern="#,##0" /> ₫</c:otherwise>
</c:choose>
