# fall_fire_detection

Để tối ưu hóa model cho ScyllaDB, ta cần tập trung vào **query patterns** và thiết kế bảng denormalized. Dưới đây là giải pháp chi tiết:

---

### **1. Xác định Query Patterns**
Giả định các truy vấn phổ biến:
1. Lấy lịch sử request theo `user_id`, sắp xếp giảm dần theo thời gian.
2. Tìm request theo `device_id` và `event_type` trong khoảng thời gian.
3. Truy xuất chi tiết request bằng `message_id`.

---

### **2. Thiết Kế Bảng Dữ Liệu**

#### **Bảng 1: `ai_requests_by_user`**
- **Mục đích:** Hỗ trợ truy vấn theo `user_id`.
- **Partition Key:** `(user_id, bucket)` (sử dụng bucket để tránh partition quá lớn).
- **Clustering Key:** `timestamp` (sắp xếp giảm dần), `message_id`.

```sql
CREATE TABLE ai_requests_by_user (
    user_id TEXT,
    bucket INT,  -- Ví dụ: hash(user_id) % 10
    timestamp TIMESTAMP,
    message_id TEXT,
    device_id TEXT,
    face_id TEXT,
    event_type TEXT,
    status TEXT,
    details TEXT,
    thumbnail_url TEXT,
    PRIMARY KEY ((user_id, bucket), timestamp, message_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);
```

#### **Bảng 2: `ai_requests_by_device_event`**
- **Mục đích:** Tìm kiếm theo `device_id` và `event_type`.
- **Partition Key:** `(device_id, event_type)`.
- **Clustering Key:** `timestamp`, `message_id`.

```sql
CREATE TABLE ai_requests_by_device_event (
    device_id TEXT,
    event_type TEXT,
    timestamp TIMESTAMP,
    message_id TEXT,
    user_id TEXT,
    face_id TEXT,
    status TEXT,
    details TEXT,
    thumbnail_url TEXT,
    PRIMARY KEY ((device_id, event_type), timestamp, message_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);
```

#### **Bảng 3: `ai_requests_by_message`**
- **Mục đích:** Truy xuất nhanh bằng `message_id`.
- **Primary Key:** `message_id`.

```sql
CREATE TABLE ai_requests_by_message (
    message_id TEXT PRIMARY KEY,
    user_id TEXT,
    device_id TEXT,
    face_id TEXT,
    event_type TEXT,
    timestamp TIMESTAMP,
    status TEXT,
    details TEXT,
    thumbnail_url TEXT,
    image_status_list LIST<TEXT>  -- Hoặc JSON serialized
);
```

---

### **3. Tối Ưu Hóa Dữ Liệu**
- **Tránh lưu trữ dữ liệu lớn:** Thay vì lưu `thumbnail` (binary), chỉ lưu `thumbnail_url`.
- **Xử lý danh sách:** Dùng kiểu `LIST<TEXT>` hoặc serialize `imageStatusList` thành JSON.
- **Kiểu dữ liệu timestamp:** Sử dụng `TIMESTAMP` để sắp xếp và lọc theo thời gian.

---

### **4. Cách Thức Insert Dữ Liệu**
- **Ghi đè vào nhiều bảng:** Khi có request mới, insert đồng thời vào cả 3 bảng để tối ưu truy vấn.
- **Asynchronous writes:** Sử dụng async queries để tăng hiệu suất.

```java
// Ví dụ code Java (Async)
public CompletionStage<Void> saveRequest(AiServiceRequestForm request) {
    // Chuyển đổi sang các đối tượng của từng bảng
    AiRequestByUser userRequest = convertToUserRequest(request);
    AiRequestByDeviceEvent deviceRequest = convertToDeviceRequest(request);
    AiRequestByMessage messageRequest = convertToMessageRequest(request);

    // Thực hiện insert async
    return CompletableFuture.allOf(
        aiServiceServiceImpl.insertUserRequestAsync(userRequest),
        aiServiceServiceImpl.insertDeviceRequestAsync(deviceRequest),
        aiServiceServiceImpl.insertMessageRequestAsync(messageRequest)
    );
}
```

---

### **5. Giải Pháp Backup Consistency**
- **Sử dụng Batch Log:** Ghi dữ liệu vào một bảng log trước, sau đó dùng batch job để đồng bộ sang các bảng khác (nếu cần đảm bảo consistency).
- **Materialized Views (nếu phù hợp):** Tạo view từ bảng chính, nhưng cần kiểm tra giới hạn của ScyllaDB.

---

