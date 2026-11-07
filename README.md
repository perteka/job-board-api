# Job Board & Application API

Java 17 ve Spring Boot 3.5.7 ile geliştirilmiş, JWT tabanlı rol yönetimine sahip bir iş ilanı ve başvuru yönetim REST API'si. İşverenler firma oluşturup ilan yayınlayabilir, adaylar CV yükleyip başvurabilir; başvurular `APPLIED → REVIEWED → HIRED / REJECTED` akışıyla yönetilir.

## Öne Çıkan Özellikler

- **JWT kimlik doğrulama:** Access token + yenileme tokenı, BCrypt şifreleme ve stateless oturum.
- **Rol bazlı yetkilendirme:** `CANDIDATE`, `EMPLOYER` ve `ADMIN` rolleri; kayıt sırasında `ADMIN` oluşturulamaz.
- **Firma ve ilan yönetimi:** İşveren başına tek firma; taslak, yayında, kapalı ve arşiv durumları.
- **Başvuru akışı:** Aynı ilana ikinci başvuru engeli, katı durum geçiş kuralları ve sahiplik kontrolleri.
- **Arama ve sayfalama:** Başlık/açıklamada anahtar kelime, konumda şehir filtresi; tüm listeler sayfalanır.
- **CV depolama:** MinIO üzerinde PDF, DOC ve DOCX; 5 MB boyut sınırı ve imzalı indirme bağlantısı.
- **E-posta bildirimleri:** Başvuru alındığında ve durum değiştiğinde asenkron e-posta; yerelde Mailpit ile görüntülenir.
- **Merkezi hata yönetimi:** Doğrulama, kimlik, yetki ve iş kuralı hataları için tutarlı JSON yanıtı.
- **Veritabanı sürümleme:** Flyway migration ve Hibernate `ddl-auto: validate`.
- **Testler:** Başvuru ve işe alım akışını kapsayan JUnit 5 + Mockito servis testleri.

## Teknoloji Stack

| Katman | Teknoloji |
|---|---|
| Dil / çalışma zamanı | Java 17 |
| Framework | Spring Boot 3.5.7, Spring Web, Spring Data JPA |
| Güvenlik | Spring Security, JJWT 0.12.6, BCrypt |
| Veritabanı | PostgreSQL 16, Flyway |
| Dosya depolama | MinIO (S3 uyumlu) |
| E-posta | Spring Mail, Mailpit (yerel SMTP) |
| Doğrulama | Jakarta Bean Validation |
| Test | JUnit 5, Mockito, AssertJ |
| Derleme | Maven Wrapper 3.9.9 |
| Yardımcı araçlar | Lombok |

## Mimari

```text
İstemci
  │
  ▼
Spring Security / JwtAuthenticationFilter
  │
  ▼
Controller → Service → Repository → PostgreSQL
               │
               ├── MinioClient → MinIO
               └── MailService (async) → SMTP / Mailpit
```

- Controller yalnızca HTTP katmanını yönetir.
- İş kuralları ve transaction sınırları Service katmanındadır.
- Entity'ler doğrudan dışarı açılmaz; Request/Response DTO'ları ve manuel mapper'lar kullanılır.
- Hatalar `ApiException` alt sınıfları ve `GlobalExceptionHandler` ile tek noktadan üretilir.

## Hızlı Başlangıç

### Gereksinimler

- JDK 17
- Docker Desktop (PostgreSQL, MinIO ve Mailpit için)
- Maven zorunlu değildir; proje Maven Wrapper içerir. Alternatif olarak Maven 3.9+ kullanılabilir.

### 1. Projeyi aç

```bash
cd job-board-api
```

### 2. Yerel servisleri başlat

```bash
docker compose up -d
docker compose ps
```

> `docker-compose.yml` artık varsayılan şifre içermez; önce `.env.example` dosyasını `.env` olarak kopyalayıp değerleri doldurun:
>
> ```bash
> cp .env.example .env
> ```

İlk açılışta imajlar indirileceği için PostgreSQL'in `healthy` olması biraz sürebilir.

| Servis | Adres | Yerel giriş |
|---|---|---|
| PostgreSQL | `localhost:5432` (yalnızca `127.0.0.1`) | `.env` içindeki `POSTGRES_*` değerleri |
| MinIO API | `http://localhost:9000` (yalnızca `127.0.0.1`) | `.env` içindeki `MINIO_ROOT_*` değerleri |
| MinIO Konsolu | `http://localhost:9001` (yalnızca `127.0.0.1`) | `.env` içindeki `MINIO_ROOT_*` değerleri |
| Mailpit SMTP | `localhost:1025` (yalnızca `127.0.0.1`) | — |
| Mailpit Arayüzü | `http://localhost:8025` (yalnızca `127.0.0.1`) | — |
| Uygulama | `http://localhost:8080` | — |

