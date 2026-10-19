# ClipDock
Kotlin tabanlı, sol kenara yapışan modern clipboard geçmişi uygulaması.

## Derleme
GitHub deposuna tüm dosyaları yükleyin. Actions > Build ClipDock APK > Run workflow. Artifacts bölümünden APK'yı alın.

## Kullanım
Uygulamayı açın, ekran üstünde gösterme izni verin ve "Çentiği etkinleştir" seçeneğini kullanın.
Sol kenardaki mor çentiği sürükleyebilir veya dokunup geçmişi açabilirsiniz.
Kayıtlar cihazda SharedPreferences içinde saklanır (şifreli değildir); hassas metinler için kullanmayın.

## Android kısıtlaması
Android 10+ sürümlerinde üçüncü taraf uygulamalar, odakta veya varsayılan klavye değilken panoya sürekli erişemez.
Bu proje ClipboardManager dinleyicisini kullanır; ancak başka uygulamalardan yapılan **tüm**
kopyalamaları yakalayamaz. Ekran üstü izni ve foreground service bu kısıtlamayı aşmaz.
Uygulama, çentik açılırken panoyu okumayı da dener. İşletim sistemi erişimi reddedebilir.
Klavye değişmeden eksiksiz otomatik geçmiş için standart Android API'si yoktur.
\n## v1.1\nYeni uygulama ikonu, çentik yüksekliği (48–160 dp), genişliği (22–65 dp), saydamlığı (%20–100) ve canlı renk ayarı eklendi. Ayarlar kalıcıdır.\n\n## v1.2 Android 15 deneme\nErişilebilirlik servisi eklendi: yalnızca Kopyala eylemine benzeyen olayları algılar ve Android izin verirse panodaki metni kaydeder. Erişilebilirlik izni otomatik verilmez; Ayarlar > Erişilebilirlik > ClipDock üzerinden kullanıcı açar. Android 15'te bu servis dahi tüm panoya erişemez. Shizuku entegrasyonu **yoktur**: Shizuku tek başına bu sistem kısıtlamasını güvenilir biçimde çözmez. Erişilebilirlik hizmeti ekran metinlerini okumaz.\n\n## v1.3\nÇentik simgesi kaldırıldı. Çentik genişliği 8 dp, yüksekliği 20 dp seviyesine kadar küçültülebilir. Çentiğe dokunmak artık panoyu yeniden okumaz; yalnızca kayıt listesini açar. Android erişim kısıtlamaları nedeniyle tüm kopyalamalar otomatik yakalanamaz.\n
## v1.4 – Çentikten pano senkronizasyonu
Çentiğe dokununca kısa süreli şeffaf bir Activity ön plana getirilerek panonun son içeriği okunur, ardından panel açılır. Android'in ön plan koşuluna uymayı dener; cihaz üreticisine göre geçiş görülebilir veya erişim engellenebilir. Geçmişteki tüm ara kopyalamaları kurtaramaz. Çentik etkinleştir butonu tek başına kayıt eklemez.
\n## v1.5 panel titreşimi düzeltmesi\nKopyala, sil ve temizle işlemleri artık pencereyi kaldırıp yeniden oluşturmaz; yalnızca listeyi yerinde günceller. Uygulamanın kendi kopyalama işlemi pano dinleyicisinde kısa süreli göz ardı edilir. Kopyala işleminden sonra panel açık kalır.\n\n## v1.6\nPanel ekranın %96 genişliğini ve %88 yüksekliğini kaplar. Kopyalama simgesi 17sp ve düğmesi 30dp olarak küçültüldü. Çentik renk, genişlik, yükseklik ve saydamlık ayarları açılır panel içindeki genişletilebilir ayarlar bölümüne eklendi. Çentik ayarları paneli kapatmadan anında uygulanır. Android 15 pano kısıtlamaları devam eder.\n\n## v1.7\nPanel ekran yüksekliğini tamamen, genişliğin %82'sini kaplar. Sağdaki yarı saydam boşluğa dokununca kapanır. İç işlemler paneli kapatmaz.\n\n## v1.8 Kaydırarak kapatma\nPanel içinde sola doğru en az 75dp yatay kaydırma hareketi paneli kapatır. Sağdaki karartılmış boşluğa dokunarak kapatma devam eder. Dikey liste kaydırma ve kopyalama butonları korunur.\n\n\n## v1.9 — Sabitlenen kopyalar ve açıklamalar\n- Yıldız ile kopyayı sabitle / sabitlemeyi kaldır.\n- Sabitlenenler listenin üstünde tutulur; yeni kopyalar veya Temizle işlemi bunları silmez.\n- Her kopyanın altında Açıklama ekle / düzenle alanı bulunur.\n- Mevcut metin geçmişi otomatik olarak yeni veri biçimine taşınır.\n- Sabitlenmiş kayıtlar da × ile tek tek silinebilir.\n
## v1.9.1 — Sabitlenenlerde üç nokta menüsü
- Sabitlenmiş kayıtların doğrudan silme düğmesi yerine dikey üç nokta (⋮) menüsü vardır.
- Silme işlemi menüdeki **Sil** seçeneğiyle yapılır. Sabitlenmemiş kayıtlarda doğrudan silme düğmesi korunur.

