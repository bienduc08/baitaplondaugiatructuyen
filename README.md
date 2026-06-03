cat > README.md << 'EOF'
# Hệ thống Đấu giá Trực tuyến - BTL UET

Hệ thống đấu giá trực tuyến theo mô hình Client-Server sử dụng Java Socket, cho phép người dùng đăng ký, đăng sản phẩm đấu giá, đặt giá realtime và quản trị hệ thống.

## Thành viên nhóm
1. Biện Minh Đức
2. Nguyễn Đình Đức
3. Đinh Văn Toàn
4. Trần Đức Tuấn

## Công nghệ sử dụng
- **Ngôn ngữ:** Java 21
- **Giao diện Client:** JavaFX 21 (FXML)
- **Giao tiếp mạng:** Java Socket (TCP)
- **Cơ sở dữ liệu:** MySQL 8.x
- **Build tool:** Apache Maven 3.8+
- **Serialization:** Gson 2.10.1

## Yêu cầu cài đặt
- JDK 21+
- Apache Maven 3.8+
- MySQL Server 8.0+

## Cấu trúc thư mục
src/main/java/com/uet/auction/
├── client/             # Client (JavaFX)
│   ├── controller/     # Xử lý giao diện (Login, Bid, Admin...)
│   ├── network/        # Socket client, nghe phản hồi
│   └── util/           # AlertHelper, SceneManager, SessionManager
├── server/             # Server
│   ├── config/         # Kết nối Database
│   ├── DAO/            # Truy vấn DB (User, Product, Bid)
│   ├── model/          # Entity (User, Product, Auction, Bid...)
│   ├── network/        # SocketServer, ClientHandler
│   └── service/        # Logic nghiệp vụ (AuctionService, AuthService...)
└── common/             # Dùng chung Client & Server
    ├── DTO/            # Data Transfer Object
    ├── Request/        # Các loại request gửi lên server
    └── Response/       # Các loại response trả về client


## Cài đặt và chạy

### 1. Tạo Database
```bash
mysql -u root -p < schema.sql
```

### 2. Cấu hình Database
Chỉnh thông tin kết nối tại:
`src/main/java/com/uet/auction/server/config/DatabaseConnection.java`

### 3. Build project
```bash
mvn clean package -DskipTests
```

### 4. Chạy Server (chạy trước)
```bash
# Windows / Linux / MacOS
mvn exec:java -Dexec.mainClass="com.uet.auction.server.ServerApplication"
```

### 5. Chạy Client (chạy sau khi server đã khởi động)
```bash
# Windows / Linux / MacOS
mvn javafx:run
```
Để chạy nhiều client đồng thời, mở nhiều terminal và lặp lại lệnh trên.

## Danh sách chức năng đã hoàn thành
- [x] Đăng ký / Đăng nhập / Đăng xuất
- [x] Xem danh sách sản phẩm đấu giá
- [x] Đăng sản phẩm đấu giá (Seller)
- [x] Đặt giá theo thời gian thực (Bidding)
- [x] Realtime update giá cao nhất tới tất cả client
- [x] Tự động kết thúc phiên đấu giá khi hết giờ
- [x] Xem lịch sử đấu giá
- [x] Quản lý người dùng (Admin: khoá / mở khoá tài khoản)
- [x] Duyệt sản phẩm (Admin: pending / approved / rejected)
- [x] Nạp tiền vào tài khoản
- [x] Quản lý hồ sơ cá nhân (User / Seller / Admin)
- [x] Thống kê hệ thống (Admin)
- [x] AutoBid tự động đấu giá
- [x] Xử lý lỗi concurrent bidding

## Báo cáo và Demo
- 📄 Báo cáo PDF: [Link Google Drive]
- 🎬 Video demo: [Link Google Drive]
  EOF