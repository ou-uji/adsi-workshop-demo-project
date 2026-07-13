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
| ② | 出勤ボタンが二重に押せる（打刻の状態管理） | ✅ 完了・main マージ済み（TDD） | 下記参照 / Issue #1（Closed）/ PR #2（Merged） |
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

### Git 状態（2026-07-13 バグ② マージ後）

- **`main` が最新**。バグ①（`7e78fbf`）とバグ②（PR #2 マージ `0c20051`）を反映済み。ローカル `main` は `origin/main` と同期済み。
- **作業ブランチ `bug-fix/bug2-double-clock-in` は削除済み**（ローカル・リモートとも）。
- **未コミットの変更なし**（この CLAUDE.md 更新を除く。更新後にコミット予定）。
- Issue #1 は PR #2 の `Closes #1` で **自動クローズ済み**。オープンな Issue は無し。

---

## ハンズオン2: 新規機能「打刻メモ（備考）入力」— ✅ 完了・main マージ済み（2026-07-13）

> バグ③は **AWS/SageMaker 環境都合で難しいため後回し**。代わりにハンズオン2（新規機能を SDD で作る）に着手。
> 「まだ無い機能」を、あいまいな要件から AI と壁打ちして仕様を固める体験がテーマ。

### 機能概要

打刻（出勤・退勤）時に任意の**備考メモ**を残せるようにする。履歴画面で閲覧＋本人がその場で編集・削除。

### 確定仕様（ゲート①通過）

任意入力 / 200文字上限 / 出退勤**両方** / 承認不要・本人がその場で編集削除 / 閲覧は本人＋上長＋管理者 /
編集は本人のみ / 履歴画面の行から操作 / DB は **`clock_in_memo`・`clock_out_memo` の2カラム**分離。

### 設計判断（ゲート②、既存パターン準拠）

- 打刻: クエリ `employeeId` 維持 + JSON body `{memo}`（`createCorrection` 先例）。body なしでも 201（**後方互換**）
- 編集: `PATCH /api/attendance/records/{id}/memo` で出退勤メモをまとめて更新、削除は空→null
- 認可: サービス層で `record.getEmployee().getId()` 比較（`validateApprover` パターン、403）
- 楽観ロック: 既存 `@Version` + 手動 `validateVersion`（409）
- DB は `V5__add_memo_to_attendance_records.sql`（`ddl-auto` 禁止のため必須）

### SDD ドキュメント（`process/20260713/`）

| ファイル | 工程 |
|---|---|
| `新規機能_打刻メモ_QA.md` | 壁打ち Q&A（Q1〜Q10、全 [Answer] 済） |
| `新規機能_打刻メモ_仕様(SDD).md` | 確定仕様・ゲート① |
| `新規機能_打刻メモ_設計.md` | 実装設計・ゲート② |
| `新規機能_打刻メモ_テスト仕様(TDD).md` | テスト仕様（仕様→Redテストの対応表） |
| `新規機能_打刻メモ_レビュー結果.md` | 実装前の一次レビュー（Approve） |
| `新規機能_打刻メモ_PRレビュー結果.md` | PR #3 の再レビュー（実装を知らないレビュアー視点、Approve） |

### 進捗（全工程完了）

- ✅ ブランチ `feature/attendance-memo` 作成（main 最新から）
- ✅ 要求 Q&A → 仕様確定（ゲート①）→ 設計・Plan（ゲート②）→ TDD テスト仕様
- ✅ **TDD 実装（Red → Green → Refactor）完了**。Backend + Frontend 全実装
- ✅ multi-agent-review 実施（Approve）。指摘対応: `blankToNull` に trim 追加、対称/境界値テスト追加。HIGH「employeeId セッション非照合」は既存アーキ由来で現状維持（known-issue 記録）
- ✅ **実機検証済み**（実 API）: メモ付き打刻201・編集200・201字400・古いversion409・他人編集403・履歴反映・trim。Backend attendance テスト全 green / Frontend 26件 green / tsc・build 成功
- ✅ **PR #3 作成 → 再レビュー（スライド62「実装を知らないレビュアー」視点）**。subagent の CRITICAL 1・HIGH 2 を親が実コードで裏取りし全棄却（trim は Backend `blankToNull` で吸収／team・all は `AttendanceTable` 未使用で編集導線なし）。判定 **Approve**、残りは任意の LOW/MEDIUM
- ✅ **ユーザー動作確認 → Approve → PR #3 マージ（`5f103c9`、fast-forward）**。リモート/ローカルブランチ削除済み。PR レビュー結果を main に記録（`c7ac8c3`）
- ※ 今回は**時間の都合で Issue 起票はスキップ**（process ドキュメントで代替記録）。process に QA/仕様/設計/テスト仕様/一次レビュー/PRレビューの6点を記録