### 3. Uygulamayı başlat

Maven Wrapper ile:

```bash
# macOS / Linux / Git Bash
./mvnw spring-boot:run

# Windows CMD / PowerShell
mvnw.cmd spring-boot:run
```

Maven kuruluysa `mvn spring-boot:run` de kullanılabilir. IntelliJ IDEA'dan `JobBoardApiApplication` sınıfı da çalıştırılabilir.

İlk açılışta Flyway tabloları oluşturur; `DataInitializer` altı kategori ve on dört yetenek ekler. Uygulama varsayılan olarak `8080` portunu dinler.

### 4. Çalıştığını doğrula

```bash
curl http://localhost:8080/api/categories
```

`200 OK` ve kategori listesi dönmelidir. API için Swagger/OpenAPI uç noktası bulunmuyor; tüm uç noktalar bu README'de ve Postman koleksiyonunda listelenmiştir.

## Yapılandırma

Varsayılan değerler `src/main/resources/application.yml` içinde yerel geliştirme için hazırdır. Aşağıdaki ortam değişkenleriyle geçersiz kılınabilir:

| Değişken | Varsayılan | Açıklama |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobboard` | JDBC bağlantı adresi |
| `DB_USERNAME` | `jobboard` | Veritabanı kullanıcısı |
| `DB_PASSWORD` | **zorunlu** | Veritabanı şifresi |
| `JWT_SECRET` | **zorunlu** | Access token imzalama anahtarı (en az 32 karakter) |
| `JWT_REFRESH_SECRET` | `JWT_SECRET` değeri | Refresh token imzalama anahtarı; ayrı secret önerilir |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO API adresi |
| `MINIO_ACCESS_KEY` | **zorunlu** | MinIO erişim anahtarı |
| `MINIO_SECRET_KEY` | **zorunlu** | MinIO gizli anahtarı |
| `MINIO_BUCKET` | `job-board-cvs` | CV kovası; yoksa otomatik oluşturulur |
| `MAIL_HOST` | `localhost` | SMTP sunucusu |
| `MAIL_PORT` | `1025` | SMTP portu |
| `MAIL_ENABLED` | `true` | E-posta gönderimini açar/kapatır |
| `WEB_BASE_URL` | `http://localhost:3000` | Doğrulama/şifre sıfırlama e-postalarındaki link tabanı |
| `EMAIL_VERIFICATION_ENABLED` | `true` | E-posta doğrulama akışını açar/kapatır |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:4200` | Virgülle ayrılmış izinli origin listesi |
| `FORWARD_HEADERS_STRATEGY` | `none` | Reverse proxy arkasında `framework` yapılabilir |

`.env.example` yalnızca değerlerin referansıdır; Spring Boot `.env` dosyasını kendiliğinden okumaz. Değişkenleri IDE çalıştırma yapılandırmasından veya terminalden tanımlayın:

```bash
export JWT_SECRET="en-az-32-karakterlik-rastgele-bir-anahtar"
./mvnw spring-boot:run
```

Access token 15 dakika, refresh token 7 gün geçerlidir. `JWT_SECRET`, `JWT_REFRESH_SECRET`, `DB_PASSWORD`, `MINIO_ACCESS_KEY` ve `MINIO_SECRET_KEY` her ortamda zorunludur (refresh secret tanımsızsa `JWT_SECRET` kullanılır); en az 32 karakterlik kriptografik olarak rastgele değerler tanımlayın. Değer tanımlı değilse uygulama başlamaz.

## Kimlik Doğrulama ve Roller

| Rol | Yetkiler |
|---|---|
| `CANDIDATE` | Profil, CV yükleme, ilan arama, başvuru, ilan kaydetme |
| `EMPLOYER` | Firma yönetimi, ilan oluşturma/yayınlama, başvuru değerlendirme |
| `ADMIN` | Rol tanımlıdır ancak public kayıtla oluşturulamaz |

Kayıt veya giriş yanıtındaki `accessToken` korumalı isteklerde şu başlıkla gönderilir:

```http
Authorization: Bearer <accessToken>
```

- `POST /api/auth/refresh` yenileme tokenını döndürür ve eski tokenı iptal eder (rotation).
- `POST /api/auth/logout` kullanıcının tüm yenileme tokenlarını iptal eder.
- Şifreler BCrypt ile saklanır; API hiçbir zaman şifre veya token değerini loglamaz.
- Kayıt sonrası e-posta doğrulaması zorunludur; doğrulanmamış hesap `enabled=false` kalır ve giriş yapamaz.
- Şifre politikası: en az 10 karakter, harf + rakam zorunluluğu ve yaygın şifre listesi kontrolü.
- Art arda 5 başarısız giriş denemesinde hesap 15 dakika kilitlenir (`app.security.login-*` ile ayarlanır).
- Kimlik doğrulama uç noktalarında IP bazlı rate limiting uygulanır; CV yüklemede kullanıcı başına istek ve depolama kotası vardır.
- Access ve refresh token farklı secret'larla imzalanır ve `iss` claim'i taşır.

## API Uç Noktaları

### Kimlik Doğrulama

| Metot | Yol | Erişim | Açıklama |
|---|---|---|---|
| `POST` | `/api/auth/register` | Herkese açık | Aday veya işveren kaydı; doğrulama maili gönderir |
| `POST` | `/api/auth/verify-email` | Herkese açık | E-posta doğrulama tokenı; başarıda token çifti döner |
| `POST` | `/api/auth/resend-verification` | Herkese açık | Doğrulama mailini yeniden gönderir |
| `POST` | `/api/auth/login` | Herkese açık | Giriş; token çifti döner |
| `POST` | `/api/auth/refresh` | Herkese açık | Refresh token ile yeni token çifti |
| `POST` | `/api/auth/forgot-password` | Herkese açık | Şifre sıfırlama maili gönderir |
| `POST` | `/api/auth/reset-password` | Herkese açık | Sıfırlama tokenı + yeni şifre; token çifti döner |
| `POST` | `/api/auth/change-password` | Giriş gerekli | Mevcut şifreyle yeni şifre belirler; oturumları kapatır |
| `POST` | `/api/auth/logout` | Giriş gerekli | Tüm refresh tokenları iptal eder |

### Genel

| Metot | Yol | Erişim | Açıklama |
|---|---|---|---|
| `GET` | `/api/categories` | Herkese açık | Kategorileri listeler |
| `GET` | `/api/skills` | Herkese açık | Yetenekleri listeler |
| `GET` | `/api/jobs` | Herkese açık | `keyword`, `city` ve sayfalama ile ilan arar |
| `GET` | `/api/jobs/{id}` | Herkese açık | Yayındaki ilan detayı; sahibi taslağı da görebilir |
| `GET` | `/api/companies/{id}` | Herkese açık | Firma detayı |

### Aday

| Metot | Yol | Erişim | Açıklama |
|---|---|---|---|
| `GET` | `/api/profile/me` | `CANDIDATE` | Profili getirir |
| `PUT` | `/api/profile/me` | `CANDIDATE` | Profili günceller |
| `POST` | `/api/files/cv` | `CANDIDATE` | PDF/DOC/DOCX CV yükler (multipart `file`) |
| `POST` | `/api/jobs/{jobId}/applications` | `CANDIDATE` | İlana başvurur |
| `GET` | `/api/applications/me` | `CANDIDATE` | Kendi başvurularını listeler |
| `POST` | `/api/jobs/{jobId}/save` | `CANDIDATE` | İlanı kaydeder |
| `DELETE` | `/api/jobs/{jobId}/save` | `CANDIDATE` | Kaydı kaldırır |
| `GET` | `/api/saved-jobs` | `CANDIDATE` | Kaydedilen ilanları listeler |

### İşveren

| Metot | Yol | Erişim | Açıklama |
|---|---|---|---|
| `POST` | `/api/companies` | `EMPLOYER` | Firma oluşturur |
| `GET` | `/api/companies/me` | `EMPLOYER` | Kendi firmasını getirir |
| `PUT` | `/api/companies/me` | `EMPLOYER` | Firmasını günceller |
| `GET` | `/api/jobs/me` | `EMPLOYER` | Kendi ilanlarını listeler |
| `POST` | `/api/jobs` | `EMPLOYER` | Taslak ilan oluşturur |
| `PUT` | `/api/jobs/{id}` | `EMPLOYER` | İlanı günceller |
| `PATCH` | `/api/jobs/{id}/publish` | `EMPLOYER` | İlanı yayınlar |
| `PATCH` | `/api/jobs/{id}/close` | `EMPLOYER` | Yayındaki ilanı kapatır |
| `PATCH` | `/api/jobs/{id}/archive` | `EMPLOYER` | İlanı arşivler |
| `GET` | `/api/jobs/{jobId}/applications` | `EMPLOYER` | İlan başvurularını listeler |
| `PATCH` | `/api/applications/{id}/status` | `EMPLOYER` | Başvuru durumunu değiştirir |

### Dosya

| Metot | Yol | Erişim | Açıklama |
|---|---|---|---|
| `GET` | `/api/files/presign?objectKey=cvs/{userId}/...` | Aday (yalnızca kendi CV'si) | 10 dakikalık imzalı indirme URL'si |
| `GET` | `/api/files/applications/{applicationId}/cv-url` | İlan sahibi işveren | Başvuru CV'si için imzalı URL |

Başarılı yanıtlar: oluşturma `201`, okuma/güncelleme `200`, silme ve çıkış `204`. Sayfalı yanıtlar `content`, `page`, `size`, `totalElements`, `totalPages` ve `last` alanlarını içerir.

## Örnek Akış

### 1. Aday kaydı

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "aday@example.com",
    "password": "Password123!",
    "fullName": "Aday Kullanıcı",
    "role": "CANDIDATE"
  }'
```

