# EdGrim

**EdGrim** is a fork of [Grim](https://github.com/GrimAnticheat/Grim), the libre simulation Minecraft anti-cheat. It adds a large number of checks and detection modules beyond the upstream Grim codebase.

> **Disclaimer** — This project contains a significant amount of AI-generated code (Gemini / ChatGPT). Review and test thoroughly before deploying in production.

## Languages

[English](README.md) | [中文](README_zh.md) | [Русский](README_ru.md) | [한국어](README_ko.md) | [日本語](README_ja.md) | [Deutsch](README_de.md) | [Français](README_fr.md) | [Español](README_es.md) | [العربية](README_ar.md) | [ئۇيغۇرچە](README_ug.md)

## Fork Base

- **Upstream:** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **This fork:** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **License:** GPLv3

## Description

Libre simulation anticheat designed for 1.21 with 1.8–1.21 support, powered by PacketEvents 2.0.

**100+ checks across**, including:

| Category | Description |
|---|---|
| Aim | Heuristic / trajectory-based aim detection |
| Autoclicker | Click pattern analysis |
| BadPackets | Malformed / invalid packet detection |
| Baritone | Baritone bot detection |
| Breaking | Block-breaking validity |
| Chat | Chat / command abuse |
| Combat | Reach, hitboxes, multi-interact |
| Crash | Crash exploit prevention |
| Elytra | Elytra movement validation |
| Exploit | General exploit mitigation |
| Flight | Flight / movement hacks |
| GroundSpoof | NoFall / ground spoof |
| Interact | Interaction validation |
| Inventory | Inventory manipulation |
| Misc | Client brand, ghost blocks, transaction order |
| Movement | Movement pattern validation |
| MultiActions | Combined action abuse |
| PacketOrder | Packet sequencing checks |
| PingSpoof | Ping / latency spoofing |
| Prediction | Prediction-based offset & phase checks |
| Scaffolding | Scaffold / tower detection |
| Sprint | Sprint / omni-sprint |
| Timer | Game timer manipulation |
| Vehicle | Vehicle movement hacks |
| Velocity | Knockback / velocity validation |

## Third-party code references

This fork draws inspiration and implementation details from the following anti-cheat projects:

| Project | Source |
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

Requirements:
- JDK 21+
- Gradle (wrapper included)

## Platform support

- Paper / Spigot 1.8–1.21
- Folia scheduler support
- PacketEvents 2.0
- ViaVersion compatibility

## Contributing

This is a personal fork. Issues and PRs are not actively monitored.

## License

GPLv3 — see [LICENSE](LICENSE). Modified binaries or plugins with copied Grim code must be private, or provide full source code to recipients at no additional cost.
