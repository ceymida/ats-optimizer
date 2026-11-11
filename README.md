# Smart Resume & ATS Optimizer

Özgeçmişinizi (PDF / JSON / TXT) hedef iş ilanıyla karşılaştırıp **ATS uyum skorunu** hesaplayan,
**eksik anahtar kelimeleri** ve **iyileştirme önerilerini** gösteren full-stack web aracı.

- **Backend:** Java 21, Spring Boot 3.4, Apache PDFBox, H2 (in-memory), OpenAI (opsiyonel)
- **Frontend:** Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4

## Özellikler

- PDF, JSON (JSON Resume dahil) veya düz metin özgeçmiş yükleme; alternatif olarak metin yapıştırma
- İş ilanından teknoloji/anahtar kelime çıkarımı (Türkçe + İngilizce eş anlamlılarla)
- 3 boyutlu skorlama: anahtar kelime uyumu (%60), format/yapı (%25), okunabilirlik (%15)
- Eksik terimleri önem ağırlığına göre sıralama (● yüksek öncelik, ○ destekleyici)
- Format kontrolleri: iletişim bilgisi, bölüm başlıkları, madde işaretleri, eylem fiilleri, tarih,
  ölçülebilir başarı, ideal uzunluk
- OpenAI API anahtarı tanımlıysa özgeçmişe özel AI önerileri; tanımlı değilse kural tabanlı öneriler
- Son analizlerin geçmişi (H2 in-memory, uygulama yeniden başlayınca sıfırlanır)

## Mimari

```
frontend (Next.js) --multipart/form-data--> backend (Spring Boot)
                                              ├── ResumeTextExtractor (PDFBox / Jackson)
                                              ├── KeywordExtractor
                                              ├── AtsScoringService
                                              ├── OpenAiSuggestionClient (opsiyonel)
                                              └── AnalysisRecordRepository (H2)
```

## Gereksinimler

- Java 21+ (`java -version`)
- Maven 3.9+
- Node.js 20+ ve npm

## VS Code'da tek tıkla çalıştırma

Klasörü VS Code'da açın (`File > Open Folder` → `ats-optimizer`). Önerilen eklentiler
kurulduktan sonra:

- **Tüm projeyi başlat:** `Cmd+Shift+B` (varsayılan build görevi) veya
  `Terminal > Run Task...` → **Start ATS (backend + frontend)**. Backend, frontend ve
  `npm install` otomatik başlar; iki ayrı terminalde çalışır.
- **Debug ile başlat:** `Cmd+Shift+D` → **ATS: Start All (Debug Backend + Frontend)** →
  yeşil oynat. Backend'de breakpoint koyabilirsiniz.
- **Durdur:** `Terminal > Run Task...` → **Stop ATS (free ports 8080 & 3000)** veya
  terminalde `Ctrl+C`.

Ardından tarayıcıda `http://localhost:3000` adresini açın.

## Çalıştırma (manuel)

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

Backend `http://localhost:8080` üzerinde çalışır.

OpenAI önerilerini etkinleştirmek için (opsiyonel):

```bash
export OPENAI_API_KEY="sk-..."
# farklı model: export OPENAI_MODEL="gpt-4o-mini"
mvn spring-boot:run
```

Anahtar yoksa uygulama sorunsuz çalışır, kural tabanlı öneriler döner.

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Arayüz: `http://localhost:3000`

Farklı bir backend adresi kullanacaksanız `frontend/.env.local` oluşturun:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Canlıya alma

### Backend (Render / Railway / Fly.io)

Vercel yalnızca Next.js çalıştırır; Spring Boot backend ayrı bir platformda barındırılmalıdır.
Repo içindeki `backend/Dockerfile` ile Docker destekli her platforma deploy edebilirsiniz.

1. Backend'i deploy edin (ör. Render → New Web Service → repo + `backend` root, Docker).
2. Ortam değişkenlerini tanımlayın:

| Değişken | Örnek | Açıklama |
| --- | --- | --- |
| `APP_CORS_ALLOWED_ORIGINS` | `https://ats-optimizer.vercel.app,http://localhost:3000` | Tam origin listesi (virgülle) |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | `https://*.vercel.app` | Preview deploy'ları için joker |
| `OPENAI_API_KEY` | `sk-...` | Opsiyonel; yoksa kural tabanlı öneriler |
| `PORT` | platformun verdiği değer | `application.yml` otomatik okur |

3. `https://<backend-adresiniz>/api/health` → `ok` döndüğünü doğrulayın.

### Frontend (Vercel)

1. Vercel → Add New Project → GitHub reposunu içe aktarın.
2. **Root Directory** olarak `frontend` seçin (framework otomatik Next.js algılanır).
3. Environment Variables → `NEXT_PUBLIC_API_URL=https://<backend-adresiniz>` (sonunda `/` yok).
4. Deploy sonrası `APP_CORS_ALLOWED_ORIGINS` değişkenine Vercel canlı URL'sini ekleyip backend'i
   yeniden başlatın. CORS hataları genellikle bu adımın atlanmasından kaynaklanır.
5. `NEXT_PUBLIC_*` değişkenleri build sırasında gömülür; değiştirince yeniden deploy edin.

## API

| Metot | Yol | Açıklama |
| --- | --- | --- |
| `POST` | `/api/analyze` | Multipart: `file` (opsiyonel), `resumeText` (opsiyonel), `jobDescription` (zorunlu) |
| `GET` | `/api/analyses?limit=10` | Son analizler |
| `GET` | `/api/health` | Sağlık kontrolü |

Örnek:

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "file=@samples/ornek-ozgecmis.json" \
  -F "jobDescription=< samples/ornek-ilan.txt"
```

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "resumeText=Java, Spring Boot, PostgreSQL, Docker deneyimim var." \
  -F "jobDescription=Java Spring Boot Kafka Kubernetes arıyoruz."
```

> `samples/` altında örnek özgeçmiş (JSON/TXT) ve iş ilanı bulunur. PDF test etmek için
> TXT dosyasını herhangi bir araçla PDF'e dönüştürebilirsiniz.

## Skorlama

| Bileşen | Ağırlık | İçerik |
| --- | --- | --- |
| Anahtar kelime | %60 | İlan terimlerinin ağırlıklı eşleşme oranı (teknoloji terimleri 3.0, sık geçen terimler 1.0–2.0) |
| Format | %25 | 12 kontrol: e-posta, telefon, LinkedIn, bölümler, maddeler, eylem fiilleri, tarih, ölçülebilir başarı, uzunluk |
| Okunabilirlik | %15 | Ortalama cümle uzunluğu ve kelime sayısı |

Not: Skor bir uyum göstergesidir; gerçek ATS motorları firmaya göre farklı davranabilir.

## Testler

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run lint
npm run build
```

## Sınırlamalar

- H2 in-memory kullanıldığı için geçmiş kayıtları yeniden başlatmada silinir (PostgreSQL'e geçirilebilir).
- Taranmış (görüntü) PDF'ler için OCR yoktur; metin tabanlı PDF gerekir.
- Dosya sınırı 10 MB, PDF en fazla 20 sayfa.
- AI önerileri yalnızca `OPENAI_API_KEY` tanımlıysa devreye girer.

## Olası Geliştirmeler

- PostgreSQL + kullanıcı hesapları, analiz geçmişi ve karşılaştırma
- OCR (Tesseract) ile taranmış PDF desteği
- İş ilanı URL'sinden otomatik metin çıkarma
- Özgeçmiş düzenleme önerilerini doğrudan PDF/DOCX olarak indirme
- Çoklu ilan karşılaştırma ve başvuru takibi
