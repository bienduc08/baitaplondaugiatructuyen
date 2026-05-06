import java.time.LocalDateTime;

public class ProductDTO implements java.io.Serializable {
    private int id;
    private String name;
    private double currentPrice;
    private String sellerName; // Người đăng bán
    private String ownerName;  // Người trả giá cao nhất hiện tại
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;     // PENDING, APPROVED, OPEN, CLOSED

}