# EdGrim

**EdGrim**은 자유 시뮬레이션 마인크래프트 안티치트 [Grim](https://github.com/GrimAnticheat/Grim)의 포크입니다. 이 포크는 업스트림 Grim 코드베이스에 더해 다수의 검사 및 탐지 모듈을 추가했습니다.

> **면책 조항** — 이 프로젝트에는 AI 생성 코드(Gemini / ChatGPT)가 대량 포함되어 있습니다. 프로덕션에 배포하기 전에 철저히 검토하고 테스트하십시오.

## 포크 기반

- **업스트림:** [GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim)
- **이 포크:** [jiuxian1337/EdGrim](https://github.com/jiuxian1337/EdGrim)
- **라이선스:** GPLv3

## 설명

PacketEvents 2.0 기반의 1.21용 자유 시뮬레이션 안티치트, 1.8–1.21 지원.

**100개 이상의 검사**, 포함 항목:

| 카테고리 | 설명 |
|---|---|
| Aim (에임) | 휴리스틱 / 궤적 기반 에임 탐지 |
| Autoclicker (오토클리커) | 클릭 패턴 분석 |
| BadPackets (비정상 패킷) | 잘못된 / 유효하지 않은 패킷 탐지 |
| Baritone (바리톤) | Baritone 봇 탐지 |
| Breaking (블록 파괴) | 블록 파괴 유효성 검사 |
| Chat (채팅) | 채팅 / 명령어 남용 |
| Combat (전투) | 공격 거리, 히트박스, 다중 상호작용 |
| Crash (크래시) | 크래시 익스플로잇 방지 |
| Elytra (겉날개) | 겉날개 이동 검증 |
| Exploit (익스플로잇) | 일반 익스플로잇 완화 |
| Flight (비행) | 비행 / 이동 핵 |
| GroundSpoof (지면 스푸핑) | NoFall / 지면 스푸핑 |
| Interact (상호작용) | 상호작용 유효성 검사 |
| Inventory (인벤토리) | 인벤토리 조작 |
| Misc (기타) | 클라이언트 브랜드, 고스트 블록, 트랜잭션 순서 |
| Movement (이동) | 이동 패턴 검증 |
| MultiActions (다중 동작) | 복합 동작 남용 |
| PacketOrder (패킷 순서) | 패킷 시퀀싱 검사 |
| PingSpoof (핑 스푸핑) | 핑 / 지연 시간 스푸핑 |
| Prediction (예측) | 예측 기반 오프셋 및 페이즈 검사 |
| Scaffolding (스캐폴딩) | 스캐폴드 / 타워 탐지 |
| Sprint (달리기) | 달리기 / 전방향 달리기 |
| Timer (타이머) | 게임 타이머 조작 |
| Vehicle (탈것) | 탈것 이동 핵 |
| Velocity (넉백) | 넉백 / 속도 검증 |

## 제3자 코드 참조

이 포크는 다음 안티치트 프로젝트에서 영감과 구현 세부 사항을 참고했습니다:

| 프로젝트 | 출처 |
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

## 빌드

```bash
./gradlew build
```

요구 사항:
- JDK 21+
- Gradle (래퍼 포함)

## 플랫폼 지원

- Paper / Spigot 1.8–1.21
- Folia 스케줄러 지원
- PacketEvents 2.0
- ViaVersion 호환

## 기여

이 프로젝트는 개인 포크입니다. 이슈와 PR은 적극적으로 관리되지 않습니다.

## 라이선스

GPLv3 — [LICENSE](LICENSE) 참조. Grim 코드가 복사된 수정된 바이너리 또는 플러그인은 비공개로 유지하거나 수취인에게 추가 비용 없이 전체 소스 코드를 제공해야 합니다.
