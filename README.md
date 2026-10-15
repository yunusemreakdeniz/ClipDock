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
\n## v1.1\nYeni uygulama ikonu, çentik yüksekliği (48–160 dp), genişliği (22–65 dp), saydamlığı (%20–100) ve canlı renk ayarı eklendi. Ayarlar kalıcıdır.\n\n## v1.2 Android 15 deneme\nErişilebilirlik servisi eklendi: yalnızca Kopyala eylemine benzeyen olayları algılar ve Android izin verirse panodaki metni kaydeder. Erişilebilirlik izni otomatik verilmez; Ayarlar > Erişilebilirlik > ClipDock üzerinden kullanıcı açar. Android 15'te bu servis dahi tüm panoya erişemez. Shizuku entegrasyonu **yoktur**: Shizuku tek başına bu sistem kısıtlamasını güvenilir biçimde çözmez. Erişilebilirlik hizmeti ekran metinlerini okumaz.\n