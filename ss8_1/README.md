# Bài 1 - CRM Document Ingestion vào Supabase pgvector

Project độc lập triển khai ETL pipeline đọc tài liệu Markdown, chia chunk bằng `TokenTextSplitter` và nạp embedding vào Supabase pgvector qua Spring AI.

## Cấu trúc chính

- `DocumentIngestionService`: pipeline đọc, gắn metadata, chia chunk và ghi `VectorStore`.
- `DocumentIngestionController`: endpoint `POST /api/documents/ingest`.
- `src/main/resources/documents/quy-trinh-doi-tra.md`: tài liệu mẫu.
- `DocumentIngestionServiceTest`: test không cần Supabase hoặc Ollama thật.

## Tham số xử lý

`TokenTextSplitter` được cấu hình:

- `chunkSize = 600`: kích thước mục tiêu của mỗi chunk theo token.
- `minChunkSizeChars = 120`: chunk nhỏ hơn 120 ký tự sẽ được gom với phần kế tiếp khi có thể.
- `maxTokens = 10000`: trong API Spring AI 2.0, tham số tương ứng là `withMaxNumChunks(10000)`, giới hạn số chunk tối đa được tạo trong một lần split.

Mỗi chunk giữ metadata `category` và `source_file`, giúp lọc hoặc truy vết nguồn khi truy vấn RAG.

## Vì sao maximum-pool-size là 4?

Supabase Free Tier có giới hạn tài nguyên và số kết nối PostgreSQL đồng thời. HikariCP có thể mở nhiều connection theo số request; nếu đặt pool quá lớn, ứng dụng dễ chiếm hết connection khả dụng, gây lỗi `too many connections`, timeout hoặc ảnh hưởng các dịch vụ khác. Pool size 4 là mức thận trọng cho workload ingestion nhỏ, đồng thời `minimum-idle: 2` giữ chi phí tài nguyên thấp. Đây là giới hạn vận hành bảo thủ, không thay thế việc kiểm tra quota thực tế của project Supabase.

Khi dùng Supavisor pooler, nên dùng đúng port/chế độ được Supabase hướng dẫn. Không nên tăng pool chỉ để bù cho truy vấn chậm; hãy tối ưu truy vấn hoặc batch trước.

## Tác động của minChunkSizeChars đến RAG

Giá trị 120 loại bớt các mẩu quá ngắn, vốn thường thiếu ngữ cảnh và tạo embedding kém ổn định. Chunk đủ dài thường giúp truy vấn semantic có tín hiệu tốt hơn và giảm kết quả rời rạc.

Nếu đặt quá cao, các đoạn ngắn nhưng có ý nghĩa như điều kiện, mã lỗi hoặc câu trả lời ngắn có thể bị nhập vào đoạn khác, làm giảm độ chính xác của metadata/ngữ cảnh. Nếu đặt quá thấp, hệ thống tạo nhiều vector nhỏ, tăng chi phí embedding và dễ trả về kết quả thiếu ngữ cảnh. Giá trị 120 phù hợp để bắt đầu, nhưng nên đánh giá bằng bộ câu hỏi CRM thực tế.

## Cấu hình môi trường

Không commit mật khẩu Supabase hoặc API key. Có thể đặt các biến:

```powershell
$env:SUPABASE_JDBC_URL = "jdbc:postgresql://<project>.pooler.supabase.com:6543/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME = "postgres.<project-id>"
$env:SUPABASE_DB_PASSWORD = "<password>"
$env:OLLAMA_BASE_URL = "http://localhost:11434"
```

Chạy Ollama và tải model embedding:

```powershell
ollama pull nomic-embed-text
.\gradlew.bat bootRun
```

Gửi tài liệu:

```powershell
curl.exe -X POST "http://localhost:8080/api/documents/ingest" -F "file=@src/main/resources/documents/quy-trinh-doi-tra.md" -F "category=customer-care"
```

## Minh chứng console khi nạp thành công

Với database Supabase và Ollama đã kết nối, log tương ứng có dạng:

```text
INFO  ...DocumentIngestionService : CRM document ingested successfully: sourceFile=quy-trinh-doi-tra.md, category=customer-care, chunks=1
```

Response mẫu:

```json
{"status":"success","chunks":1}
```

## Kiểm thử

Từ thư mục `ss8_1`:

```powershell
.\gradlew.bat test
```

Test unit dùng mock `VectorStore`, vì vậy không cần credential cloud.

## Đẩy lên GitHub

Tạo repository rỗng trên GitHub, sau đó chạy trong thư mục này:

```powershell
git init
git add .
git commit -m "Implement CRM Markdown ingestion pipeline"
git branch -M main
git remote add origin https://github.com/<username>/<repository>.git
git push -u origin main
```
