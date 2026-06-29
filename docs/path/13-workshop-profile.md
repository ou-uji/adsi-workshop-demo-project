# 13: workshop プロファイル追加（H2 インメモリ DB）

## プロンプト

> SageMaker Studio で Docker が使えるか調査して、ダメなら H2 で動くようにして

## 調査

### SageMaker Studio の Docker サポート調査

Qiita 記事と AWS 公式ドキュメントを確認した結果、Studio の Docker サポートには以下の制約があることが判明：

| 機能 | サポート |
|------|----------|
| Docker CLI / Docker Compose CLI | OK |
| Docker networks | NG |
| Docker volumes（named） | NG（bind mount のみ） |
| ポートマッピング (-p) | NG |

現在の `compose.yaml` は named volume と port mapping を使っており、そのままでは動かない。

### 判断

Docker 対応の修正（volume → bind mount、ports 削除、network 制約対応）は可能だが：
- ドメイン設定で Docker 有効化が必要（管理者作業）
- 当日のトラブルリスクが高い
- ワークショップの目的は AI 駆動開発体験であり、DB セットアップではない

→ H2 インメモリ DB で Docker 不要にする方針を採用。

## やったこと

### 1. build.gradle.kts 修正

- `testRuntimeOnly("com.h2database:h2")` → `runtimeOnly("com.h2database:h2")` に変更
- Flyway の H2 対応モジュール（`flyway-community-db-support`）は Flyway 11 には存在しなかったが、Flyway core だけで H2 に対応できた

### 2. マイグレーション SQL 修正

全4ファイルの `TIMESTAMPTZ` を `TIMESTAMP WITH TIME ZONE` に一括置換：

- `V1__create_departments.sql`
- `V2__create_employees.sql`
- `V3__create_attendance_records.sql`
- `V4__create_attendance_corrections.sql`

H2 の PostgreSQL 互換モード（`MODE=PostgreSQL`）でも `TIMESTAMPTZ` は認識されないため。`TIMESTAMP WITH TIME ZONE` は SQL 標準構文で PostgreSQL でも同義。

### 3. application-workshop.yaml 作成

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:attendance;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
```

- Flyway 有効（マイグレーション + シードデータ自動適用）
- H2 Console 有効（`/h2-console` でブラウザから DB 操作可能）

### 4. logback-spring.xml 修正

workshop プロファイル用の appender がないと Spring Boot が起動時に無言で落ちた。dev と同じコンソール出力の appender を追加。

### 5. package.json にスクリプト追加

```json
"boot:workshop": "cd packages/backend && ./gradlew bootRun --args='--spring.profiles.active=workshop'"
```

## つまずき

### Flyway H2 モジュール問題

`flyway-database-h2` は存在せず、`flyway-community-db-support` は Flyway 10.x 系までしかリリースされていなかった。結果的に Flyway 11 の core モジュールだけで H2 のマイグレーションが動作した。

### TIMESTAMPTZ 非対応

H2 の PostgreSQL モード（`MODE=PostgreSQL`）でも `TIMESTAMPTZ` は型エイリアスとして認識されず、`Unknown data type: "TIMESTAMPTZ"` エラーが発生。SQL 標準の `TIMESTAMP WITH TIME ZONE` に書き換えて解決。

### logback 無言クラッシュ

logback-spring.xml に workshop プロファイル用の設定がないと、appender が存在しないため Spring Boot が起動直後にログ出力なしで exit code 1 で終了。エラーメッセージが一切出ないため原因特定に時間がかかった。

## 動作確認

- `npm run boot:workshop` で起動成功（3.2秒）
- Flyway マイグレーション 5本（V1〜V4 + V1000 シード）全成功
- `actuator/health` → `{"status":"UP"}`
- 既存テスト全パス（リグレッションなし）

## ドキュメント更新

- `docs/design/02-db-design.md` — `timestamptz` → `TIMESTAMP WITH TIME ZONE` に統一、H2 互換の注記追加
- `docs/design/05-infrastructure.md` — ワークショップ環境セクション追加
