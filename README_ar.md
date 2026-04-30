# EdGrim

**EdGrim** هو تفرع (fork) من [Grim](https://github.com/GrimAnticheat/Grim)، مضاد الغش المحاكي الحر للعبة ماينكرافت. يضيف هذا التفرع عددًا كبيرًا من الفحوصات ووحدات الكشف تتجاوز قاعدة كود Grim الأساسية.

> **إخلاء مسؤولية** — يحتوي هذا المشروع على كمية كبيرة من الكود المولَّد بالذكاء الاصطناعي (Gemini / ChatGPT). راجع واختبر بدقة قبل النشر في بيئة الإنتاج.

## أساس التفرع

- **المشروع الأصلي:** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **هذا التفرع:** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **الترخيص:** GPLv3

## الوصف

مضاد غش محاكي حر مصمم للإصدار 1.21 مع دعم الإصدارات من 1.8 إلى 1.21، يعمل بواسطة PacketEvents 2.0.

**أكثر من 100 فحص**، تشمل:

| الفئة | الوصف |
|---|---|
| Aim (التصويب) | كشف التصويب القائم على الاستدلال / المسار |
| Autoclicker (النقر التلقائي) | تحليل أنماط النقر |
| BadPackets (حزم غير صالحة) | كشف الحزم المشوهة / غير الصالحة |
| Baritone (باريتون) | كشف بوت Baritone |
| Breaking (تكسير المكعبات) | التحقق من صلاحية تكسير المكعبات |
| Chat (الدردشة) | إساءة استخدام الدردشة / الأوامر |
| Combat (القتال) | مدى الضربات، صناديق الاصطدام، التفاعل المتعدد |
| Crash (انهيار) | منع استغلال الانهيار |
| Elytra (الأجنحة) | التحقق من حركة الأجنحة |
| Exploit (استغلال) | تخفيف عام للاستغلالات |
| Flight (الطيران) | اختراقات الطيران / الحركة |
| GroundSpoof (تزييف الأرض) | NoFall / تزييف الوقوف على الأرض |
| Interact (التفاعل) | التحقق من صحة التفاعل |
| Inventory (المخزون) | التلاعب بالمخزون |
| Misc (متنوعات) | ماركة العميل، المكعبات الشبحية، ترتيب المعاملات |
| Movement (الحركة) | التحقق من أنماط الحركة |
| MultiActions (إجراءات متعددة) | إساءة استخدام الإجراءات المركبة |
| PacketOrder (ترتيب الحزم) | فحوصات تسلسل الحزم |
| PingSpoof (تزييف البنق) | تزييف البنق / زمن الاستجابة |
| Prediction (التنبؤ) | فحوصات الإزاحة والطور القائمة على التنبؤ |
| Scaffolding (السقالات) | كشف السقالات / الأبراج |
| Sprint (الركض) | الركض / الركض متعدد الاتجاهات |
| Timer (المؤقت) | التلاعب بمؤقت اللعبة |
| Vehicle (المركبات) | اختراقات حركة المركبات |
| Velocity (الاندفاع) | التحقق من الاندفاع / السرعة |

## مراجع كود الطرف الثالث

يستمد هذا التفرع الإلهام وتفاصيل التنفيذ من مشاريع مكافحة الغش التالية:

| المشروع | المصدر |
|---|---|
| Intave | [intave/intave](https://github.com/intave/intave) |
| AntiCheatAddition | [Photon-GitHub/AntiCheatAddition](https://github.com/Photon-GitHub/AntiCheatAddition) |
| Artemis | [artemisac/artemis-minecraft-anticheat](https://github.com/artemisac/artemis-minecraft-anticheat) |
| FairFight | [dw1e/FairFight](https://github.com/dw1e/FairFight) |
| Karhu Fixed | [Dg32z/Karhu-Fixed](https://github.com/Dg32z/Karhu-Fixed) |
| LonAntiCheat | [Araykal/LonAntiCheat](https://github.com/Araykal/LonAntiCheat) |
| Medusa | [infiniteSM/Medusa](https://github.com/infiniteSM/Medusa) |
| MX-Project | [kireikosasha/MX-Project](https://github.com/kireikosasha/MX-Project) |
| NoCheatPlus | [Updated-NoCheatPlus/NoCheatPlus](https://github.com/Updated-NoCheatPlus/NoCheatPlus) |

## البناء

```bash
./gradlew build
```

المتطلبات:
- JDK 21+
- Gradle (مع wrapper مضمَّن)

## دعم المنصات

- Paper / Spigot 1.8–1.21
- دعم مجدول Folia
- PacketEvents 2.0
- توافق ViaVersion

## المساهمة

هذا تفرع شخصي. لا تتم مراقبة المشكلات (issues) وطلبات الدمج (PRs) بشكل نشط.

## الترخيص

GPLv3 — راجع [LICENSE](LICENSE). يجب أن تكون النسخ المعدلة أو الإضافات التي تحتوي على كود Grim منسوخًا خاصة، أو توفر كود المصدر الكامل للمستلمين دون أي تكلفة إضافية.