### 実装の要点（設計ドキュメント参照）

- Backend: V5 migration / Entity 2カラム / DTO 拡張（version 露出）/ `ClockRequest`・`MemoUpdateRequest` /
  Service に memo 保存 + `updateMemo`（所有者チェック・version）/ Controller 拡張 + PATCH / SecurityConfig 確認
- Frontend: 型・API 拡張 / hook（`useUpdateMemo` 新規・`onError` toast 追加）/ ClockButtons に textarea /
  AttendanceTable に備考列 + 編集ボタン（team/all では非表示）/ `MemoEditDialog` 新規（FormDialog 再利用）
- フロントは zod/react-hook-form なし・native validation・生 textarea・invalidate のみ（楽観更新なし）

---

## ハンズオン3: 新規機能「有給休暇申請」— ✅ 完了・PR #4 作成済み（レビュー/マージ待ち）（2026-07-13）

> スライド⑤「大きめの機能を追加してみよう」。お題は **有給休暇申請機能**。
> 既存の **勤怠修正（correction / Unit 05）** と同じ「申請 → 承認/却下 → 反映」の型を、**SDD 工程スキルにプロセスを任せて**再現する体験。
> **研修用デモのため短時間で作る・完成しなくても OK**。今回は **ユーザーレビューなしで NonStop で PR 作成まで**進めた（ただし AI 自身のレビュー＋テスト検証は飛ばさず、CRITICAL エラー・デグレ無しを確認済み）。
> **成果**: 全 SDD 工程（要求→設計→TDD→検証→レビュー）を各スキルで完走 → **PR #4**（https://github.com/ou-uji/adsi-workshop-demo-project/pull/4 、3コミット）作成。実機（実 API）でも承認フロー全ケース実証済み。

### ブランチ

`feature/skill-auto-dev`（main 最新 `e404e6c` から作成）。

### 確定仕様（ゲート①通過）

correction に寄せて**最小に倒した**（芋づる式に重くなる要素は全部スコープ外）:

- 残日数管理**なし** / **1日単位（全休）** / **単一日** / **有給のみ**（種別カラムなし）
- 承認者=**申請者と同一部署の上長**（`isManager`、自己承認可）・ADMIN 特権承認なし
- 却下理由**必須**（≤500字）/ 取り下げ**なし** / 対象日**制約なし** / 重複チェック**なし**
- 反映=**`leave_requests` に APPROVED で残すだけ**（`attendance_records`・集計は触らない＝波及ゼロ）
- 閲覧=**本人＋自部署上長**（管理者全社一覧なし）/ 楽観ロック=**@Version＋version チェック（409）**
- 新テーブル `leave_requests`（Flyway **V6**）/ 新パッケージ `com.example.attendance.leave` / `src/features/leave/`

> **拡張性の記録**: 「拡張が要るなら各 QA の別選択肢で再設計」の指示に基づき、捨てた選択肢を QA 各問の **「▸ 拡張時」** と仕様書末尾の **「将来拡張マトリクス」** に記録済み（残日数=Q1・履歴/集計反映=Q9 が波及最大）。

### SDD ドキュメント（`process/20260713/`）

| ファイル | 工程 | 状態 |
|---|---|---|
| `新規機能_有給休暇申請_QA.md` | 壁打ち Q&A（Q1〜Q13、全 [Answer] 済・拡張メモ付） | ✅ |
| `新規機能_有給休暇申請_仕様(SDD).md` | 確定仕様・ゲート①・将来拡張マトリクス | ✅ |
| `新規機能_有給休暇申請_設計.md` | 実装設計・ゲート② | ✅ |
| `新規機能_有給休暇申請_テスト仕様(TDD).md` | テスト仕様（仕様→Redテスト対応表） | ✅ |
| `新規機能_有給休暇申請_レビュー結果.md` | AI 自己レビュー結果（4 reviewer 並列、Approve） | ✅ |

