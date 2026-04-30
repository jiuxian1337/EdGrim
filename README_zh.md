# EdGrim

**EdGrim** 是 [Grim](https://github.com/GrimAnticheat/Grim) 的一个分支，Grim 是自由开源的模拟式 Minecraft 反作弊。本分支在上游 Grim 代码库之外新增了大量检测和检测模块。

> **免责声明** — 本项目包含大量 AI 生成的代码（Gemini / ChatGPT）。在生产环境部署前请务必充分审查和测试。

## 分支基础

- **上游项目：** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **本分支：** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **许可证：** GPLv3

## 项目描述

基于 PacketEvents 2.0 的自由仿真反作弊，专为 1.21 设计，同时支持 1.8–1.21。

**100+ 检测项**，包括：

| 类别 | 描述 |
|---|---|
| Aim（自瞄） | 基于启发式/轨迹的自瞄检测 |
| Autoclicker（连点器） | 点击模式分析 |
| BadPackets（异常数据包） | 畸形/无效数据包检测 |
| Baritone（机器人） | Baritone 机器人检测 |
| Breaking（破坏） | 方块破坏合法性检查 |
| Chat（聊天） | 聊天/命令滥用 |
| Combat（战斗） | 攻击距离、碰撞箱、多重交互 |
| Crash（崩溃） | 崩溃漏洞防护 |
| Elytra（鞘翅） | 鞘翅移动验证 |
| Exploit（漏洞利用） | 通用漏洞缓解 |
| Flight（飞行） | 飞行/移动作弊 |
| GroundSpoof（地面伪装） | NoFall / 地面欺骗 |
| Interact（交互） | 交互合法性检查 |
| Inventory（背包） | 背包操作异常 |
| Misc（杂项） | 客户端品牌、幽灵方块、事务顺序 |
| Movement（移动） | 移动模式验证 |
| MultiActions（多重操作） | 组合操作滥用 |
| PacketOrder（数据包顺序） | 数据包时序检查 |
| PingSpoof（延迟伪装） | Ping / 延迟欺骗 |
| Prediction（预测） | 基于预测的偏移和相位检查 |
| Scaffolding（搭路） | 搭路/速建检测 |
| Sprint（疾跑） | 疾跑/全方向疾跑 |
| Timer（变速） | 游戏计时器篡改 |
| Vehicle（载具） | 载具移动作弊 |
| Velocity（击退） | 击退/速度验证 |

## 第三方代码引用

本分支从以下反作弊项目中汲取灵感和实现细节：

| 项目 | 来源 |
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

## 构建

```bash
./gradlew build
```

要求：
- JDK 21+
- Gradle（包含 wrapper）

## 平台支持

- Paper / Spigot 1.8–1.21
- Folia 调度器支持
- PacketEvents 2.0
- ViaVersion 兼容

## 贡献

此为个人分支，不活跃关注 Issue 和 PR。

## 许可证

GPLv3 — 详见 [LICENSE](LICENSE)。修改后的二进制文件或复制了 Grim 代码的插件必须保持私有，或向接收者免费提供完整源代码。
