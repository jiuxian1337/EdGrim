# EdGrim

**EdGrim** est un fork de [Grim](https://github.com/GrimAnticheat/Grim), l'anti-triche libre de simulation pour Minecraft. Ce fork ajoute un grand nombre de contrôles et de modules de détection en plus de la base de code Grim d'origine.

> **Avertissement** — Ce projet contient une quantité significative de code généré par IA (Gemini / ChatGPT). Vérifiez et testez soigneusement avant de déployer en production.

## Base du fork

- **Upstream :** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **Ce fork :** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **Licence :** GPLv3

## Description

Anti-triche libre par simulation conçu pour la 1.21 avec support 1.8–1.21, propulsé par PacketEvents 2.0.

**Plus de 100 contrôles**, incluant :

| Catégorie | Description |
|---|---|
| Aim (Visée) | Détection heuristique / par trajectoire de la visée |
| Autoclicker (Auto-clic) | Analyse des schémas de clics |
| BadPackets (Paquets invalides) | Détection des paquets malformés / invalides |
| Baritone (Baritone) | Détection du bot Baritone |
| Breaking (Minage) | Validité du minage de blocs |
| Chat (Chat) | Abus de chat / commandes |
| Combat (Combat) | Portée, hitboxes, interactions multiples |
| Crash (Crash) | Prévention des exploits de crash |
| Elytra (Élytres) | Validation du mouvement en élytres |
| Exploit (Exploits) | Atténuation générale des exploits |
| Flight (Vol) | Hacks de vol / mouvement |
| GroundSpoof (Sol usurpé) | NoFall / usurpation de sol |
| Interact (Interaction) | Validation des interactions |
| Inventory (Inventaire) | Manipulation d'inventaire |
| Misc (Divers) | Marque client, blocs fantômes, ordre des transactions |
| Movement (Mouvement) | Validation des schémas de mouvement |
| MultiActions (Actions multiples) | Abus d'actions combinées |
| PacketOrder (Ordre des paquets) | Contrôle de séquence des paquets |
| PingSpoof (Usurpation de ping) | Usurpation de ping / latence |
| Prediction (Prédiction) | Contrôles de décalage et de phase basés sur la prédiction |
| Scaffolding (Scaffold) | Détection de scaffold / tower |
| Sprint (Sprint) | Sprint / sprint omnidirectionnel |
| Timer (Timer) | Manipulation du timer du jeu |
| Vehicle (Véhicules) | Hacks de mouvement des véhicules |
| Velocity (Recul) | Validation du recul / de la vitesse |

## Références de code tiers

Ce fork s'inspire et tire des détails d'implémentation des projets anti-triche suivants :

| Projet | Source |
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

## Compilation

```bash
./gradlew build
```

Prérequis :
- JDK 21+
- Gradle (wrapper inclus)

## Plateformes supportées

- Paper / Spigot 1.8–1.21
- Support du planificateur Folia
- PacketEvents 2.0
- Compatibilité ViaVersion

## Contribuer

Ceci est un fork personnel. Les issues et PR ne sont pas activement surveillées.

## Licence

GPLv3 — voir [LICENSE](LICENSE). Les binaires modifiés ou les plugins contenant du code Grim copié doivent rester privés ou fournir le code source complet aux destinataires sans frais supplémentaires.
