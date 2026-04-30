# EdGrim

**EdGrim** es un fork de [Grim](https://github.com/GrimAnticheat/Grim), el anticheat libre de simulación para Minecraft. Este fork añade una gran cantidad de comprobaciones y módulos de detección más allá del código base original de Grim.

> **Aviso legal** — Este proyecto contiene una cantidad significativa de código generado por IA (Gemini / ChatGPT). Revisa y prueba exhaustivamente antes de usar en producción.

## Base del fork

- **Upstream:** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **Este fork:** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **Licencia:** GPLv3

## Descripción

Anticheat libre de simulación diseñado para 1.21 con soporte 1.8–1.21, impulsado por PacketEvents 2.0.

**Más de 100 comprobaciones**, incluyendo:

| Categoría | Descripción |
|---|---|
| Aim (Apuntería) | Detección de aimbot heurística / por trayectoria |
| Autoclicker (Auto-clic) | Análisis de patrones de clic |
| BadPackets (Paquetes inválidos) | Detección de paquetes malformados / inválidos |
| Baritone (Baritone) | Detección de bots Baritone |
| Breaking (Rotura) | Validación de rotura de bloques |
| Chat (Chat) | Abuso de chat / comandos |
| Combat (Combate) | Alcance, hitboxes, multi-interacción |
| Crash (Crasheo) | Prevención de exploits de crasheo |
| Elytra (Élitras) | Validación de movimiento con élitras |
| Exploit (Exploits) | Mitigación general de exploits |
| Flight (Vuelo) | Hacks de vuelo / movimiento |
| GroundSpoof (Suelo falso) | NoFall / suplantación de suelo |
| Interact (Interacción) | Validación de interacciones |
| Inventory (Inventario) | Manipulación de inventario |
| Misc (Misceláneo) | Marca de cliente, bloques fantasma, orden de transacciones |
| Movement (Movimiento) | Validación de patrones de movimiento |
| MultiActions (Multi-acciones) | Abuso de acciones combinadas |
| PacketOrder (Orden de paquetes) | Comprobaciones de secuencia de paquetes |
| PingSpoof (Suplantación de ping) | Suplantación de ping / latencia |
| Prediction (Predicción) | Comprobaciones de fase y desplazamiento basadas en predicción |
| Scaffolding (Scaffold) | Detección de scaffold / torre |
| Sprint (Sprint) | Sprint / sprint omnidireccional |
| Timer (Timer) | Manipulación del temporizador del juego |
| Vehicle (Vehículos) | Hacks de movimiento en vehículos |
| Velocity (Retroceso) | Validación de retroceso / velocidad |

## Referencias de código de terceros

Este fork toma inspiración y detalles de implementación de los siguientes proyectos anticheat:

| Proyecto | Fuente |
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

## Compilación

```bash
./gradlew build
```

Requisitos:
- JDK 21+
- Gradle (wrapper incluido)

## Soporte de plataformas

- Paper / Spigot 1.8–1.21
- Soporte del planificador Folia
- PacketEvents 2.0
- Compatibilidad con ViaVersion

## Contribuciones

Este es un fork personal. No se supervisan activamente issues ni PRs.

## Licencia

GPLv3 — consulta [LICENSE](LICENSE). Los binarios modificados o plugins con código Grim copiado deben ser privados o proporcionar el código fuente completo a los destinatarios sin costo adicional.