### 2. Firma ve ilan (işveren tokenı ile)

```bash
curl -X POST http://localhost:8080/api/companies \
  -H "Authorization: Bearer $EMPLOYER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Örnek Firma","description":"Yazılım şirketi","location":"İstanbul"}'
```

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Authorization: Bearer $EMPLOYER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Java Developer",
    "description": "Spring Boot ile backend geliştirme.",
    "requirements": "Java 17, Spring Boot, SQL",
    "location": "İstanbul",
    "employmentType": "FULL_TIME",
    "workMode": "HYBRID",
    "minSalary": 60000,
    "maxSalary": 90000,
    "categoryId": 1,
    "skillIds": [1, 2, 3],
    "expiresAt": "2027-12-31T23:59:59Z"
  }'
```

```bash
curl -X PATCH http://localhost:8080/api/jobs/1/publish \
  -H "Authorization: Bearer $EMPLOYER_TOKEN"
```

### 3. CV ve başvuru (aday tokenı ile)

```bash
curl -X POST http://localhost:8080/api/files/cv \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -F "file=@cv.pdf;type=application/pdf"
```

Dönen `objectKey` değerini profil veya başvuru `cvUrl` alanına yazın.

```bash
curl -X POST http://localhost:8080/api/jobs/1/applications \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"coverLetter":"Pozisyonla ilgileniyorum."}'
```

### 4. Değerlendirme (işveren tokenı ile)

```bash
curl -X PATCH http://localhost:8080/api/applications/1/status \
  -H "Authorization: Bearer $EMPLOYER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"REVIEWED"}'