### ユーザーストーリー

- LEAVE-01 社員: 有給を申請（対象日・理由）/ LEAVE-02 社員: 自分の申請状態確認
- LEAVE-03 上長: 自部署の申請一覧 / LEAVE-04 上長: 承認・却下（却下理由必須）
- LEAVE-05 承認者=同一部署の上長（自己承認可）/ LEAVE-06 承認/却下の競合を楽観ロック制御

### 進捗（全工程完了）

- ✅ ブランチ作成 → 要求 Q&A（`/requirements`）→ 仕様確定（ゲート①）
- ✅ 設計（`/design`）ゲート② → テスト仕様（TDD）
- ✅ **TDD 実装完了**。Backend（`com.example.attendance.leave` + V6 migration + SecurityConfig 追記）/ Frontend（`src/features/leave/` + 3ページ + Sidebar 2項目）
- ✅ 検証（`/verify`）: `./gradlew check` 211 green（`PdfExportServiceTest` のみ既存の環境依存で失敗・本機能と無関係）、ArchUnit green、`next build` 成功、Biome 新規ファイル整形済み
- ✅ **`/multi-agent-review`（java/typescript/security/test の4 subagent 並列）→ Approve**。test-reviewer の HIGH 2件（理由500字境界値・他人/他部署 isolation テスト欠落）を親が実コードで裏取り→正当と判断し、Backend テストを 21→**26件**に強化して解消。`user!.id`（ts）と `@RequestParam` アイデンティティ（security）は correction と同一の既存パターンのため現状維持
- ✅ **実機検証（実 API / H2 workshop）**: 申請201 / 理由501字→400 / 自分の一覧 / 他部署上長の承認→403 / 承認待ち一覧 / 古い version→409 / 承認→200 APPROVED / 却下理由空→400 / 却下→200 REJECTED / **承認後も勤怠レコードが作られない（Q9=A）** を curl で全実証
- ✅ **PR #4 作成**（3コミット: `feat(leave)` / `docs(leave)` process / `docs(claude)`）。ユーザーが画面で機能追加を目視確認済み

### テスト実績

- Backend leave テスト **26件** green（Repository 3 / Service 16 / Controller 7）
- Frontend leave テスト **7件** green（LeaveRequestForm 4 / LeaveApprovalActions 3）
- ※ 研修時間の都合で **Issue 起票はスキップ**、process ドキュメント5点で代替記録

### 実装の要点（correction からの差分）

- **承認の副作用が無い**のが correction との最大の差: correction は AttendanceRecord を更新/新規作成するが、leave の `approve()` は `status=APPROVED` にするだけ（`AttendanceRecordRepository` を DI しない設計で型として担保）
- Entity は `targetDate` + `reason` のみ（correction の時刻2カラム・attendanceRecord 関連を削除）
- 承認画面は既存 `/approvals`（勤怠修正）と混ざらないよう **別ルート `/leave-approvals`**
- `@DataJpaTest` のハマり所: 監査カラム（`created_at`）を埋めるには `@Import(JpaAuditingConfig.class)` が必須（既存 `EmployeeRepositoryTest` に倣う）

### 次セッションでの注意（有給機能）

- **PR #4 はまだ OPEN**（レビュー/マージ待ち）。マージ後は **main 同期 + `feature/skill-auto-dev` ブランチ削除**（ローカル・リモート）が残タスク。
- バグ③（ログアウト後に画面遷移しない ※SageMaker 都合で難しい）は依然として未着手のまま。

---

### 次セッションでの注意

- `dev:sagemaker` は**本番ビルド構成**（`next start`）。UI を変えたら毎回 `npm run dev:sagemaker` で再ビルドしないと画面に反映されない（HMR 無し）
- frontend は依存インストール済みなので、以降の再起動は `npm run dev:sagemaker` のみで OK
- Git identity は repo-local 設定済み（`ou-uji` / `--global` は未変更）
- **バグ③**（ログアウト後に画面遷移しない ※SageMaker 都合で難しい）は後回し。ヒント: ログアウト処理後のリダイレクト有無。
- ワークフロー確立済み: **専用ブランチを切る → TDD → PR（`Closes #<issue>`）→ ユーザーがマージ → main 同期 + ブランチ削除**。process 記録は `process/YYYYMMDD/` に残す。