### **6. Xử Lý Các Trường Không Dùng Đến**
- **Loại bỏ trường thừa:** Ví dụ `eventTypeInt` nếu `eventType` đã đủ.
- **Tách trường lớn:** Lưu `thumbnail_url` thay vì `thumbnail`.

---

### **Kết Luận**
- Thiết kế denormalized tables dựa trên query patterns.
- Sử dụng partition key và clustering key phù hợp để tối ưu hiệu năng.
- Đánh đổi giữa write overhead và read performance.
- Code cần xử lý insert vào nhiều bảng đồng thời.

---
---
---
Để tối ưu hóa phương thức `searchAiEvent` cho ScyllaDB, ta cần tập trung vào **thiết kế bảng denormalized**, **query patterns**, và **xử lý các ràng buộc của ScyllaDB**. Dưới đây là giải pháp chi tiết:

---

### **1. Xác định Query Patterns Chính**
Dựa trên tham số đầu vào, các truy vấn cần hỗ trợ:
1. Tìm kiếm event theo `user_id` + `deviceIds` (nhiều device) + `eventType` + `category` + `isRead` trong khoảng thời gian.
2. Phân trang theo `page` và `pageSize`.
3. Tìm kiếm full-text (`searchString`).

---

### **2. Thiết Kế Bảng Dữ Liệu**

#### **Bảng 1: `events_by_user`**
- **Mục đích:** Hỗ trợ truy vấn theo `user_id` kết hợp các điều kiện lọc.
- **Partition Key:** `(user_id, bucket)` (sử dụng `bucket` để tránh partition lớn).
- **Clustering Key:**
    - `event_date` (sắp xếp giảm dần)
    - `device_id`
    - `event_type`
    - `category`
    - `message_id` (đảm bảo uniqueness)

```sql
CREATE TABLE events_by_user (
    user_id TEXT,
    bucket INT,          -- Ví dụ: hash(user_id) % 10
    event_date TIMESTAMP,
    device_id TEXT,
    event_type TEXT,
    category INT,
    message_id TEXT,
    description TEXT,
    thumbnail_url TEXT,
    is_read BOOLEAN,
    string_description TEXT,
    string_type TEXT,
    first_label TEXT,
    -- Các trường khác...
    PRIMARY KEY ((user_id, bucket), event_date, device_id, event_type, category, message_id)
) WITH CLUSTERING ORDER BY (event_date DESC);
```

#### **Tối Ưu Hóa Truy Vấn:**
- **Lọc theo `deviceIds`:** Sử dụng `IN` clause trên clustering key `device_id` (chỉ áp dụng nếu số lượng device ít).
- **Phân trang:** Sử dụng `pagingState` của ScyllaDB để lấy trang tiếp theo.
- **Điều kiện thời gian:** Tận dụng `event_date` đã sắp xếp DESC.

**Ví dụ Query:**
```sql
SELECT * FROM events_by_user
WHERE
    user_id = 'user1' AND bucket = 1
    AND event_date >= '2023-01-01' AND event_date <= '2023-12-31'
    AND device_id IN ('device1', 'device2')
    AND event_type = 'MOTION_DETECTED'
    AND category = 1
    AND is_read = false;
```

---

### **3. Xử Lý Tìm Kiếm Full-Text (`searchString`)**
ScyllaDB không hỗ trợ full-text search, nên cần kết hợp **Elasticsearch**:
1. Lưu trữ các trường cần tìm kiếm (`string_description`, `string_type`, `first_label`) vào Elasticsearch.
2. Khi có `searchString`, gửi query đến Elasticsearch để lấy danh sách `message_id`.
3. Sử dụng kết quả từ Elasticsearch để query chính trong ScyllaDB bằng `IN` clause.

**Ví dụ Flow:**
```java
List<String> messageIds = elasticsearchClient.search(searchString);
List<DeviceIVAEvent> events = session.execute(
    QueryBuilder.selectFrom("events_by_user")
        .all()
        .whereColumn("message_id").in(messageIds)
        // Thêm các điều kiện khác...
);
```

---

### **4. Tối Ưu Hóa Phân Trang**
Sử dụng **client-side paging** của ScyllaDB để tránh offset chậm:
```java
public List<DeviceIVAEvent> searchAiEvent(
    String userId, 
    List<String> deviceIds, 
    String searchString, 
    // ... các tham số khác
) {
    int adjustedPage = page <= 0 ? 1 : page;
    int offset = (adjustedPage - 1) * pageSize;

    Statement query = QueryBuilder.selectFrom("events_by_user")
        .all()
        .whereColumn("user_id").isEqualTo(userId)
        .whereColumn("bucket").isEqualTo(bucketForUser(userId))
        .whereColumn("event_date").isGreaterThanOrEqualTo(startTime)
        .whereColumn("event_date").isLessThanOrEqualTo(endTime)
        .whereColumn("device_id").in(deviceIds.stream().map(Text::valueOf).collect(Collectors.toList()))
        // ... thêm các điều kiện khác
        .orderBy(ClusteringOrder.DESC)
        .setPageSize(pageSize)
        .setPagingState(pagingState); // Lưu paging state từ lần trước

    ResultSet result = session.execute(query);
    return convertToEvents(result);
}
```

