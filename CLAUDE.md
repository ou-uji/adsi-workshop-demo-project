# Attendance Demo Project

勤怠管理デモアプリ。モノレポ構成。

## Claude Code ハーネス

`.claude/` に rules / skills / agents を同梱（外部プラグイン不要）。詳細は [.claude/README.md](.claude/README.md)。

| やりたいこと | 参照先 |
|-------------|--------|
| 起動（ローカル / SageMaker） | `.claude/skills/dev-environment/SKILL.md` |
| SageMaker プレビュー設定 | `.claude/skills/sagemaker-code-editor/SKILL.md` |
| SageMaker から AWS デプロイ | `.claude/skills/sagemaker-aws-deploy/SKILL.md` |
| コーディング規約 | `.claude/rules/` |
| 開発の全体像（SDD / 仕様駆動開発） | `.claude/rules/common/development-process.md` |
| Issue 運用（要求/設計の永続化） | `.claude/rules/common/issue-workflow.md` |
| 要求仕様 | `.claude/skills/requirements/SKILL.md` |
| 設計 | `.claude/skills/design/SKILL.md` |
| 作業分割（UoW） | `.claude/skills/work-decomposition/SKILL.md` |
| TDD 実装 | `.claude/skills/tdd-implementation/SKILL.md` |
| コードレビュー | `.claude/skills/multi-agent-review/SKILL.md` |

> **スキルの起動方針（ワークショップ）**: SDD 工程スキル（`requirements` / `design` / `work-decomposition` / `tdd-implementation`）は **明示的に指定されたときのみ** 使用し、**自動では起動しない**。前半は参加者が自分でプロンプトを入力して各工程を体験し、後半で必要に応じてスキル名を指定して呼び出す。

### SageMaker クイックリファレンス

```bash
npm run dev:sagemaker        # 起動
npm run dev:sagemaker:stop   # 停止
```

アクセス: PORTS タブの地球儀 → URL の `ports` を `absports` に置換（例: `.../absports/3000/`）

## パッケージ構成

- `packages/backend/` — Spring Boot 3.x (Java 21)
- `packages/frontend/` — Next.js (TypeScript)
- `packages/infra/` — AWS CDK (TypeScript)。dev/prod
- `docs/path/` — デモの過程ドキュメント
- `docs/working/` — 要件・設計の Q&A 作業ドキュメント

## セットアップ

```bash
npm run setup
```

## docs/path ルール

デモの過程を `docs/path/` に番号付きファイル（`00-xxx.md`）で記録する。
新ステップに進んだら新ファイルを作成。プロンプト・やったこと・つまずき・最終構成を含める。

---

## プロジェクト現状（2026-07-13 時点キャッチアップ）

> このセクションはコード全体を読んで把握した現状スナップショット。詳細な仕様は `docs/requirements/`・`docs/design/`・`docs/units/` が一次情報。

### これは何か

SIer 向けの **AI 駆動開発（SDD / AI-DLC）+ TDD の実践デモ**。題材は勤怠管理システム。
「モダンな Java フルスタック開発をどう進めるか」を見せるためのワークショップ用リポジトリで、
`docs/path/00〜14` に要件定義→設計→UoW 分割→Phase A/B/C 実装→デプロイまでの全工程が記録されている。

### 完成度: 全 6 Unit 実装済み（ほぼフル機能）

要件（`docs/requirements/01-user-stories.md`）の全ドメインが Backend / Frontend とも実装されている。

| Unit | ドメイン | Backend | Frontend | 主な内容 |
|------|---------|:-------:|:--------:|---------|
| 00 | 基盤 | ✅ | ✅ | Actuator, Security, CORS, 例外ハンドリング, JPA Auditing |
| 01 | 部署 | ✅ | ✅ | CRUD（管理者） |
| 02 | 社員 | ✅ | ✅ | CRUD + 部署/ロールフィルタ + 論理削除（退職）+ 上長指定 |
| 03 | 認証 | ✅ | ✅ | セッションベース（Spring Security）, JSON ログイン, CSRF, ログイン/ログアウト/me |
| 04 | 打刻・勤怠 | ✅ | ✅ | 出退勤打刻, 当日ステータス, 履歴（日別/月別）, 自部署/全社員閲覧 |
| 05 | 勤怠修正 | ✅ | ✅ | 申請→承認/却下フロー, `@Version` 楽観ロック, 修正済みマーク |
| 06 | 月次集計・帳票 | ✅ | ✅ | 画面集計 + CSV + PDF（JasperReports） |

