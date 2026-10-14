# JARVIS Android — Türkçe telefon AI çekirdeği

Bu proje, JARVIS'i basit bir sohbet ekranı yerine Android üzerinde çalışan modüler bir telefon-asistan çekirdeği olarak başlatır. Uygulamanın dili, durumları, komut geri bildirimleri ve arayüzü Türkçedir.

## Şu an çalışan temel

- Holografik, dikey telefon HUD arayüzü; ilk açılışta JARVIS CORE başlatma dizisi.
- Ana ekran, komut merkezi, sistem, araçlar, güvenlik, yerel hafıza ve ayarlar ekranları.
- Türkçe, açıklanabilir komut ayrıştırma ve çok adımlı eylem planı:
  - Wi-Fi'yi aç, Bluetooth'u aç, sesi yüzde 30'a getir ve müziği başlat.
  - YouTube'u aç.
  - Feneri aç.
  - Telefonumu kontrol et.
  - Alarm kur: 07:00
  - Bunu hatırla: ...
  - Bunu araştır.
- Android'in resmî niyetleri/API'leri üzerinden uygulama açma, sistem ayarına yönlendirme, medya sesi ayarlama, fener, kamera, telefon, dosya seçici, alarm taslağı, takvim taslağı, SMS taslağı ve web araması.
- Kullanıcı tetiklemeli Türkçe konuşma tanıma ve metinden sese yanıt.
- RAM, depolama, pil, batarya sıcaklığı, ağ taşımaları, Bluetooth, NFC, VPN ve görülebilen uygulama sayısı için yerel cihaz raporu.
- Yerel konuşma günlüğü, açıkça kaydedilmiş hafıza notları ve tercihler.
- Komut yorumlayıcısı için birim testleri.

## Mimari

~~~text
UI / HUD
   ↓
JarvisViewModel
   ↓
JarvisCore
   ├── TurkishCommandParser       → niyet ve çok adımlı plan
   ├── AndroidActionGateway       → resmî Android eylemleri / sistem panelleri
   ├── DeviceMonitor              → doğrulanabilir cihaz ölçümleri
   └── JarvisMemoryStore          → yerel konuşma, hafıza ve tercih verisi
~~~

JarvisAction tipleri, AI katmanının serbest metinle doğrudan cihaz kontrolü yapmasını engeller. Genişletilecek her AI sağlayıcısı, önce bu güvenli ve denetlenebilir eylem planına dönmek zorundadır.

## Android sınırları — kasıtlı ve görünür

Android normal bir uygulamaya telefonun tamamı üzerinde sınırsız erişim vermez. Bu proje izin veya güvenlik denetimini atlamaz.

| Alan | JARVIS davranışı |
| --- | --- |
| Wi‑Fi, Bluetooth, NFC, VPN, güvenlik ayarları | İlgili Android sistem panelini açar; değişikliği kullanıcı onaylar. |
| Uygulama açma | Görülebilen ve başlatılabilir yüklü uygulamayı açar. Android 11+ paket görünürlüğü tüm uygulama listesini sınırlayabilir. |
| Fener | Kamera izni verildiyse doğrudan açıp kapatır. |
| Ses | Medya sesini doğrudan ayarlar. |
| Kamera, arama, SMS, alarm, takvim | Güvenli taslak/uygulama akışı açar. Arama yapma veya mesaj gönderme son kullanıcı onayı olmadan gerçekleştirilmez. |
| Bildirim özeti | Bildirim erişimi, kullanıcının Android ayarlarında açıkça etkinleştirmesini gerektirir. Bu başlangıç sürümü o resmî paneli açar; Notification Listener modülü henüz ekli değildir. |
| CPU/GPU yüzdesi ve üretici termal verisi | Standart Android API'sinde her cihazda güvenilir biçimde erişilebilir değildir. Uygulama tahmin üretmez ve erişilemeyen metriği açıkça gösterir. |

##enüz eklenmesi gereken modüller

Bu kaynak ağacı üretim mimarisinin başlangıcıdır; aşağıdakiler için ayrı cihaz/sunucu entegrasyonu gerekir:

1. Sürekli JARVIS uyandırma kelimesi: cihaz üstü keyword modeli, foreground service, kalıcı bildirim ve pil optimizasyon istisnası gerekir.
2. Gerçek yerel LLM: cihaz modeline uygun bir ONNX / MediaPipe / llama.cpp sağlayıcısı ve model indirme yöneticisi gerekir.
3. Çevrimiçi AI: kullanıcı tarafından getirilen, Android Keystore'da saklanan sağlayıcı anahtarı ve açık gizlilik ekranı gerekir. Anahtar kaynak koda eklenmez.
4. Araştırma motoru: izinli web sağlayıcısı, kaynak/atıf modeli, ağ hatası ve maliyet kontrolleri gerekir.
5. Bildirim, dosya indeksi, kişi ve takvim modülleri: kullanıcı izni, Android sürüm farklılıkları ve veri minimizasyonu ile ayrı modüller şeklinde tamamlanmalıdır.
6. Otomasyon: tetikleyici → koşul → plan → kullanıcı onayı/işlem → günlük motoru için WorkManager/AlarmManager tabanlı zamanlama, sınırları açık eylem politikası ve denetlenebilir kayıt gerekir.
7. Güncelleme/kod motoru: uygulamanın kendi kendine kaynak kodunu değiştirip yüklemesi Android/mağaza güvenlik modeliyle uyumlu değildir. Bunun yerine imzalı sürüm, test, değişiklik günlüğü ve açık kullanıcı onayıyla normal güncelleme süreci kurulmalıdır.

## Android Studio ile derleme

1. Android Studio'nun güncel kararlı sürümünü ve gömülü JDK 17+ ortamını kullanın.
2. Bu klasörü Android Studio ile açın.
3. İlk Gradle eşitlemesine izin verin; compileSdk 35 platformunu kurun.
4. Bir Android cihazı veya emülatör seçin.
5. Run ile debug yapısını yükleyin ya da Build > Build APK(s) ile APK üretin.
6. Mikrofon veya Kamera gibi işlevleri ilk kullandığınızda Android izin iletişim kutusundan açıkça onaylayın.

## Kontrol

Bu çalışma ortamında Android SDK, Gradle ve JDK 17+ bulunmadığı için APK derlemesi başlatılamadı. Kaynak yapılandırması Android Studio'nun güncel derleme zinciri için hazırlanmıştır; çalışma alanında Java 8 bulunduğundan yerel derleme sonucu veya APK varmış gibi gösterilmemektedir.
