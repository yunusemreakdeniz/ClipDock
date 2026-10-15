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
