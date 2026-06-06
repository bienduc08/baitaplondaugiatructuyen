# Hệ Thống Đấu Giá Trực Tuyến UET

Bài tập lớn môn Lập trình nâng cao — nhóm 4 thành viên. Hệ thống mô phỏng nền tảng đấu giá trực tuyến dạng desktop, trong đó nhiều client JavaFX kết nối tới một server TCP để đăng nhập, quản lý phiên đấu giá, đặt giá theo thời gian thực và xử lý ví điện tử.

Repository: [https://github.com/bienduc08/baitaplondaugiatructuyen](https://github.com/bienduc08/baitaplondaugiatructuyen)

---

## 1. Phạm vi hệ thống

Hệ thống tập trung vào luồng đấu giá nội bộ chạy local/demo:

- Người dùng đăng ký, đăng nhập và sử dụng hệ thống theo vai trò `USER`, `SELLER`, `ADMIN`.
- Seller tạo sản phẩm đấu giá, chờ Admin duyệt trước khi phiên được mở.
- User xem danh sách phiên đang diễn ra, vào trang chi tiết để đặt giá thủ công hoặc đăng ký đấu tự động (auto-bid).
- Server cập nhật giá thầu realtime tới tất cả client qua broadcast TCP socket.
- Admin duyệt sản phẩm, quản lý người dùng (khoá / mở khoá tài khoản) và theo dõi toàn bộ hệ thống.
- Ví điện tử cho phép nạp tiền; server kiểm tra số dư trước mỗi lượt đặt giá.

---

## 2. Thành viên

| Thành viên | Phụ trách chính                                  |
|---|--------------------------------------------------|
| Biện Minh Đức | Kiến trúc tổng thể, server, concurrency, review , DAO / Database |
| Nguyễn Đình Đức | Live bidding UI, auto-bid, schema MySQL                     |
| Đinh Văn Toàn | Client JavaFX, giao diện người dùng              |
| Trần Đức Tuấn | realtime chart         |

---

## 3. Công nghệ và môi trường

| Nhóm | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Build | Apache Maven 3.8+ |
| Client | JavaFX 21, FXML, CSS |
| Server | TCP Socket, Java Object Serialization |
| Database | MySQL 8.x |
| Serialization | Gson 2.10.1 |
| Testing | JUnit Jupiter 5 |

**Yêu cầu cài đặt:**

- JDK 21+
- Apache Maven 3.8+
- MySQL Server 8.0+
- Hệ điều hành có môi trường đồ họa để chạy JavaFX client: Windows, macOS hoặc Linux desktop

---

## 4. Cấu trúc dự án

Dự án là một Maven single-module, toàn bộ source code nằm trong `src/main/java/com/uet/auction/`:

```
src/main/java/com/uet/auction/
├── client/
│   ├── controller/     # Xử lý giao diện (Login, ProductDetail, Admin, Seller, User...)
│   ├── network/        # SocketClient, ResponseListener
│   └── util/           # AlertHelper, SceneManager, SessionManager, CountdownTask
├── server/
│   ├── config/         # DatabaseConnection, DbMigrator
│   ├── DAO/            # Truy vấn DB: UserDAO, ProductDAO, BidDAO
│   ├── model/          # Entity: User, Product, Bid, Auction, AutoBidConfig...
│   ├── network/        # SocketServer, ClientHandler
│   └── service/        # AuctionService, AuthService, AuctionTimer, SessionManager
└── common/
    ├── DTO/            # AuctionDTO, BidDTO, ProductDTO, UserDTO
    ├── Request/        # AuctionRequest, LoginRequest
    ├── Response/       # AuctionResponse, LoginResponse
    └── exception/      # AuctionException, InvalidBidException, UnauthorizedException

src/main/resources/com/uet/auction/
├── view/               # Các file FXML giao diện
├── style/              # style.css
└── images/             # Ảnh mặc định, logo
```

---

## 5. Cài đặt và chạy

### Bước 1 — Tạo database

```sql
mysql -u root -p < schema.sql
```

File `schema.sql` ở thư mục gốc repository sẽ tạo database `auction_db` và các bảng `users`, `products`, `bids`.

### Bước 2 — Cấu hình kết nối database

Chỉnh thông tin kết nối (host, port, username, password) tại:

```
src/main/java/com/uet/auction/server/config/DatabaseConnection.java
```

Mặc định: `jdbc:mysql://localhost:3306/auction_db`

### Bước 3 — Build project

```bash
mvn clean package -DskipTests
```

### Bước 4 — Chạy Server (chạy trước)

```bash
mvn exec:java -Dexec.mainClass="com.uet.auction.server.ServerApplication"
```

Server khởi động, lắng nghe kết nối tại cổng `8080`, và tự động chạy `AuctionTimer` để quản lý vòng đời phiên đấu giá (PENDING → OPEN → CLOSED) mỗi 5 giây.

### Bước 5 — Chạy Client (sau khi server đã khởi động)

```bash
mvn javafx:run
```

Để demo nhiều người dùng đồng thời, mở thêm terminal và lặp lại lệnh trên. Client tự động kết nối lại mỗi 3 giây nếu mất kết nối với server.

---

## 6. Tài khoản demo

Tạo thủ công tài khoản qua màn hình đăng ký, hoặc insert thẳng vào database:

| Username | Vai trò | Ghi chú |
|---|---|---|
| *(tự tạo)* | `ADMIN` | Chọn role ADMIN khi đăng ký |
| *(tự tạo)* | `SELLER` | Chọn role SELLER khi đăng ký |
| *(tự tạo)* | `USER` | Role mặc định, dùng để đặt giá |

---

## 7. Chức năng đã hoàn thành

**Xác thực & Hồ sơ**
- [x] Đăng ký tài khoản với đầy đủ thông tin (họ tên, username, email, số điện thoại, mật khẩu, vai trò)
- [x] Đăng nhập / đăng xuất, phân quyền theo vai trò
- [x] Chỉnh sửa hồ sơ cá nhân (họ tên, số điện thoại, đổi mật khẩu)
- [x] Nạp tiền vào tài khoản

**Người dùng (USER)**
- [x] Xem danh sách sản phẩm đang đấu giá (OPEN), đếm ngược thời gian thực
- [x] Tìm kiếm và lọc sản phẩm theo danh mục
- [x] Vào trang chi tiết sản phẩm: xem lịch sử đặt giá, biểu đồ LineChart diễn biến giá, người đang giữ đỉnh
- [x] Đặt giá thủ công với kiểm tra số dư và bước giá tối thiểu
- [x] Đăng ký đấu giá tự động (auto-bid) với giá tối đa và bước tăng tuỳ chỉnh
- [x] Xem các phiên đã tham gia

**Seller**
- [x] Tạo sản phẩm đấu giá kèm ảnh, mô tả, giá khởi điểm, bước giá, thời gian bắt đầu/kết thúc
- [x] Xem danh sách sản phẩm của mình và trạng thái duyệt

**Admin**
- [x] Duyệt hoặc từ chối sản phẩm (PENDING → OPEN / REJECTED)
- [x] Quản lý người dùng: xem danh sách, tìm kiếm, khoá / mở khoá tài khoản

**Server & Realtime**
- [x] Broadcast realtime khi có lượt đặt giá mới — tất cả client cập nhật đồng thời
- [x] Tự động chuyển trạng thái phiên: đúng giờ mở (PENDING → OPEN), hết giờ đóng (OPEN → CLOSED)
- [x] Anti-sniping: tự động gia hạn thêm 5 phút nếu có bid trong 30 giây cuối
- [x] Auto-bid engine: kích hoạt và xử lý chuỗi đặt giá tự động sau mỗi lượt bid thủ công
- [x] Thông báo kết thúc phiên broadcast tới toàn bộ client kèm tên người thắng và giá trúng
- [x] Thread pool giới hạn 50 client đồng thời, xử lý concurrent bidding với `synchronized`
- [x] Client tự động kết nối lại khi mất kết nối

---

## 8. Kiến trúc realtime

```
Client A  ──┐
Client B  ──┼──▶  SocketServer (port 8080)
Client C  ──┘        │
                     ├── ClientHandler (thread pool, tối đa 50)
                     ├── AuctionService  (xử lý bid, auto-bid)
                     ├── AuthService     (xác thực, quản lý user)
                     └── AuctionTimer    (cron 5s: PENDING→OPEN→CLOSED, anti-sniping)
                              │
                         MySQL 8.x (auction_db)
                         ├── users
                         ├── products
                         └── bids
```

Mỗi sự kiện thay đổi giá hoặc trạng thái phiên đều kích hoạt `SocketServer.broadcast()` — tất cả client nhận update đồng thời mà không cần polling.

---

## 9. Database schema

Ba bảng chính:

| Bảng | Mô tả |
|---|---|
| `users` | Thông tin tài khoản, vai trò, số dư, trạng thái |
| `products` | Thông tin sản phẩm đấu giá, giá hiện tại, người đang giữ đỉnh, thời gian |
| `bids` | Lịch sử toàn bộ lượt đặt giá |

Xem chi tiết tại [`schema.sql`](schema.sql).

---

## 10. Báo cáo và Demo

- 📄 Báo cáo PDF: *(cập nhật link)*
- 🎬 Video demo: *(cập nhật link)*

**Kịch bản demo đề xuất:**

1. Chạy server: `mvn exec:java -Dexec.mainClass="com.uet.auction.server.ServerApplication"`
2. Mở 3 terminal, chạy 3 client: `mvn javafx:run`
3. Đăng nhập với tài khoản Seller → tạo sản phẩm đấu giá mới
4. Đăng nhập Admin → duyệt sản phẩm vừa tạo
5. Đăng nhập 2 tài khoản User → cùng vào phiên, cạnh tranh đặt giá realtime
6. Một User đăng ký auto-bid — quan sát hệ thống tự động đặt giá
7. Chờ hoặc rút ngắn thời gian phiên để xem thông báo kết thúc và tên người thắng