---

### **5. Giải Pháp Cho Số Lượng Device Lớn**
Nếu `deviceIds` quá nhiều (hàng trăm/thousands), thay vì dùng `IN`, thiết kế **bảng riêng cho từng device**:
```sql
CREATE TABLE events_by_user_device (
    user_id TEXT,
    device_id TEXT,
    event_date TIMESTAMP,
    message_id TEXT,
    -- Các trường khác...
    PRIMARY KEY ((user_id, device_id), event_date)
) WITH CLUSTERING ORDER BY (event_date DESC);
```

**Query riêng cho từng `device_id` và merge kết quả:**
```java
List<ResultSetFuture> futures = new ArrayList<>();
for (String deviceId : deviceIds) {
    Statement query = QueryBuilder.selectFrom("events_by_user_device")
        .all()
        .whereColumn("user_id").isEqualTo(userId)
        .whereColumn("device_id").isEqualTo(deviceId)
        .whereColumn("event_date").isGreaterThanOrEqualTo(startTime)
        .whereColumn("event_date").isLessThanOrEqualTo(endTime);
    futures.add(session.executeAsync(query));
}

// Merge và phân trang kết quả từ tất cả futures
```

---

### **6. Đánh Đổi và Lưu Ý**
- **Write Overhead:** Ghi dữ liệu vào nhiều bảng (denormalized) để tối ưu read.
- **Tính nhất quán:** Sử dụng batch hoặc asynchronous writes.
- **Secondary Index:** Tránh dùng cho `is_read`, thay vào đó thêm vào clustering key nếu cần.
- **Time Bucketing:** Chia `bucket` theo thời gian (ví dụ: theo ngày) nếu event rate cao.

---

### **7. Ví Dụ Code Java**
```java
public List<DeviceIVAEvent> searchAiEvent(
    String userId,
    List<String> deviceIds,
    String searchString,
    String eventType,
    int eventCat,
    Boolean read,
    Long startTime,
    Long endTime,
    int page,
    int pageSize
) {
    // Xử lý searchString với Elasticsearch
    List<String> messageIds = elasticsearchClient.search(searchString);

    // Xác định bucket cho user
    int bucket = hash(userId) % 10;

    // Xây dựng query ScyllaDB
    Select query = QueryBuilder.selectFrom("events_by_user")
        .all()
        .whereColumn("user_id").isEqualTo(Text.valueOf(userId))
        .whereColumn("bucket").isEqualTo(Int.valueOf(bucket))
        .whereColumn("event_date").isGreaterThanOrEqualTo(startTime)
        .whereColumn("event_date").isLessThanOrEqualTo(endTime)
        .whereColumn("device_id").in(deviceIds.stream().map(Text::valueOf).collect(Collectors.toList()))
        .whereColumn("event_type").isEqualTo(Text.valueOf(eventType))
        .whereColumn("category").isEqualTo(Int.valueOf(eventCat))
        .whereColumn("is_read").isEqualTo(Boolean.valueOf(read))
        .whereColumn("message_id").in(messageIds.stream().map(Text::valueOf).collect(Collectors.toList()))
        .orderBy(ClusteringOrder.DESC)
        .limit(pageSize)
        .pagingState(pagingState);

    // Thực thi query
    ResultSet result = session.execute(query);
    return convertToEvents(result);
}
```

---

### **Kết Luận**
- Sử dụng bảng `events_by_user` với partition key kết hợp `user_id` và `bucket`.
- Kết hợp Elasticsearch cho tìm kiếm full-text.
- Tối ưu phân trang qua `pagingState`.
- Xử lý số lượng device lớn bằng cách query riêng từng device và merge kết quả.
- Đánh đổi write overhead để đạt read performance.

src/main/java/com/camai/fallfire/
├── FallFireApplication.java
├── config/
│   ├── ScyllaConfig.java
│   └── HazelcastConfig.java
├── model/
│   ├── Event.java
│   ├── EventType.java
│   └── EventStatus.java
├── repository/
│   ├── EventRepository.java
│   └── ScyllaEventRepository.java
├── service/
│   ├── EventService.java
│   └── EventServiceImpl.java
├── controller/
│   └── EventController.java
└── exception/
└── EventException.java