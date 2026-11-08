# [Bài tập] Tối ưu hóa hiển thị sản phẩm với Redis Cache

Dự án triển khai theo kiến trúc **Clean Layered Architecture** sử dụng **Spring Boot 4 (v4.1.2)**, **JDK 17**, cơ sở dữ liệu **PostgreSQL** và bộ nhớ đệm **Redis Cache** (chạy trên **Docker Desktop**).

---

## 1. Yêu cầu kỹ thuật đã đạt được
* **Mô hình Cache-Aside**:
  * **Lần 1 (Cache Miss)**: Đọc từ Database (mất ~200-300ms), tự động serialize sang JSON và lưu vào Redis.
  * **Lần 2 trở đi (Cache Hit)**: Đọc trực tiếp từ Redis (mất <10ms, không chạm tới DB).
* **Quản lý TTL**: Cấu hình thời gian sống **30 phút** (`Duration.ofMinutes(30)`) cho cache `products`.
* **Cơ chế Cache Invalidation (@CacheEvict)**: Tự động xóa cache trong Redis ngay khi sản phẩm được cập nhật (`PUT /api/v1/products/{id}`).
* **Serialization hiện đại**: Sử dụng `RedisSerializer.json()` để lưu dữ liệu JSON trong suốt, tránh nhị phân JDK.

---

## 2. Hướng dẫn khởi chạy

### Bước 1: Khởi động PostgreSQL & Redis với Docker Desktop
Tại thư mục gốc dự án:
```bash
docker compose up -d
```
* **PostgreSQL**: `localhost:5432` (Database: `product_db`, User: `postgres`, Password: `postgres`)
* **Redis**: `localhost:6379`

### Bước 2: Chạy ứng dụng Spring Boot
Di chuyển vào thư mục `product-service`:
```bash
cd product-service
./gradlew bootRun
```
*Ứng dụng sẽ tự động khởi tạo bảng `products` trong PostgreSQL và thêm sẵn 1 sản phẩm mẫu ID = 1.*

---

## 3. Kiểm thử API & Hiệu năng

### 3.1. Truy vấn chi tiết sản phẩm (Lần 1 - Cache Miss)
```bash
curl -X GET http://localhost:8080/api/v1/products/1
```
* **Thời gian phản hồi**: ~200ms - 300ms.
* **Console Log**: `===> [CACHE MISS] Đang truy vấn từ Database cho sản phẩm id: 1`

### 3.2. Truy vấn chi tiết sản phẩm (Lần 2 trở đi - Cache Hit)
```bash
curl -X GET http://localhost:8080/api/v1/products/1
```
* **Thời gian phản hồi**: `< 10ms`.
* **Console Log**: Không log query Database (đã lấy từ Redis).

### 3.3. Kiểm tra dữ liệu và TTL trong Redis CLI
Mở terminal hoặc container Redis:
```bash
docker exec -it product_redis redis-cli

# Xem danh sách key
KEYS *
# Output: "products::1"

# Xem định dạng JSON được lưu
GET "products::1"

# Kiểm tra TTL (còn lại ~1800 giây = 30 phút)
TTL "products::1"
```

### 3.4. Cập nhật sản phẩm & Xóa Cache (@CacheEvict)
```bash
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bàn phím cơ RK84 V2 Pro",
    "description": "Bàn phím cơ Bluetooth RGB",
    "price": 1390000,
    "imageUrl": "https://example.com/rk84-pro.jpg"
  }'
```
* Sau khi PUT thành công, kiểm tra lại trong `redis-cli`: key `products::1` đã bị xóa.
* Gọi lại `GET`, thời gian lại mất ~200ms để query dữ liệu mới nhất từ PostgreSQL và nạp lại vào Redis.

---

## 4. Chạy Integration Test tự động
Dự án đã có sẵn test case kiểm chứng toàn bộ luồng hoạt động:
```bash
cd product-service
./gradlew test
```
