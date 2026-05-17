# EdGrim

**EdGrim** 係 [Grim](https://github.com/GrimAnticheat/Grim) 嘅其中一個分支。Grim 本身係一個自由開源、以模擬為核心嘅 Minecraft 反作弊系統；而呢個分支就喺上游 Grim 嘅基礎上，再加咗唔少額外檢查同偵測模組。

> **免責聲明** — 呢個專案包含相當多由 AI 生成嘅程式碼（Gemini / ChatGPT）。正式部署去生產環境之前，請務必先做足審查同測試。

## Fork 基礎

- **上游專案：** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **本分支：** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **授權條款：** GPLv3

## 專案簡介

呢個自由模擬式反作弊系統建基於 PacketEvents 2.0，主要為 1.21 而設，同時支援 1.8–1.21。

**內置 100+ 項檢查**，包括：

| 類別 | 說明 |
|---|---|
| Aim | 基於啟發式同軌跡分析嘅自瞄偵測 |
| Autoclicker | 連點模式分析 |
| BadPackets | 異常 / 無效封包偵測 |
| Baritone | Baritone 機械人偵測 |
| Breaking | 方塊破壞合法性檢查 |
| Chat | 聊天 / 指令濫用 |
| Combat | 攻擊距離、Hitbox、多重互動 |
| Crash | 防止崩服漏洞利用 |
| Elytra | 鞘翅移動驗證 |
| Exploit | 一般漏洞利用緩解 |
| Flight | 飛行 / 移動外掛 |
| GroundSpoof | NoFall / 落地偽裝 |
| Interact | 互動合法性驗證 |
| Inventory | 背包操作異常 |
| Misc | 客戶端品牌、幽靈方塊、交易順序 |
| Movement | 移動模式驗證 |
| MultiActions | 組合操作濫用 |
| PacketOrder | 封包順序檢查 |
| PingSpoof | Ping / 延遲偽裝 |
| Prediction | 基於預測嘅偏移同穿模檢查 |
| Scaffolding | 搭路 / 搭高偵測 |
| Sprint | 疾跑 / 全方向疾跑 |
| Timer | 遊戲計時速度篡改 |
| Vehicle | 載具移動外掛 |
| Velocity | 擊退 / 速度驗證 |

## 第三方程式碼參考

呢個分支有參考以下反作弊專案嘅設計思路同部分實作細節：

| 專案 | 來源 |
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

## 建置

```bash
./gradlew build
```

需求：
- JDK 21+
- Gradle（已附帶 wrapper）

## 平台支援

- Paper / Spigot 1.8–1.21
- 支援 Folia 排程器
- PacketEvents 2.0
- 相容 ViaVersion

## 貢獻

呢個係個人分支，暫時唔會主動處理 Issue 同 PR。

## 授權

GPLv3 — 詳情請參閱 [LICENSE](LICENSE)。修改後嘅二進位發行檔，或者複製咗 Grim 程式碼嘅插件，必須保持私有；否則就要免費向接收者提供完整原始碼。