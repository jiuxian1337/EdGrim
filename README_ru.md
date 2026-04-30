# EdGrim

**EdGrim** — это форк [Grim](https://github.com/GrimAnticheat/Grim), свободного симуляционного античита для Minecraft. Данный форк добавляет большое количество проверок и модулей обнаружения поверх кодовой базы оригинального Grim.

> **Дисклеймер** — Этот проект содержит значительное количество AI-сгенерированного кода (Gemini / ChatGPT). Тщательно проверяйте и тестируйте перед использованием в продакшене.

## Основа форка

- **Оригинал:** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **Данный форк:** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **Лицензия:** GPLv3

## Описание

Свободный симуляционный античит для 1.21 с поддержкой 1.8–1.21, работающий на PacketEvents 2.0.

**100+ проверок**, включая:

| Категория | Описание |
|---|---|
| Aim (Аим) | Эвристическое / траекторное обнаружение аима |
| Autoclicker (Автокликер) | Анализ паттернов кликов |
| BadPackets (Плохие пакеты) | Обнаружение некорректных / недействительных пакетов |
| Baritone (Баритон) | Обнаружение ботов Baritone |
| Breaking (Разрушение) | Проверка валидности разрушения блоков |
| Chat (Чат) | Злоупотребление чатом / командами |
| Combat (Бой) | Дистанция атаки, хитбоксы, мульти-взаимодействие |
| Crash (Краш) | Защита от краш-эксплойтов |
| Elytra (Элитры) | Проверка движения на элитрах |
| Exploit (Эксплойты) | Общее смягчение эксплойтов |
| Flight (Полёт) | Читы на полёт / движение |
| GroundSpoof (Подмена земли) | NoFall / подмена нахождения на земле |
| Interact (Взаимодействие) | Проверка валидности взаимодействия |
| Inventory (Инвентарь) | Манипуляции с инвентарём |
| Misc (Разное) | Бренд клиента, фантомные блоки, порядок транзакций |
| Movement (Движение) | Проверка паттернов движения |
| MultiActions (Мульти-действия) | Злоупотребление комбинированными действиями |
| PacketOrder (Порядок пакетов) | Проверка последовательности пакетов |
| PingSpoof (Подмена пинга) | Подмена пинга / задержки |
| Prediction (Предсказание) | Проверки смещения и фазы на основе предсказания |
| Scaffolding (Скаффолд) | Обнаружение скаффолда / быстрой постройки |
| Sprint (Спринт) | Спринт / всенаправленный спринт |
| Timer (Таймер) | Манипуляция игровым таймером |
| Vehicle (Транспорт) | Читы на движение транспорта |
| Velocity (Скорость) | Проверка отбрасывания / скорости |

## Сторонние заимствования кода

Данный форк черпает вдохновение и детали реализации из следующих античит-проектов:

| Проект | Источник |
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

## Сборка

```bash
./gradlew build
```

Требования:
- JDK 21+
- Gradle (wrapper включён)

## Поддержка платформ

- Paper / Spigot 1.8–1.21
- Поддержка планировщика Folia
- PacketEvents 2.0
- Совместимость с ViaVersion

## Участие в разработке

Это личный форк. Issues и PR активно не отслеживаются.

## Лицензия

GPLv3 — см. [LICENSE](LICENSE). Модифицированные бинарные файлы или плагины со скопированным кодом Grim должны быть приватными, либо предоставлять полный исходный код получателям без дополнительной платы.
