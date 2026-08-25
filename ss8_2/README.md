# Bài 2 - Chiến lược Chunking cho tài liệu CRM

Project độc lập minh họa hai bean `TextSplitter` trong Spring AI 2.0.0:

- `tokenBasedTextSplitter`: chia theo số token, phù hợp tài liệu quy trình dạng danh sách.
- `headerBasedTextSplitter`: splitter nội bộ theo tiêu đề Markdown `#` và `##`, phù hợp quy chế dạng chương/điều.

## Mã nguồn cấu hình

File chính: `src/main/java/demo/crm/config/ChunkingConfig.java`.

Spring AI 2.0.0 chưa cung cấp `MarkdownHeaderTextSplitter` trong artifact chuẩn. Project định nghĩa implementation nhỏ kế thừa `TextSplitter`; tiêu đề được giữ nguyên ở đầu chunk và section ngắn hơn 120 ký tự được nhập vào section trước.

### Token-based

```java
@Bean
public TextSplitter tokenBasedTextSplitter() {
    return TokenTextSplitter.builder()
        .withChunkSize(600)
        .withMinChunkSizeChars(120)
        .withMinChunkLengthToEmbed(20)
        .withMaxNumChunks(10_000)
        .withKeepSeparator(true)
        .build();
}
```

### Header-based

```java
@Bean
public TextSplitter headerBasedTextSplitter() {
    return new MarkdownHeaderTextSplitter(120);
}
```

## So sánh chi tiết

| Tiêu chí | Token-based Chunking | Header-based Chunking |
|---|---|---|
| Cách chia | Theo kích thước token cố định, có giới hạn tối đa | Theo ranh giới tiêu đề Markdown `#`, `##` |
| Loại A - quy trình hoàn tiền | Ưu: kích thước ổn định, dễ batch embedding, xử lý được tài liệu dài. Nhược: có thể cắt giữa Bước 2 và Bước 3, khiến điều kiện của bước bị tách khỏi hành động. | Ưu: nếu mỗi bước là heading thì giữ trọn bước. Nhược: tài liệu không dùng heading cho từng bước sẽ tạo chunk lớn hoặc không chia đúng. |
| Loại B - quy chế dài | Ưu: không phụ thuộc Markdown chuẩn và luôn giới hạn kích thước. Nhược: có thể cắt giữa `Điều 1` và phần ngoại lệ, làm mất quan hệ cấu trúc. | Ưu: bảo toàn ngữ nghĩa Chương/Điều, kết quả truy vấn dễ truy vết. Nhược: section có độ dài không đều; một Điều rất dài cần splitter phụ trợ. |
| Chất lượng retrieval | Kích thước đều giúp embedding/index ổn định nhưng ngữ nghĩa có thể bị cắt. | Giữ ngữ nghĩa tốt hơn khi Markdown có cấu trúc đúng, nhưng chunk quá dài làm embedding loãng. |
| Chi phí | Dự đoán được số token và chi phí embedding. | Phụ thuộc số lượng/độ dài section; có thể cần bước tách thứ hai. |
| Khả năng chống dữ liệu xấu | Tốt hơn với văn bản tự do. | Phụ thuộc chất lượng heading và quy ước soạn thảo. |

## Cơ chế bảo vệ ngữ cảnh

Với Loại A, `chunkSize=600` đủ rộng để một chunk chứa nhiều câu liên tiếp của quy trình; `withKeepSeparator(true)` giữ dấu phân cách khi ghép nội dung. `minChunkSizeChars=120` ngăn việc tạo các mẩu quá ngắn, ví dụ một câu điều kiện hoặc phần kết luận bị tách riêng. Khi section nhỏ hơn ngưỡng, implementation header gộp nó vào chunk trước, nhờ đó ngữ cảnh điều kiện và hành động được truy xuất cùng nhau.

Với Loại B, header được đưa vào chính chunk thay vì dùng header chỉ làm delimiter. Vì vậy embedding chứa cả nhãn `Chương I`/`Điều 1`, giúp truy vấn semantic và citation nhận biết phạm vi của đoạn. Với Điều rất dài, nên chạy thêm TokenTextSplitter sau header splitter; khi đó có thể lặp lại header trong metadata hoặc prefix của các sub-chunk.

Không có giá trị tối ưu cho mọi dữ liệu: 120 là điểm bắt đầu cần đánh giá bằng tập câu hỏi CRM, recall@k và tỷ lệ câu trả lời có đủ điều kiện/ngoại lệ.

## Minh chứng chạy thực tế

Test `ChunkingConfigTest` khởi tạo `AnnotationConfigApplicationContext` trực tiếp, không cần Ollama hay database. Log chạy thành công:

```text
> Task :compileJava
> Task :compileTestJava
> Task :test
BUILD SUCCESSFUL
```

Test xác nhận Context có đúng hai bean `TextSplitter` và header vẫn nằm trong chunk tương ứng.

## Chạy project

Project đã có Gradle wrapper riêng:

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

## Đẩy lên GitHub

Tạo repository riêng cho bài 2, sau đó chạy:

```powershell
git init
git add .
git commit -m "Add CRM chunking strategies"
git branch -M main
git remote add origin https://github.com/<username>/<repository-bai-2>.git
git push -u origin main
```

Repository link cần nộp: thay `<username>/<repository-bai-2>` bằng URL repository GitHub thực tế sau khi push.
