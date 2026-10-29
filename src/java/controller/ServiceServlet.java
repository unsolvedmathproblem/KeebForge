package controller;

import dao.ServiceDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import model.ServiceReview;
import model.WorkshopService;

@WebServlet(name = "ServiceServlet", urlPatterns = {"/services"})
public class ServiceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String rawId = request.getParameter("id");

        try (ServiceDAO dao = new ServiceDAO()) {
            List<WorkshopService> services = dao.getActiveServices();
            request.setAttribute("services", services);

            if (rawId != null && !rawId.isBlank()) {
                WorkshopService selectedService = findActiveService(rawId, services);
                if (selectedService != null) {
                    List<ServiceReview> reviews = dao.getReviews(selectedService.getServiceId());
                    request.setAttribute("selectedService", selectedService);
                    request.setAttribute("reviews", reviews);
                } else {
                    request.setAttribute("errorMessage", "Dịch vụ bạn chọn không tồn tại hoặc hiện đã ngừng nhận yêu cầu.");
                }
            }
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách dịch vụ tại ServiceServlet.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể tải danh sách dịch vụ lúc này. Vui lòng thử lại sau.");
        }

        request.getRequestDispatcher("/WEB-INF/views/services.jsp").forward(request, response);
    }

    private WorkshopService findActiveService(String rawId, List<WorkshopService> services) {
        try {
            int id = Integer.parseInt(rawId.trim());
            if (id <= 0) {
                return null;
            }
            for (WorkshopService service : services) {
                if (service.getServiceId() == id) {
                    return service;
                }
            }
            return null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