- Backend: Java 約 2,900 行 + テスト 29 クラス（ユニット/WebMvc/DataJpa/統合/ArchUnit）
- Frontend: TS/TSX 約 5,700 行、feature ベース構成（`src/features/*`）、TanStack Query + shadcn/base-ui + Tailwind
- Frontend テストは 7 ファイル（format, validators, api-client, MonthSelector, StatusBadge, MonthlySummary, LoginForm）— **Backend に比べ手薄**
- 勤務時間計算（休憩控除・残業）は `WorkDuration` VO に集約、労基法準拠ルールを実装

### 技術スタックの実態

- Backend: Spring Boot **3.5.0** / Java 21。**4.x 互換の書き方を徹底**（`SecurityFilterChain`, `@MockitoBean`, `requestMatchers` 等）。DB は PostgreSQL 17（本番/dev）、Flyway 管理（`ddl-auto` 禁止）
- Frontend: **Next.js 16 / React 19**。lint は Biome、テストは Vitest + Testing Library
- Infra: AWS CDK。ECS Fargate + ALB + CloudFront + S3 + RDS 想定。`.github/workflows/deploy.yml` で main push 時に自動デプロイ

### 起動プロファイル

- ローカル: PostgreSQL（Podman）+ `npm run boot` + `npm run dev`
- **ワークショップ用**: `--spring.profiles.active=workshop` で **H2 インメモリ**起動（DB 不要、`npm run boot:workshop`）
- SageMaker: `npm run dev:sagemaker`（フル basePath 復元プロキシ構成）
- デモアカウント（seed）: `admin@example.com` / `demo1234`（管理者）、`tanaka@example.com` 等 / `password123`（一般）

### 既知の課題・未対応

- **セッション管理**: JVM メモリ内保持 + ALB Sticky Session。デプロイ/再起動/スケールアウトでセッション消失 → 再ログインが必要。改善策は Spring Session + DynamoDB 外部化（`docs/design/05-infrastructure.md` 記載、未実装）
- **Frontend テストカバレッジが薄い**: ページ/feature コンポーネントの多くが未テスト。目標 80% には Backend 頼み
- **スコープ外（要件で明示）**: パスワード変更/リセット、代理承認、祝日カレンダー、修正前の値の履歴参照、シフト/給与/有給
- Operation フェーズ（本番運用・監視）はワークショップ範囲外
- オープンな GitHub Issue は現状なし（Issue 運用は仕組みだけ整備済み）

---

## セッション引き継ぎメモ（2026-07-13）

> 別ウィンドウで会話を再開するための引き継ぎ。次セッションはここを読めば状況を再現できる。

### いまアプリは起動中

`npm run dev:sagemaker` で 3 プロセスが**バックグラウンド稼働中**（backend :8080 / frontend next start :3001 / proxy :3000）。
- health 確認: `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health` → 200
- ブラウザは `https://<studio-domain>/codeeditor/default/absports/3000/` を**直接**開く（PORTS の地球儀 `ports` は不可、必ず `absports`）
- 停止: `npm run dev:sagemaker:stop`

### このセッションでやったこと

1. コードベース全体をキャッチアップし、本 CLAUDE.md に「プロジェクト現状」セクションを追記（↑）
2. SageMaker でアプリ起動。**つまずき**: `packages/frontend` の依存が未インストールでビルド失敗 → `cd packages/frontend && npm install` で解消（既知の落とし穴）
3. UI 変更デモ（出勤ボタンを緑に / 勤怠履歴見出しを「今月の勤怠」に）を実施 → 確認後に `git checkout` で**元に戻し済み**
4. **【バグ修正デモ開始】** スライド41「バグの例（回答例）」に沿って、仕込まれたバグを順に解消していくフェーズに入った

### 仕込みバグ修正の進捗（スライド41）

| # | バグ | 状態 | 対応 |
|---|------|:----:|------|
| ① | 社員編集で部署・ロールが名称でなく内部コード/ID 表示 | ✅ 修正・目視確認済み | 下記参照 |
| ② | 出勤ボタンが二重に押せる（打刻の状態管理） | ✅ 修正・目視確認済み（TDD） | 下記参照 / Issue #1 / PR |
| ③ | ログアウト後に画面が遷移しない（※SageMaker都合で難しい） | ⬜ 未着手 | ヒント: ログアウト処理後のリダイレクト有無 |

> ※上記以外にもバグが潜んでいる可能性あり。見つけたら Issue 起票を推奨（Issue 運用は仕組みだけ整備済み）。

