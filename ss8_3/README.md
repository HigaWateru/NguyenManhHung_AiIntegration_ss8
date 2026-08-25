# Bài 3 - Defensive RAG Retrieval

Project độc lập triển khai `RAGRetrievalService` cho CRM, có ngưỡng similarity động và chặn câu hỏi ngoài phạm vi trước khi gọi LLM.

## Mã nguồn chính

`src/main/java/demo/crm/service/RAGRetrievalService.java`

```java
public String answer(String question) {
    SearchRequest request = SearchRequest.builder()
        .query(question)
        .topK(3)
        .similarityThreshold(similarityThreshold)
        .build();

    List<Document> relevantDocuments = vectorStore.similaritySearch(request).stream()
        .filter(document -> document.getScore() != null)
        .filter(document -> document.getScore() > similarityThreshold)
        .limit(3)
        .toList();

    if (relevantDocuments.isEmpty()) {
        return "Xin lỗi, thông tin bạn tìm kiếm không nằm trong tài liệu quy chế của chúng tôi.";
    }

    return chatClient.prompt()
        .system("Chỉ trả lời dựa trên CONTEXT.")
        .user(buildPrompt(question, relevantDocuments))
        .call()
        .content();
}
```

Threshold được inject bằng `rag.similarity-threshold`, mặc định `0.75`. Service vẫn lọc lại score ở application layer dù đã truyền threshold cho VectorStore, vì defensive code không phụ thuộc hoàn toàn vào việc implementation backend có thực thi filter hay không. `topK(3)` giới hạn dữ liệu lấy từ vector store và `.limit(3)` bảo vệ thêm ở application layer.

## Luồng xử lý

1. Câu hỏi rỗng bị từ chối ngay.
2. VectorStore tìm tối đa 3 ứng viên.
3. Chỉ giữ document có `score > 0.75`.
4. Không có document đạt ngưỡng: trả thông báo mặc định, không tạo prompt và không gọi LLM.
5. Có document đạt ngưỡng: ghép context và mới gọi `ChatClient`.

## Cosine, L2 và Dot Product trong pgvector

| Phép đo | pgvector operator | Ý nghĩa | Nhận xét cho văn bản |
|---|---|---|---|
| Cosine distance/similarity | `<=>` (distance); similarity thường là `1 - distance` | Đo góc giữa hai vector, bỏ qua độ lớn | Phù hợp nhất khi embedding biểu diễn hướng/ngữ nghĩa; ít nhạy với độ dài văn bản |
| L2 Euclidean distance | `<->` | Căn bậc hai tổng bình phương sai khác từng chiều | Hữu ích khi độ lớn vector mang ý nghĩa; có thể bị ảnh hưởng bởi scale/norm |
| Inner product / Dot product | `<#>` là negative inner product để PostgreSQL sort tăng dần | Tích vô hướng, phụ thuộc cả hướng và độ lớn | Tốt khi model được huấn luyện cho inner product và vector đã chuẩn hóa hoặc norm có ý nghĩa |

Cosine similarity thường phù hợp cho semantic text search vì hai câu cùng ý có thể có độ dài khác nhau nhưng hướng embedding tương tự. Khi vector đã L2-normalize, cosine và dot product cho cùng thứ tự xếp hạng; nếu chưa normalize, dot product có thể ưu tiên vector có norm lớn. L2 giữ cả hướng lẫn độ lớn nên không phải lựa chọn mặc định an toàn cho mọi model embedding.

Trong pgvector cần phân biệt distance và similarity: operator cosine `<=>` trả cosine distance càng nhỏ càng tốt. Spring AI thường chuyển kết quả thành score similarity càng lớn càng tốt; vì vậy code ứng dụng dùng `score > 0.75`. Ngưỡng phải được hiệu chỉnh theo model embedding và dữ liệu thật, không nên bê nguyên giữa các model.

## Minh chứng chạy thực tế

Test `RAGRetrievalServiceTest` dùng mock VectorStore và ChatClient:

```text
Question: Làm thế nào để học Java?
Vector candidates: 2
Relevant documents (score > 0.75): 0
WARN ...RAGRetrievalService : RAG blocked out-of-scope question: threshold=0.75, candidates=2
LLM calls: 0
Response: Xin lỗi, thông tin bạn tìm kiếm không nằm trong tài liệu quy chế của chúng tôi.
BUILD SUCCESSFUL
```

Test còn xác nhận request có `topK=3`, threshold `0.75`, và không có tương tác nào với `ChatClient`.

## Chạy kiểm thử

Project đã có Gradle wrapper riêng:

```powershell
.\gradlew.bat test
```

## Đẩy lên GitHub

Tạo repository riêng cho bài 3 rồi chạy:

```powershell
git init
git add .
git commit -m "Implement defensive RAG retrieval"
git branch -M main
git remote add origin https://github.com/<username>/<repository-bai-3>.git
git push -u origin main
```

Link GitHub cần nộp là URL repository thực tế sau khi push.
