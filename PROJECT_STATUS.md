# ORBITA: FLOW ARENA - Üretim Durumu

## MILESTONE_01: CORE VERTICAL SLICE
Tarih: 12 Temmuz 2026
Durum: Aktif Geliştirme (v0.1)

### IMPLEMENTED (Gerçekten Çalışanlar)
- **Godot 4 Altyapısı:** Sıfırdan `orbita-godot` projesi oluşturuldu (`main.tscn`, `project.godot`).
- **Core State Machine:** `orbita_app.gd` üzerinden Menu -> Game -> Result ekranları arası geçiş.
- **Core Gameplay (game_world.gd):**
  - Çekirdek dönüşü (matematiksel rotasyon).
  - Pin (iğne) fırlatma ve çarpışma tespiti (fizik motoru olmadan, saf açısal matematik ile optimize).
- **Pulse Window:** Amber renkli hedef penceresi, zamanlama algoritması ve `PERFECT PULSE` takibi.
- **Risk x2 Sistemi:** Risk modu açıldığında iğne/çekirdek rengi kızarır (kızıl/red), çarpışma alanı (safe_dist) daralır (zorlaşır).
- **Chain Geometry:** İğneler 3'ten fazla olduğunda aralarında turkuaz renkli geometrik kalkan çizgileri çizilir (Anadolu Bilimkurgu estetiği).
- **Level Engine & JSON:** `levels.json` üzerinden 10 level okuyan sistem. Dönüş hızı, ters akış (reverse timer) ve iğne limitleri buradan yönetilir.
- **Ters Akış:** Bölüm 3 ve sonrasında, belirli saniyelerde çekirdek yön değiştirir.

### MOCK (Şu an Geçici Arayüz Olanlar)
- **Kozmetik Arayüzler:** Ana menü, Dashboard ve Result ekranları Godot'nun native kod UI'ları ile (Control, Label, Button) çizildi. Henüz final PNG UI assetleri uygulanmadı.
- **Joker Butonu:** Ekranda görünüyor ancak mekanik bağlanmadı (henüz sadece buton event'i hazır).
- **Görsel Temalar:** PNG kullanılmadı. Tüm oyun `_draw()` fonksiyonu ile "Turkuaz enerji, Lacivert arka plan, Amber pulse" kurallarına göre gerçek zamanlı geometrik render ediliyor.

### PENDING (Bekleyenler)
- **Ses ve Titreşim:** `AudioStreamPlayer` eklenecek, SFX dosyaları aranacak.
- **Android Build & APK:** `/tmp` disk limiti nedeniyle (sadece 1GB RAM disk) dev boyutlu Android SDK, Gradle ve Godot template indirmeleri şimdilik engelleniyor. Core gameplay testleri stabil olunca çözüm üretilecek.
- **Analytics & Backend:** Tamamen kapalı, local save stub hazır.

### BLOCKERS (Engeller)
- Arena Sandbox üzerindeki `/tmp` alanının RAM disk (1GB) olması, tam donanımlı Android derleme ortamının kurulmasını fiziksel olarak kısıtlıyor. Proje GitHub üzerinden kod olarak %100 oynanabilir şekilde teslim edilecek. Masaüstü HTML/Linux build alternatifleri denenebilir.