```

## İş Kuralları

- Aynı aday aynı ilana yalnızca bir kez başvurabilir; veritabanında da benzersiz kısıt vardır.
- Yalnızca `PUBLISHED` ve süresi dolmamış ilanlara başvurulur.
- Başvuru için adayın profilinde veya istekte geçerli bir CV anahtarı bulunmalıdır.
- Durum geçişleri katıdır:
  - `APPLIED → REVIEWED`
  - `REVIEWED → HIRED` veya `REVIEWED → REJECTED`
  - `HIRED` ve `REJECTED` son durumdur.
- Aynı duruma tekrar geçiş idempotent kabul edilir; e-posta tekrar gönderilmez.
- Firma ve ilan işlemlerinde sahiplik kontrolü yapılır; başka işverenin kaynağına erişim `403` döner.
- İlan `DRAFT` olarak oluşturulur; yayınlama, kapatma ve arşivleme ayrı uç noktalardadır.
- Arama yalnızca yayında ve süresi dolmamış ilanları getirir; `keyword` başlık/açıklamada, `city` konumda aranır.

## Hata Formatı

Tüm hatalar aynı gövdeyle döner:

```json
{
  "timestamp": "2026-11-05T21:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Doğrulama hatası",
  "path": "/api/jobs",
  "validationErrors": {
    "title": "must not be blank"
  }
}
```

| Kod | Anlamı |
|---|---|
| `400` | Geçersiz istek, doğrulama hatası veya geçersiz durum geçişi |
| `401` | Eksik/geçersiz access token veya hatalı giriş |
| `403` | Rol veya kaynak sahipliği yetersiz |
| `404` | Kaynak bulunamadı |
| `409` | Çakışma (ör. e-posta ya da başvuru zaten mevcut) |
| `500` | Beklenmeyen sunucu hatası |

## Testler

```bash
./mvnw test
```

Windows:

```text
mvnw.cmd test
```

Test suite'i Mockito/JUnit ile veritabanı gerektirmeden servis kurallarını, JWT/refresh token davranışlarını, dosya erişim doğrulamasını, mail bildirimlerini ve temel hata yanıtlarını doğrular. `ApplicationServiceTest` şu senaryoları kapsar:

1. Başarılı başvuru ve bildirim.
2. Aynı ilana ikinci başvurunun reddi.
3. Yayında olmayan ilana başvurunun reddi.
4. `APPLIED → REVIEWED` geçişi ve bildirim.
5. `APPLIED → HIRED` geçersiz geçişinin reddi.
6. İlan sahibi olmayan işverenin başvuruyu değerlendirememesi.
7. Başvuru e-postasının yalnızca transaction commit'inden sonra gönderilmesi.

Şirket, ilan, profil, kaydedilen ilanlar, kimlik doğrulama, refresh token, taksonomi ve dosya depolama davranışları da ayrı test sınıflarında kapsanır. Bu testler PostgreSQL/MinIO entegrasyon testi değildir.

## Postman

Hazır koleksiyon: `postman/job-board-api.postman_collection.json`

1. Postman'de **Import** ile koleksiyonu içe aktarın.
2. `baseUrl` değişkeninin `http://localhost:8080` olduğunu kontrol edin.
3. Sırasıyla **01 - Auth**, **02 - Public**, **03 - Employer Setup**, **04 - Candidate Flow**, **05 - Employer Review** klasörlerini çalıştırın; oturum testleri için **06 - Session** isteğe bağlıdır.
4. CV yükleme isteğinde kendi PDF/DOC/DOCX dosyanızı seçin.
5. Tokenlar ve oluşturulan `jobId` / `applicationId` değişkenleri test scriptleriyle otomatik kaydedilir.

