package model;

import java.math.BigDecimal;

/**
 * JavaBean tương ứng bảng WorkshopService: một gói công cố định của workshop.
 * Đây là dữ liệu dịch vụ, không phải tầng service trong kiến trúc.
 */
public class WorkshopService {

    private int serviceId;
    private String serviceName;
    private String description;
    private BigDecimal basePrice;
    private boolean active;

    public WorkshopService() {
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
