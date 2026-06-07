# 🏆 Hệ Thống Đấu Giá Trực Tuyến UET

> Bài tập lớn môn Lập trình nâng cao — Trường Đại học Công nghệ, ĐHQGHN
> Hệ thống mô phỏng nền tảng đấu giá trực tuyến dạng desktop, nhiều client JavaFX kết nối tới một server TCP để đăng nhập, quản lý phiên đấu giá, đặt giá theo thời gian thực và xử lý ví điện tử.

🔗 Repository: [github.com/bienduc08/baitaplondaugiatructuyen](https://github.com/bienduc08/baitaplondaugiatructuyen)

---

## 👥 Thành viên nhóm

| Thành viên | Phụ trách chính |
|---|---|
| Biện Minh Đức | Kiến trúc tổng thể, server, concurrency, review, DAO / Database |
| Nguyễn Đình Đức | Live bidding UI, auto-bid, schema MySQL |
| Đinh Văn Toàn | Client JavaFX, giao diện người dùng |
| Trần Đức Tuấn | Realtime chart |

---

## 🛠️ Công nghệ sử dụng

| Nhóm | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Build | Apache Maven 3.8+ |
| Client | JavaFX 21, FXML, CSS |
| Server | TCP Socket, Java Object Serialization |
| Database | MySQL 8.x |
| Testing | JUnit Jupiter 5 |

**Yêu cầu cài đặt:** JDK 21+ · Maven 3.8+ · MySQL 8.0+ · Hệ điều hành có đồ họa (Windows / macOS / Linux desktop)

---

## 📁 Cấu trúc dự án

Dự án là một Maven single-module, toàn bộ source code nằm trong `src/main/java/com/uet/auction/`:

```
src/main/java/com/uet/auction/
├── client/
│   ├── controller/     # Xử lý giao diện (Login, ProductDetail, Admin, Seller, User...)
│   ├── network/        # SocketClient, ResponseListener
│   └── util/           # AlertHelper, SceneManager, SessionManager, CountdownTask
├── server/
│   ├── config/         # DatabaseConnection, DbMigrator
│   ├── DAO/            # Truy vấn DB: UserDAO, ProductDAO, BidDAO, NotificationDAO
│   ├── model/          # Entity: User, Product, Bid, Auction, AutoBidConfig...
│   ├── network/        # SocketServer, ClientHandler
│   └── service/        # AuctionService, AuthService, AuctionTimer, SessionManager
└── common/
    ├── DTO/            # AuctionDTO, BidDTO, ProductDTO, UserDTO, NotificationDTO
    ├── Request/        # AuctionRequest, LoginRequest
    ├── Response/       # AuctionResponse, LoginResponse
    └── exception/      # AuctionException, InvalidBidException, UnauthorizedException

src/main/resources/com/uet/auction/
├── view/               # Các file FXML giao diện
├── style/              # style.css
└── images/             # Ảnh mặc định, logo
```

---

## 🚀 Cài đặt và chạy

### Bước 1 — Tạo database

```sql
mysql -u root -p < schema.sql
```

File `schema.sql` tạo database `auction_db` với các bảng `users`, `products`, `bids`, `notifications` và dữ liệu mẫu sẵn.

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

### Bước 4 — Chạy Server *(chạy trước)*

```bash
mvn exec:java -Dexec.mainClass="com.uet.auction.server.ServerApplication"
```

Server lắng nghe tại cổng `8080` và tự động chạy `AuctionTimer` mỗi 5 giây để quản lý vòng đời phiên đấu giá (APPROVED → OPEN → CLOSED).

### Bước 5 — Chạy Client

```bash
mvn javafx:run
```

Để demo nhiều người dùng đồng thời, mở thêm terminal và lặp lại lệnh trên. Client tự động kết nối lại mỗi 3 giây nếu mất kết nối.

---

## 🔑 Tài khoản demo

Mật khẩu mặc định tất cả tài khoản: **`123456`**

| Username | Vai trò | Số dư |
|---|---|---|
| `admin` | ADMIN | 0 VNĐ |
| `seller1` | SELLER | 0 VNĐ |
| `user1` | USER | 50.000.000 VNĐ |
| `user2` | USER | 20.000.000 VNĐ |

---

## ✅ Chức năng đã hoàn thành

**Xác thực & Hồ sơ**
- [x] Đăng ký tài khoản với đầy đủ thông tin (họ tên, username, email, số điện thoại, mật khẩu, vai trò)
- [x] Đăng nhập / đăng xuất, phân quyền theo vai trò; chặn tài khoản bị khoá
- [x] Chỉnh sửa hồ sơ cá nhân (họ tên, số điện thoại, đổi mật khẩu)
- [x] Nạp tiền vào ví (tối đa 500.000.000 VNĐ / lần)

**Người dùng (USER)**
- [x] Xem danh sách phiên đang đấu giá (OPEN), đếm ngược thời gian thực
- [x] Tìm kiếm và lọc sản phẩm theo danh mục
- [x] Trang chi tiết: lịch sử đặt giá, biểu đồ LineChart diễn biến giá, người đang giữ đỉnh
- [x] Đặt giá thủ công với kiểm tra số dư và bước giá tối thiểu
- [x] Đăng ký đấu giá tự động (auto-bid) với giá tối đa và bước tăng tuỳ chỉnh
- [x] Xem danh sách các phiên đã tham gia

**Seller**
- [x] Tạo sản phẩm đấu giá kèm ảnh, mô tả, giá khởi điểm, bước giá, thời gian bắt đầu/kết thúc
- [x] Chỉnh sửa sản phẩm chờ duyệt (không thể sửa khi phiên đang OPEN)
- [x] Xem danh sách sản phẩm của mình và trạng thái duyệt

**Admin**
- [x] Duyệt hoặc từ chối sản phẩm (PENDING → APPROVED → OPEN khi đến giờ / REJECTED)
- [x] Quản lý người dùng: xem danh sách, tìm kiếm, khoá / mở khoá tài khoản

**Server & Realtime**
- [x] Broadcast realtime khi có lượt đặt giá mới — tất cả client cập nhật đồng thời
- [x] Tự động chuyển trạng thái phiên: Admin duyệt → APPROVED, đúng giờ mở (APPROVED → OPEN), hết giờ đóng (OPEN → CLOSED)
- [x] Anti-sniping: gia hạn thêm 3 phút nếu có bid trong 30 giây cuối (tối đa 3 lần)
- [x] Auto-bid engine: xử lý chuỗi đặt giá tự động dây chuyền sau mỗi lượt bid thủ công
- [x] Thông báo kết thúc phiên broadcast toàn bộ client kèm tên người thắng và giá trúng
- [x] Thread pool giới hạn 50 client đồng thời, xử lý concurrent bidding với `synchronized`
- [x] Client tự động kết nối lại khi mất kết nối

---

## 🔄 Vòng đời phiên đấu giá

```
Seller tạo sản phẩm
        │
        ▼
   [PENDING] ──── Admin từ chối ────▶ [REJECTED]
        │
   Admin duyệt
        │
        ▼
  [APPROVED]
        │
   AuctionTimer: start_time <= NOW()
        │
        ▼
    [OPEN] ◀──── Anti-sniping gia hạn nếu bid trong 30s cuối
        │
   AuctionTimer: end_time <= NOW()
        │
        ▼
   [CLOSED] ──── Trả tiền Seller · Thông báo người thắng
```

---

## 🏗️ Kiến trúc realtime

```
Client A  ──┐
Client B  ──┼──▶  SocketServer (port 8080)
Client C  ──┘        │
                     ├── ClientHandler      (thread pool, tối đa 50)
                     ├── AuctionService     (xử lý bid, auto-bid)
                     ├── AuthService        (xác thực, quản lý user)
                     └── AuctionTimer       (cron 5s: APPROVED→OPEN, anti-sniping, OPEN→CLOSED)
                              │
                         MySQL 8.x (auction_db)
                         ├── users
                         ├── products
                         ├── bids
                         └── notifications
```

Mỗi sự kiện thay đổi giá hoặc trạng thái phiên đều kích hoạt `SocketServer.broadcast()` — tất cả client nhận update đồng thời mà không cần polling.

---

## 🗄️ Database schema

| Bảng | Mô tả |
|---|---|
| `users` | Thông tin tài khoản, vai trò, số dư, trạng thái |
| `products` | Sản phẩm đấu giá, giá hiện tại, người giữ đỉnh, thời gian, số lần gia hạn |
| `bids` | Lịch sử toàn bộ lượt đặt giá |
| `notifications` | Thông báo bị vượt giá, kết thúc phiên cho từng user |

Xem chi tiết tại [`schema.sql`](schema.sql).

---

## 🎬 Kịch bản demo

1. Chạy server: `mvn exec:java -Dexec.mainClass="com.uet.auction.server.ServerApplication"`
2. Mở 3 terminal, chạy 3 client: `mvn javafx:run`
3. Đăng nhập `seller1` → tạo sản phẩm mới với `start_time` trong quá khứ
4. Đăng nhập `admin` → duyệt sản phẩm (trạng thái chuyển APPROVED)
5. Chờ 5 giây → AuctionTimer tự mở phiên (APPROVED → OPEN)
6. Đăng nhập `user1` và `user2` → cùng vào phiên, cạnh tranh đặt giá realtime
7. Một user đăng ký auto-bid → quan sát hệ thống tự động đặt giá dây chuyền
8. Cập nhật `end_time` trực tiếp trong MySQL để rút ngắn → quan sát anti-sniping gia hạn và thông báo kết thúc

---

## 📄 Báo cáo

- 📝 Báo cáo: [Google Docs](https://docs.google.com/document/d/1wZ_LSpcFZGR3Xeszm4fEgzW_rkjZoU1Z/edit?usp=sharing&ouid=111826535776200247084&rtpof=true&sd=true)
- 🎬 Video demo: [Google Divers](https://drive.google.com/file/d/17u22NXIbkbY4chtTHqUnznPAl_O5r0iq/view?usp=sharing)