## v1.9.2 — Aşağı kaydırarak silme
- Sabitlenmemiş bir kopya üzerinde 110dp'den uzun aşağı kaydırma hareketi kaydı siler.
- Sabitlenmiş kayıtlar aşağı kaydırmayla silinmez; yalnızca ⋮ menüsünden silinir.
- Yatay panel kapatma hareketi korunur.
- Not: Listeyi aşağı doğru kaydırmak için yapılan uzun hareketler de silme olarak yorumlanabilir.

## v2.0 — Akıllı içerik ve sıralama
- URL: bağlantıyı tarayıcıda açma (↗); telefon: arama ekranı (☎); IBAN: boşluksuz kopyalama (₺).
- Sabitlenmiş kayıtlarda ⋮ menüsünden Yukarı taşı / Aşağı taşı. Sabit sırası yeniden kopyalamada korunur.
- Sağa uzun kaydırma (100dp) yalnızca sabitlenmemiş kaydı siler. Dikey kaydırma artık silmez.
- Paneli kapatma: X veya karartılmış dış alana dokunma. Panel içindeki sola kaydırma kaldırıldı.
- Android 10+ arka plan pano okuma kısıtlamaları devam eder.

## v2.1 — Gizli kayıtlar
- Normal kayıttaki göz simgesiyle gizli kayda dönüştür.
- Gizli kayıtta metin ve açıklama maskelenir; göz simgesiyle göster/gizle.
- Gizli kaydı kapatıp açınca yeniden maskeli görünür.
- Gizli kayıtlar kaydırarak veya Temizle ile silinmez; ⋮ menüsünden yönetilir.
- Bu özellik yalnızca ekranda gizleme sağlar. Yerel kayıtlar şifrelenmez, biyometrik/PIN doğrulama yoktur.

## v2.2 — Swipe çentik kontrolü
- Çentiği sağa 48dp kaydır: pano yakalama ekranını/paneli aç. Tek dokunuş açmaz.
- Çentiğe uzun bas ve dikey sürükle: konumunu değiştir.
- Açılırken kısa titreşim; uzun basma algılandığında dokunsal geri bildirim.
- Panel başlığında sola 60dp kaydırarak paneli kapat; X ve dış alana dokunma korunur.
- Liste öğelerindeki sağa kaydırarak silme hareketi bağımsızdır.
- Not: Sistem titreşim/dokunsal geri bildirim ayarları kapalıysa titreşim olmayabilir.


## v2.3 — Yedekleme / Geri yükleme
- Ana ekrandaki **Yedek oluştur (JSON)** ile kayıtları, sabitleme sırasını, notları, gizlilik işaretlerini ve çentik ayarlarını bir dosyaya kaydet.
- **Yedeği geri yükle** ile bu dosyayı seçerek yeni kurulumda kayıtları geri al. Geri yükleme mevcut verilerin yerine geçer.
- Android uygulaması kaldırılırsa uygulama verileri silinir; önceden dışa aktarılmış yedek gereklidir.
- Gizli kayıtlar yedek dosyasında **şifrelenmemiş** olarak bulunur. Dosyayı güvenli bir yerde sakla.
- Aynı paket adı ve aynı imzayla APK'yı kaldırmadan güncellemek normalde mevcut verileri korur.