#### バグ① 修正内容（完了）

- **対象**: [packages/frontend/src/features/employee/EmployeeFormDialog.tsx](packages/frontend/src/features/employee/EmployeeFormDialog.tsx)
- **症状**: 社員管理 → 編集（鉛筆アイコン）のポップアップで、部署が UUID（例 `a0000000-...-001`）、ロールが `ADMIN` のまま表示される
- **原因**: UI は Base UI (`@base-ui/react/select`)。`SelectValue` はポップアップ（Portal）内の `SelectItem` テキストからラベルを解決するが、**一度も開いていないと Item が DOM に無く**、生の value（UUID / enum）がそのまま表示される
- **修正**: `SelectValue` の children render prop で **値→表示名の明示マッピング**を追加
  - 部署: `departments` 配列から `id` で `name` を引き当て
  - ロール: `{ ADMIN: "管理者", EMPLOYEE: "一般" }`
- **確認**: `npx next build` 成功 → `npm run dev:sagemaker` 再ビルド → ブラウザで「開発部 / 管理者」表示を目視確認

#### バグ② 修正内容（完了・TDD）

- **起点**: GitHub Issue #1（テンプレートに沿って起票 → Plan mode で原因調査 → コメント追記 → 壁打ち → TDD 実装）
- **症状**: 打刻画面で出勤後も「出勤」ボタンが押せ連続打刻でき、その後退勤できなくなる
- **原因（2 箇所）**:
  - Frontend `ClockButtons.tsx:66`: `canClockIn` の論理式 `status === "NOT_CLOCKED_IN" || status !== "CLOCKED_OUT"` が実質常に true（主因）
  - Backend `AttendanceServiceImpl.clockIn()`: 二重打刻ガードが無く未退勤レコードが複数化 → `clockOut` が `Optional` 前提で `NonUniqueResultException` → 退勤不可（副因）
- **修正（二重防御）**:
  - Frontend: `canClockIn = status === "NOT_CLOCKED_IN"` に修正
  - Backend: `clockIn()` 冒頭で当日の未退勤レコードを確認し、あれば `409 "Already clocked in"`（既存 `findBy...ClockOutIsNull` を再利用）
- **回帰テスト**: Backend（Service ユニット + 統合で「出勤中に再 clockIn→409」）、Frontend（`ClockButtons.test.tsx` で 3 状態のボタン活性）
- **検証**: Backend attendance 34件 green / Frontend 56件 green / 実 API で 201→409→200 実証 / ブラウザ目視確認
- **記録**: [process/20260713/](process/20260713/) に `Bug2_修正方針.md` / `Bug2_TDD.md` / `Bug2_テスト検証.md`
- **ハマり所（記録済み）**: Frontend テストの DOM 未クリーンアップ（`globals:false` で auto-cleanup 未登録 → `afterEach(cleanup)` で対処）。`PdfExportServiceTest` の失敗は既存の環境依存問題（JasperReports グラフィック環境）でデグレ無関係

### 未コミットの変更（`git status`）

**バグ① 修正**（`EmployeeFormDialog.tsx` + CLAUDE.md + package-lock）は `main` にコミット済み（`f26f2f0..7e78fbf`）。

**バグ② 修正**は作業ブランチ `bug-fix/bug2-double-clock-in` 上でコミット → PR 作成済み（`Closes #1`）。以下を含む:
- `packages/backend/.../AttendanceServiceImpl.java`（二重打刻ガード）
- `packages/backend/.../AttendanceServiceTest.java` / `AttendanceIntegrationTest.java`（回帰テスト）
- `packages/frontend/.../ClockButtons.tsx`（`canClockIn` 修正）+ `ClockButtons.test.tsx`（新規）
- `process/20260713/`（Bug2 の方針・TDD・検証ノウハウ 3 ファイル）
- `CLAUDE.md`（本進捗表の更新）

> PR マージはユーザーが実施。マージ後に `main` へ反映される。

### 次セッションでの注意

- `dev:sagemaker` は**本番ビルド構成**（`next start`）。UI を変えたら毎回 `npm run dev:sagemaker` で再ビルドしないと画面に反映されない（HMR 無し）
- frontend は依存インストール済みなので、以降の再起動は `npm run dev:sagemaker` のみで OK
- Git identity は repo-local 設定済み（`ou-uji` / `--global` は未変更）
- 次はバグ③（ログアウト後に画面遷移しない ※SageMaker 都合で難しい）
