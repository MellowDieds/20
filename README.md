# 🌿 20-20-20 Göz Sağlığı Mola Hatırlatıcısı (Android)

Ekran kullanımına bağlı dijital göz yorgunluğunu (**Computer Vision Syndrome - CVS**) ve göz kuruluğunu azaltmak için geliştirilmiş, **20-20-20 kuralını** nazikçe hatırlatan ve eğitici animasyonlarla göz egzersizlerini öğreten modern Android uygulaması.

---

## 📱 Temel Özellikler

- **Akıllı Ekran Takibi (Foreground Service):** Telefon ekranı açıkken (`ACTION_SCREEN_ON`) sessizce aktif ekran süresini sayar, ekran kapandığında (`ACTION_SCREEN_OFF`) otomatik olarak duraklar.
- **Sıfır Rahatsızlık İlkesi:** Sayım süresince kullanıcıyı ses, titreşim veya tam ekran kapatıcı pencerelerle bölmez.
- **Nazik Üst Bildirim (Heads-Up Alert):** 20 dakika dolduğunda ekranın üst kısmında zarif bir mola uyarısı belirir:
  - **Ertele (5 Dk):** Molayı 5 dakika erteler (sürekli ertelemeyi önlemek için sınırlandırılabilir).
  - **Göster / Yap:** Doğrudan animasyonlu açıklama ve egzersiz rehberini açar.
- **Eğitici Animasyonlu Rehber:**
  1. *Uzağa Odaklanma (20 Sn):* Siliyer göz kaslarını gevşetmek için 6 metre (20 feet) uzağa odaklanma simülasyonu.
  2. *Bilinçli Göz Kırpma (10 Tekrar):* Gözyaşı tabakasını tazeleyen ritmik animasyon.
  3. *Kas Esnetme:* Sonsuzluk döngüsünde hareket eden odak noktasını gözle takip etme.
  4. *Avuç İçiyle Isıtma (Palming):* Derin nefes ve göz rahatlatma egzersizi.
- **⚡ Hızlı Test Modu (20 Saniye):** 20 dakika beklemeden akışı ve bildirimleri saniyeler içinde test etme imkânı.
- **İstatistik & Kayıt (Room Veritabanı):** Günlük mola sayısı, toplam aktif ekran süresi ve göz rahatlama skoru takibi.

---

## 🔬 Bilimsel ve Tıbbi Dayanak

Uygulama, **Amerikan Oftalmoloji Akademisi (AAO)** ve **Amerikan Optometri Derneği (AOA)** tarafından kabul gören *20-20-20 protokolünü* temel alır:

1. **Talens-Estarelles et al. (2023) - *Contact Lens and Anterior Eye* (PubMed ID: 36725458):** Yazılımsal hatırlatıcılarla 20-20-20 kuralının uygulanmasının dijital göz yorgunluğu ve göz kuruluğu semptomlarını anlamlı ölçüde azalttığı kanıtlanmıştır.
2. **Tsubota & Nakamori (1993) / Rosenfield (2011) - *NEJM* & *Ophthalmic & Physiological Optics*:** Ekran karşısında göz kırpma sıklığının %66'ya varan oranda azaldığı ve bilinçli kırpmanın kornea yüzeyini koruduğu saptanmıştır.

---

## 🛠️ Teknik Altyapı

- **Dil:** Kotlin
- **Arayüz:** Jetpack Compose & Material Design 3 (M3)
- **Mimari:** MVVM & Clean Architecture
- **Veritabanı:** Room Database + KSP
- **Arka Plan:** Android Foreground Service (`specialUse`) + Dinamik `BroadcastReceiver`
- **Minimum SDK:** Android 7.0 (API 24)
- **Hedef SDK:** Android 15 / 16 (API 36)

---

## 🚀 GitHub'dan Kurulum ve Derleme (Build & Install)

### 1. Depoyu Klonlayın
```bash
git clone https://github.com/KULLANICI_ADINIZ/goz-sagligi-20-20-20.git
cd goz-sagligi-20-20-20
```

### 2. APK Dosyasını Oluşturun
Bilgisayarınızda Android Studio veya terminal üzerinden:
```bash
# Debug APK oluşturmak için:
./gradlew assembleDebug
```
Oluşturulan APK dosyası şu dizinde yer alır:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Telefona Kurulum
- **Yöntem A (USB ile / ADB):**
  ```bash
  adb install app/build/outputs/apk/debug/app-debug.apk
  ```
- **Yöntem B (Doğrudan Telefona Atma):**
  `app-debug.apk` dosyasını Google Drive, WhatsApp veya USB kablosuyla telefonunuza gönderip üzerine dokunarak yükleyebilirsiniz (Gerektiğinde "Bilinmeyen kaynaklardan yükleme" iznini açın).
