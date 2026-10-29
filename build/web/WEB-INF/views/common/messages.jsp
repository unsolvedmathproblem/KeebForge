<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty requestScope.message}">
    <div class="notice notice-success" role="status"><c:out value="${requestScope.message}" /></div>
</c:if>
<c:if test="${not empty requestScope.errorMessage}">
    <div class="notice notice-error" role="alert" id="feedback-error"><c:out value="${requestScope.errorMessage}" /></div>
</c:if>