## v2.4 — Şifreleme
- Yerel kopya geçmişi AES-256-GCM ve Android Keystore anahtarıyla şifrelenir. Mevcut düz metin geçmişi ilk erişimde şifreli biçime taşınır.
- Parolalı yedekler AES-256-GCM + PBKDF2-HMAC-SHA256 (310.000 iterasyon, rastgele salt ve nonce) kullanır; .cdb dosyası yeni kurulumda parolayla geri yüklenebilir.
- Ayarlardan isteğe bağlı biyometrik koruma etkinleştirilirse göz simgesi gizli kaydı açmadan önce biyometrik doğrulama ister (Android 9+).
- ÖNEMLİ: Eski v2.3 düz metin JSON yedekleri bu sürümde içe aktarılmaz. Güncelleme öncesi mevcut verilerinizi yedekleyin.
- Uygulama kaldırıldığında Keystore anahtarı ve yerel veriler silinir; yeniden yüklemeden önce .cdb yedeği dışarı kaydedilmelidir.
- Bu koruma Android panosundaki sistem kopyalarını, ekran görüntülerini veya başka uygulamaların erişebildiği verileri şifrelemez.

## v2.5 — Çentik dokunma hareketi düzeltmesi
- Paneli açmak için kısa sağa sürükleme (18dp): Android'in kenar geri hareketiyle daha az çakışır.
- Panel artık kaydırma sonunda doğrudan açılır; araya geçici Activity sokulmaz.
- Çentiği taşımak için hareket etmeden uzun basın, titreşimden sonra yukarı/aşağı sürükleyin.
- Parmağı kaydırmadan tek dokunma paneli açmaz.
- Çentik ekrandan dışarı taşınmaz, yukarı/aşağı güvenli sınırlarda tutulur.
- Not: Android'in kenar hareketleri ve üretici overlay kısıtları cihazdan cihaza farklılık gösterebilir.

## v2.6 — Android hareketli gezinme uyumluluğu
- Çentik ekranın solundan 12dp içeride açılır.
- Android 10+ için yalnızca çentik alanına systemGestureExclusionRects uygulanır (üretici/sistem sınırlamalarına tabi).
- Çift dokunma alternatif açılış yöntemidir; tek dokunma açmaz.
- Sağa kısa kaydırma paneli açar, uzun basıp titreşimden sonra dikey taşıma yapılır.
- Çentiğin dikey konumu yeniden başlatmalarda saklanır.
- Gerçek cihazda ve APK derlemesinde test edilmemiştir.

## v2.7 — Çentik sol kenarda ve daha geniş dokunma alanıyla
- Görünür çentik tam sol kenarda; 56dp şeffaf dokunma alanı kaydırma/çift dokunmayı kolaylaştırır.
- 14dp sağa kaydırma parmak kaldırılınca paneli açar.
- Uzun basıp titreşimden sonra yukarı/aşağı sürükleyerek taşıyın.
- Çift dokunma alternatif açma hareketidir.
- Daha önceki şifreli yedekleme ve gizli kayıtlar korunur.
- Derleme ve fiziksel cihaz testi henüz doğrulanmadı.

## v2.8 — Panel açılmıyor düzeltmesi
- Tek dokunma paneli açar (hareketli gezinmede güvenilir yedek yöntem).
- Sağa kaydırma hareketi ACTION_MOVE sırasında açılır; Android ACTION_CANCEL üretse de açılış tetiklenir.
- Uzun basarak çentiği taşıma korunur.
- Panel açılışındaki hatalar artık logcat'e yazılır ve ekranda hata gösterilir; ana uygulama ekranı yedek olarak açılır.
- Şifreli geçmiş okunamazsa panelin tamamı kapanmak yerine kullanıcıya geri yükleme uyarısı gösterilir.
- Gerçek cihaz APK derlemesi doğrulanmadı.

## v2.9 stability fixes
- Replaced malformed touch-handler braces with a single simple state machine.
- Removed overlay gesture-exclusion callbacks that may fail on OEM Android builds.
- Prevented encrypted history reads before panel construction.
- Guarded overlay attach/remove and service cleanup paths.
- Supports tap to open, swipe right to open, and long-press then drag vertically.
- APK compilation and device testing are NOT verified.
