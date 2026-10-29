package controller;

import dao.ProductDAO;
import dao.ServiceDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import model.Brand;
import model.Category;
import model.Product;
import model.WorkshopService;

@WebServlet(name = "ProductServlet", urlPatterns = {"/home", "/parts"})
public class ProductServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 6;
    private static final int FEATURED_LIMIT = 6;
    private static final int MAX_KEYWORD_LENGTH = 120;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if ("/home".equals(request.getServletPath())) {
            showHome(request, response);
        } else {
            showParts(request, response);
        }
    }

    private void showHome(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (ServiceDAO serviceDao = new ServiceDAO();
                ProductDAO productDao = new ProductDAO()) {
            List<WorkshopService> services = serviceDao.getActiveServices();
            List<Product> products = productDao.getFeaturedProducts(FEATURED_LIMIT);
            request.setAttribute("services", services);
            request.setAttribute("products", products);
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải dữ liệu trang chủ tại ProductServlet.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể tải dữ liệu trang chủ lúc này. Vui lòng thử lại sau.");
        }
        request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
    }

    private void showParts(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = trimmed(request, "q");
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        int requestedPage = parsePage(request.getParameter("page"));

        request.setAttribute("q", keyword);
        request.setAttribute("pageSize", PAGE_SIZE);

        try (ProductDAO dao = new ProductDAO()) {
            List<Category> categories = dao.getAllCategories();
            List<Brand> brands = dao.getAllBrands();

            Integer categoryId = parseCategoryId(request.getParameter("categoryId"), categories);
            Integer brandId = parseBrandId(request.getParameter("brandId"), brands);

            int totalProducts = dao.countProducts(keyword, categoryId, brandId);
            int totalPages = totalProducts == 0 ? 1 : (totalProducts + PAGE_SIZE - 1) / PAGE_SIZE;
            int currentPage = Math.min(requestedPage, totalPages);

            List<Product> products = totalProducts == 0
                    ? Collections.emptyList()
                    : dao.getProducts(keyword, categoryId, brandId, currentPage, PAGE_SIZE);

            request.setAttribute("categories", categories);
            request.setAttribute("brands", brands);
            request.setAttribute("categoryId", categoryId);
            request.setAttribute("brandId", brandId);
            request.setAttribute("products", products);
            request.setAttribute("totalProducts", totalProducts);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("currentPage", currentPage);
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách linh kiện tại ProductServlet.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể tải danh sách linh kiện lúc này. Vui lòng thử lại sau.");
        }

        request.getRequestDispatcher("/WEB-INF/views/parts.jsp").forward(request, response);
    }

    private int parsePage(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            int page = Integer.parseInt(raw.trim());
            return Math.max(1, page);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private Integer parseCategoryId(String raw, List<Category> categories) {
        Integer id = parsePositiveInt(raw);
        if (id == null) {
            return null;
        }
        for (Category category : categories) {
            if (category.getCategoryId() == id) {
                return id;
            }
        }
        return null;
    }

    private Integer parseBrandId(String raw, List<Brand> brands) {
        Integer id = parsePositiveInt(raw);
        if (id == null) {
            return null;
        }
        for (Brand brand : brands) {
            if (brand.getBrandId() == id) {
                return id;
            }
        }
        return null;
    }

    private Integer parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimmed(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
