package controller.admin;

import dao.ServiceDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Locale;
import model.Account;
import model.WorkshopService;

/** Quản trị các gói dịch vụ workshop: danh sách và biểu mẫu thêm/sửa trên cùng trang. */
@WebServlet(name = "AdminServiceServlet", urlPatterns = {"/admin/services"})
public class AdminServiceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String saved = request.getParameter("saved");
        if ("created".equals(saved)) {
            request.setAttribute("message", "Đã thêm gói dịch vụ mới.");
        } else if ("updated".equals(saved)) {
            request.setAttribute("message", "Đã cập nhật thông tin gói dịch vụ.");
        } else if ("1".equals(request.getParameter("toggled"))) {
            request.setAttribute("message", "Đã cập nhật trạng thái nhận yêu cầu của dịch vụ.");
        }

        String action = trimToEmpty(request.getParameter("action"));
        if (!action.isEmpty() && !"list".equals(action) && !"new".equals(action) && !"edit".equals(action)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Thao tác quản trị dịch vụ không hợp lệ.");
            prepareNewDraft(request);
            renderPage(request, response);
            return;
        }

        String idRaw = trimToEmpty(request.getParameter("id"));
        if ("edit".equals(action) || !idRaw.isEmpty()) {
            Integer serviceId = parsePositiveInt(idRaw);
            if (serviceId == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("errorMessage", "Mã dịch vụ cần chỉnh sửa không hợp lệ.");
                prepareNewDraft(request);
                renderPage(request, response);
                return;
            }
            try (ServiceDAO dao = new ServiceDAO()) {
                WorkshopService target = dao.getById(serviceId);
                if (target == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    request.setAttribute("errorMessage", "Không tìm thấy gói dịch vụ cần chỉnh sửa.");
                    prepareNewDraft(request);
                } else {
                    request.setAttribute("formMode", "edit");
                    request.setAttribute("formService", target);
                    request.setAttribute("basePriceInput",
                            target.getBasePrice() != null ? target.getBasePrice().toPlainString() : "");
                    request.setAttribute("serviceIdValue", String.valueOf(target.getServiceId()));
                }
                request.setAttribute("services", dao.getAllForAdmin());
            } catch (SQLException ex) {
                getServletContext().log("Không thể tải dữ liệu quản trị dịch vụ.", ex);
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                request.setAttribute("errorMessage", "Chưa thể tải danh sách dịch vụ lúc này. Vui lòng thử lại sau.");
                prepareNewDraft(request);
            }
            request.setAttribute("page", "admin-services");
            request.getRequestDispatcher("/WEB-INF/views/admin/services.jsp").forward(request, response);
            return;
        }

        prepareNewDraft(request);
        renderPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = trimToEmpty(request.getParameter("action"));
        if ("save".equals(action)) {
            handleSave(request, response);
            return;
        }
        if ("toggle".equals(action)) {
            handleToggle(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("errorMessage", "Thao tác biểu mẫu dịch vụ không hợp lệ.");
        prepareNewDraft(request);
        renderPage(request, response);
    }

    private void handleSave(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String formMode = trimToEmpty(request.getParameter("formMode"));
        String idRaw = request.getParameter("id");
        if (idRaw == null) {
            idRaw = request.getParameter("serviceId");
        }
        idRaw = trimToEmpty(idRaw);

        boolean isEdit = "edit".equals(formMode) || !idRaw.isEmpty();
        Integer parsedId = isEdit ? parsePositiveInt(idRaw) : null;

        String serviceName = request.getParameter("serviceName");
        if (serviceName == null) {
            serviceName = request.getParameter("name");
        }
        serviceName = trimToEmpty(serviceName);

        String basePriceRaw = request.getParameter("basePrice");
        if (basePriceRaw == null) {
            basePriceRaw = request.getParameter("price");
        }
        basePriceRaw = trimToEmpty(basePriceRaw);

        String description = trimToEmpty(request.getParameter("description"));
        Boolean parsedActive = parseActive(request.getParameter("active"));

        WorkshopService draft = new WorkshopService();
        if (parsedId != null) {
            draft.setServiceId(parsedId);
        }
        draft.setServiceName(serviceName);
        draft.setDescription(description);
        draft.setActive(parsedActive != null ? parsedActive : true);

        String error = validateFields(isEdit, parsedId, serviceName, basePriceRaw, description, parsedActive, draft);

        request.setAttribute("formMode", isEdit ? "edit" : "new");
        request.setAttribute("formService", draft);
        request.setAttribute("basePriceInput", basePriceRaw);
        request.setAttribute("serviceIdValue", idRaw);

        if (error != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", error);
            renderPage(request, response);
            return;
        }

        try (ServiceDAO dao = new ServiceDAO()) {
            if (isEdit) {
                if (!dao.updateService(draft)) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    request.setAttribute("errorMessage", "Không tìm thấy gói dịch vụ cần cập nhật.");
                    request.setAttribute("services", dao.getAllForAdmin());
                    request.setAttribute("page", "admin-services");
                    request.getRequestDispatcher("/WEB-INF/views/admin/services.jsp").forward(request, response);
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/admin/services?saved=updated");
                return;
            }

            dao.addService(draft);
            response.sendRedirect(request.getContextPath() + "/admin/services?saved=created");
        } catch (SQLException ex) {
            getServletContext().log("Không thể lưu thông tin gói dịch vụ.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể lưu gói dịch vụ lúc này. Vui lòng thử lại sau.");
            renderPage(request, response);
        }
    }

    private void handleToggle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idRaw = request.getParameter("id");
        if (idRaw == null) {
            idRaw = request.getParameter("serviceId");
        }
        Integer serviceId = parsePositiveInt(idRaw);
        String activeParam = request.getParameter("active");

        if (serviceId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Mã dịch vụ cần đổi trạng thái không hợp lệ.");
            prepareNewDraft(request);
            renderPage(request, response);
            return;
        }
        if (activeParam != null && !activeParam.trim().isEmpty()
                && !("true".equals(activeParam.trim()) || "false".equals(activeParam.trim()))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Trạng thái đích của dịch vụ không hợp lệ.");
            prepareNewDraft(request);
            renderPage(request, response);
            return;
        }

        try (ServiceDAO dao = new ServiceDAO()) {
            boolean updated;
            if (activeParam != null && !activeParam.trim().isEmpty()) {
                updated = dao.toggleActive(serviceId, "true".equals(activeParam.trim()));
            } else {
                updated = dao.toggleActive(serviceId);
            }
            if (!updated) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("errorMessage", "Không tìm thấy gói dịch vụ cần đổi trạng thái.");
                prepareNewDraft(request);
                request.setAttribute("services", dao.getAllForAdmin());
                request.setAttribute("page", "admin-services");
                request.getRequestDispatcher("/WEB-INF/views/admin/services.jsp").forward(request, response);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin/services?toggled=1");
        } catch (SQLException ex) {
            getServletContext().log("Không thể đổi trạng thái gói dịch vụ.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể cập nhật trạng thái dịch vụ. Vui lòng thử lại sau.");
            prepareNewDraft(request);
            renderPage(request, response);
        }
    }

    private void prepareNewDraft(HttpServletRequest request) {
        WorkshopService draft = new WorkshopService();
        draft.setActive(true);
        request.setAttribute("formMode", "new");
        request.setAttribute("formService", draft);
        request.setAttribute("basePriceInput", "");
        request.setAttribute("serviceIdValue", "");
    }

    private void renderPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("page", "admin-services");
        try (ServiceDAO dao = new ServiceDAO()) {
            request.setAttribute("services", dao.getAllForAdmin());
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách quản trị dịch vụ.", ex);
            if (response.getStatus() == HttpServletResponse.SC_OK) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            if (request.getAttribute("errorMessage") == null) {
                request.setAttribute("errorMessage", "Chưa thể tải danh sách dịch vụ lúc này. Vui lòng thử lại sau.");
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/services.jsp").forward(request, response);
    }

    private String validateFields(boolean isEdit, Integer parsedId, String serviceName,
            String basePriceRaw, String description, Boolean parsedActive, WorkshopService draft) {
        if (isEdit && parsedId == null) {
            return "Mã dịch vụ cần cập nhật không hợp lệ.";
        }
        if (serviceName.isEmpty()) {
            return "Vui lòng nhập tên gói dịch vụ.";
        }
        if (serviceName.length() > 120) {
            return "Tên gói dịch vụ không được vượt quá 120 ký tự.";
        }
        if (containsHtmlOrScript(serviceName)) {
            return "Tên gói dịch vụ không được chứa thẻ HTML hoặc mã script.";
        }
        if (basePriceRaw.isEmpty()) {
            return "Vui lòng nhập tiền công cố định.";
        }
        BigDecimal basePrice;
        try {
            basePrice = new BigDecimal(basePriceRaw).setScale(0, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException ex) {
            return "Tiền công cố định không đúng định dạng số.";
        } catch (ArithmeticException ex) {
            return "Tiền công cố định phải là số nguyên đồng (không có phần thập phân).";
        }
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            return "Tiền công cố định không được là số âm.";
        }
        if (basePrice.precision() > 18) {
            return "Tiền công cố định vượt quá giới hạn cho phép (tối đa 18 chữ số).";
        }
        draft.setBasePrice(basePrice);

        if (description.isEmpty()) {
            return "Vui lòng nhập mô tả phạm vi công việc của dịch vụ.";
        }
        if (description.length() > 1000) {
            return "Mô tả dịch vụ không được vượt quá 1000 ký tự.";
        }
        if (containsHtmlOrScript(description)) {
            return "Mô tả dịch vụ không được chứa thẻ HTML hoặc mã script.";
        }
        if (parsedActive == null) {
            return "Trạng thái nhận yêu cầu không hợp lệ.";
        }
        return null;
    }

    private boolean containsHtmlOrScript(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.indexOf('<') >= 0 || value.indexOf('>') >= 0) {
            return true;
        }
        return value.toLowerCase(Locale.ROOT).contains("javascript:");
    }

    private Boolean parseActive(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Boolean.FALSE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "on".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized) || "0".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Integer parsePositiveInt(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimToEmpty(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private Account currentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof Account) {
            Account user = (Account) session.getAttribute("user");
            if ("ADMIN".equals(user.getRole())) {
                return user;
            }
        }
        return null;
    }
}
