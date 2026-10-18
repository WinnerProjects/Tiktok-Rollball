# RollBall

RollBall, KatamariIO projesinden türetilmiş bir Minecraft Fabric mod projesidir.

## Özellikler

- `/boyut`: Boyut bossbar göstergesini açar/kapatır.
- Bossbar metni `Boyut: X.X Blok` biçimindedir.
- Bossbar, oyuncunun `renderSize` değerini gösterir ve sunucu tick'inde güncellenir.
- Büyük top modelleri için mevcut LOD/baked-index render yaklaşımı korunmuştur.

## Kaynak proje ve atıf

Bu proje, **TACOWASA059 tarafından geliştirilen KatamariIO** kaynak kodundan türetilmiştir.
Orijinal proje adı ve yazarı burada açıkça belirtilmiştir. Orijinal kaynak pakette bulunan `LICENSE`
dosyası MIT lisans metnidir; bu lisansın telif ve izin bildirimi dağıtımlarda korunmalıdır.

**Not:** Orijinal mod sayfasındaki lisans bildirimi ile kaynak paketteki MIT lisans metni birbiriyle
çelişiyorsa, dağıtımdan önce hak sahibinden yazılı izin alın ve tüm üçüncü taraf varlıklarının
lisansını ayrıca doğrulayın. Bu README tek başına hukuki izin oluşturmaz.

## Komutlar

- `/boyut` — Boyut bossbar'ını aç/kapat.
- `/katamari size <targets> <value>` — Hedef oyuncuların boyutunu değiştirir.
- `/katamari size <target>` — Hedef oyuncunun boyutunu gösterir.
- `/katamari restitution <targets> <value>` — Esneklik katsayısını değiştirir.
- `/katamari restitution <target>` — Esneklik katsayısını gösterir.
- `/katamari ball <targets> <enabled>` — Top modunu açar/kapatır.
- `/katamari resetBlocks <targets>` — Ekli blokları ve boyutu sıfırlar.

## Derleme durumu

Kaynak proje başlangıçta Minecraft 1.20.1 için hazırlanmıştır. `gradle.properties` ve mod metadata
1.21.11'e taşınacak şekilde güncellenmiştir; ancak Minecraft 1.20.1 → 1.21.11 geçişi API, mapping,
Mixin ve Gradle eklentisi değişiklikleri içerir. Bu kaynak paketi, hedef ortamda derlenip oyunda
test edilmeden **hazır/çalışan 1.21.11 JAR'ı olarak kabul edilmemelidir**.
