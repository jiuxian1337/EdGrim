# EdGrim

**EdGrim** ist ein Fork von [Grim](https://github.com/GrimAnticheat/Grim), dem freien simulationsbasierten Minecraft-Anticheat. Dieser Fork erweitert die ursprüngliche Grim-Codebasis um eine große Anzahl an Prüfungen und Erkennungsmodulen.

> **Haftungsausschluss** — Dieses Projekt enthält eine erhebliche Menge an KI-generiertem Code (Gemini / ChatGPT). Vor dem Produktiveinsatz gründlich prüfen und testen.

## Fork-Basis

- **Upstream:** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **Dieser Fork:** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **Lizenz:** GPLv3

## Beschreibung

Freier simulationsbasierter Anticheat für 1.21 mit Unterstützung für 1.8–1.21, basierend auf PacketEvents 2.0.

**Über 100 Prüfungen**, darunter:

| Kategorie | Beschreibung |
|---|---|
| Aim (Aimbot) | Heuristische / trajektorienbasierte Aimbot-Erkennung |
| Autoclicker (Autoklicker) | Klickmusteranalyse |
| BadPackets (Ungültige Pakete) | Erkennung fehlerhafter / ungültiger Pakete |
| Baritone (Baritone) | Baritone-Bot-Erkennung |
| Breaking (Blockabbau) | Validierung des Blockabbaus |
| Chat (Chat) | Chat- / Befehlsmissbrauch |
| Combat (Kampf) | Reichweite, Hitboxen, Mehrfachinteraktion |
| Crash (Absturz) | Schutz vor Absturz-Exploits |
| Elytra (Elytren) | Elytren-Bewegungsvalidierung |
| Exploit (Exploits) | Allgemeine Exploit-Minderung |
| Flight (Fliegen) | Flug- / Bewegungshacks |
| GroundSpoof (Boden-Spoofing) | NoFall / Boden-Spoofing |
| Interact (Interaktion) | Interaktionsvalidierung |
| Inventory (Inventar) | Inventarmanipulation |
| Misc (Sonstiges) | Client-Marke, Geisterblöcke, Transaktionsreihenfolge |
| Movement (Bewegung) | Bewegungsmusterprüfung |
| MultiActions (Mehrfachaktionen) | Missbrauch kombinierter Aktionen |
| PacketOrder (Paketreihenfolge) | Paketsequenzprüfungen |
| PingSpoof (Ping-Spoofing) | Ping- / Latenz-Spoofing |
| Prediction (Vorhersage) | Vorhersagebasierte Offset- und Phasenprüfungen |
| Scaffolding (Scaffold) | Scaffold- / Tower-Erkennung |
| Sprint (Sprinten) | Sprinten / omnidirektionales Sprinten |
| Timer (Timer) | Spieltimer-Manipulation |
| Vehicle (Fahrzeuge) | Fahrzeug-Bewegungshacks |
| Velocity (Rückstoß) | Rückstoß- / Geschwindigkeitsprüfung |

## Fremdcode-Referenzen

Dieser Fork bezieht Inspiration und Implementierungsdetails aus den folgenden Anticheat-Projekten:

| Projekt | Quelle |
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

## Build

```bash
./gradlew build
```

Voraussetzungen:
- JDK 21+
- Gradle (Wrapper enthalten)

## Plattform-Unterstützung

- Paper / Spigot 1.8–1.21
- Folia-Scheduler-Unterstützung
- PacketEvents 2.0
- ViaVersion-Kompatibilität

## Mitwirken

Dies ist ein persönlicher Fork. Issues und PRs werden nicht aktiv überwacht.

## Lizenz

GPLv3 — siehe [LICENSE](LICENSE). Modifizierte Binärdateien oder Plugins mit kopiertem Grim-Code müssen privat sein oder den Empfängern den vollständigen Quellcode ohne zusätzliche Kosten zur Verfügung stellen.