Aynı e-postayla tekrar kayıt artık `409` değil, kullanıcı enumeration'ı önleyen genel bir `201` yanıtı döner; hesap doğrulanmamışsa doğrulama maili yeniden gönderilir. Giriş için e-posta doğrulaması gereklidir (`EMAIL_VERIFICATION_ENABLED=true` iken); Mailpit arayüzünden (`http://localhost:8025`) gelen linkteki token'ı `verify-email` isteğine yazın.

## Proje Yapısı

```text
job-board-api/
├── .mvn/wrapper/                 # Maven Wrapper yapılandırması
├── mvnw, mvnw.cmd                # Maven Wrapper betikleri
├── docker-compose.yml            # PostgreSQL + MinIO + Mailpit
├── .gitattributes                # Wrapper satır sonları
├── .env.example                  # Ortam değişkeni referansı
├── postman/                      # Postman koleksiyonu
├── src/main/java/com/jobboard/api/
│   ├── config/                   # Security, async, MinIO, seed verisi
│   ├── controller/               # REST uç noktaları
│   ├── dto/                      # Request ve Response kayıtları
│   ├── entity/                   # JPA entity'leri ve enum'lar
│   ├── exception/                # İş hataları ve merkezi handler
│   ├── mapper/                   # Entity ↔ DTO dönüşümleri
│   ├── repository/               # Spring Data JPA arayüzleri
│   ├── security/                 # JWT filtreleri ve user details
│   └── service/                  # İş kuralları ve transaction'lar
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/            # V1–V5 Flyway migration'ları
└── src/test/java/com/jobboard/api/service/ApplicationServiceTest.java
```

## Güvenlik Notları

- Üretimde `JWT_SECRET`, veritabanı ve MinIO kimlik bilgileri ortam değişkenlerinden verilmelidir.
- Depoya gerçek `.env`, anahtar veya sertifika eklenmemelidir; `.env` `.gitignore` içindedir.
- CV yüklemesinde hem uzantı hem `Content-Type` hem de dosya imzası (magic bytes) kontrol edilir; dosya adı temizlenir ve kullanıcı başına dosya sayısı/toplam boyut kotası uygulanır.
- MinIO kovası varsayılan olarak private'tır; erişim 10 dakikalık imzalı URL ile sağlanır.
- Public ilan ve firma uç noktaları yalnızca okuma amaçlıdır; tüm yazma işlemleri kimlik doğrulaması ve rol kontrolü gerektirir.
- Auth uç noktalarında rate limiting, hesap kilidi ve güvenlik olayları için audit logları bulunur; response'larda CSP, Referrer-Policy ve Permissions-Policy header'ları gönderilir.

## Yerel Servisleri Durdurma

```bash
docker compose down
```

Veritabanı ve MinIO verilerini de silmek için:

```bash
docker compose down -v
```

> `-v` kalıcı volume'ları siler; tüm yerel kullanıcı, ilan ve dosya verileri kaybolur.

## Öğretici Materyal

Bu depo çalışan API koduna odaklanır. Adım adım ders notları ve diyagramlar, bu klasörün dışındaki `ogretici-materyal/` dizininde tutulur.
