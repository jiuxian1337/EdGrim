# EdGrim

**EdGrim** は、自由シミュレーション型 Minecraft アンチチート [Grim](https://github.com/GrimAnticheat/Grim) のフォークです。このフォークはアップストリームの Grim コードベースに加え、多数の検出チェックと検出モジュールを追加しています。

> **免責事項** — このプロジェクトには大量の AI 生成コード（Gemini / ChatGPT）が含まれています。本番環境にデプロイする前に十分にレビューしテストしてください。

## フォークベース

- **アップストリーム:** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **このフォーク:** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **ライセンス:** GPLv3

## 概要

PacketEvents 2.0 を利用した 1.21 向けの自由シミュレーションアンチチート。1.8～1.21 をサポート。

**100以上の検出項目**、内訳:

| カテゴリ | 説明 |
|---|---|
| Aim (エイム) | ヒューリスティック / 軌道ベースのエイム検出 |
| Autoclicker (自動クリック) | クリックパターン分析 |
| BadPackets (不正パケット) | 不正 / 無効なパケットの検出 |
| Baritone (バリトン) | Baritone ボット検出 |
| Breaking (破壊) | ブロック破壊の妥当性検証 |
| Chat (チャット) | チャット / コマンドの悪用 |
| Combat (戦闘) | リーチ、ヒットボックス、多重インタラクション |
| Crash (クラッシュ) | クラッシュエクスプロイトの防止 |
| Elytra (エリトラ) | エリトラ移動の検証 |
| Exploit (エクスプロイト) | 一般的なエクスプロイトの緩和 |
| Flight (飛行) | 飛行 / 移動チート |
| GroundSpoof (地上スプーフ) | NoFall / 地上判定偽装 |
| Interact (インタラクション) | インタラクションの妥当性検証 |
| Inventory (インベントリ) | インベントリ操作 |
| Misc (その他) | クライアントブランド、ゴーストブロック、トランザクション順序 |
| Movement (移動) | 移動パターン検証 |
| MultiActions (複合操作) | 複合操作の悪用 |
| PacketOrder (パケット順序) | パケットシーケンスチェック |
| PingSpoof (Ping 偽装) | Ping / レイテンシ偽装 |
| Prediction (予測) | 予測ベースのオフセットとフェーズチェック |
| Scaffolding (スキャフォールド) | スキャフォールド / タワー検出 |
| Sprint (ダッシュ) | ダッシュ / 全方向ダッシュ |
| Timer (タイマー) | ゲームタイマー改ざん |
| Vehicle (乗り物) | 乗り物移動チート |
| Velocity (ノックバック) | ノックバック / 速度検証 |

## サードパーティコード参照

このフォークは以下のアンチチートプロジェクトから着想と実装詳細を参考にしています:

| プロジェクト | ソース |
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

## ビルド

```bash
./gradlew build
```

要件:
- JDK 21+
- Gradle（ラッパー同梱）

## プラットフォームサポート

- Paper / Spigot 1.8–1.21
- Folia スケジューラー対応
- PacketEvents 2.0
- ViaVersion 互換

## コントリビューション

個人フォークです。Issue や PR は積極的に監視されません。

## ライセンス

GPLv3 — [LICENSE](LICENSE) 参照。Grim コードを複製した改変バイナリまたはプラグインは非公開とするか、受領者に追加費用なしで完全なソースコードを提供する必要があります。
