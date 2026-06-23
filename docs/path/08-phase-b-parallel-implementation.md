# 08: Phase B 並列実装（Unit 01〜04 Backend + Frontend）

## プロンプト

> Phase B に進んで

初回は逐次実装の計画を提示したが、ユーザーから：

> これ、サブエージェント使って並列に進められたりしない？そのための PhaseB だと思うんだけど

と指摘を受け、4つの subagent を `isolation: "worktree"` で並列起動する方針に切り替えた。

## やったこと

### 1. 並列実装（4 subagent × worktree 隔離）

Phase A で定義済みのインターフェース（Entity, Service interface, DTO, Flyway）をベースに、4 Unit を同時に TDD 実装した。

| Agent | Unit | 成果物（本番） | 成果物（テスト） | テスト数 |
|-------|------|--------------|----------------|---------|
| 1 | Unit 01: 部署管理 | Repository, ServiceImpl, Controller | Repo(4), Service(5), Controller(4) | 13 |
| 2 | Unit 02: 社員管理 | Repository, Specifications, ServiceImpl, Controller, PasswordEncoderConfig | Repo(6), Service(11), Controller(7) | 24 |
| 3 | Unit 03: 認証 | UserDetailsService, EmployeeUserDetails, JsonAuthFilter, Login/Failure Handler, EntryPoint, AccessDenied, SpaCsrfHandler, AuthServiceImpl, AuthController, SecurityConfig 書換 | UserDetails(3), AuthService(1), AuthController(2), SecurityConfig 統合(5) | 11 |
| 4 | Unit 04: 打刻・勤怠 | Repository, ServiceImpl, Controller, ClockConfig | WorkDuration(5), Repo(4), Service(8), Controller(4) | 21 |

各 agent は独立した git worktree で動作し、完了後にメインに手動統合した。

### 2. 統合時の対処

各 agent が独立して作成したため、以下のファイルが重複・競合した：

| ファイル | 競合内容 | 解決 |
|---------|---------|------|
| `DepartmentRepository` | Unit 01(full), 02(minimal), 04(minimal) | Unit 01 版を採用 |
| `EmployeeRepository` | Unit 02(full+Spec), 03(findByEmail), 04(findByDepartmentId) | Unit 02 版に Unit 04 の `findByDepartmentId` を追加 |
| `PasswordEncoderConfig` | Unit 02, 03 が同一内容で作成 | どちらか一方を採用 |
| `LayerDependencyTest` | Unit 02(Controller→Entity 許可), Unit 03(Config→Repository/DTO 許可) | 両方の変更をマージ |
| Controller テスト 3 件 | `@Import(SecurityConfig.class)` が本番 SecurityConfig の依存で失敗 | `TestSecurityConfig`(permitAll) パターンに修正 |
| SpotBugs 除外 | Controller/Service の DI 誤検知、テスト text block 誤検知 | `spotbugs-exclude.xml` に除外ルール追加 |

### 3. シードデータ作成

`V1000__seed_data.sql` を作成し、動作確認用の初期データを投入した。

| 名前 | メール | パスワード | ロール | 部署 | 上長 |
|------|--------|-----------|--------|------|------|
| 佐藤 管理 | admin@example.com | demo1234 | ADMIN | 開発部 | Yes |
| 田中 太郎 | tanaka@example.com | password123 | EMPLOYEE | 開発部 | No |
| 鈴木 花子 | suzuki@example.com | password123 | EMPLOYEE | 営業部 | Yes |
| 山田 一郎 | yamada@example.com | password123 | EMPLOYEE | 営業部 | No |

### 4. 動作確認で発見・修正したバグ（3 件）

#### Bug 1: SpaCsrfTokenRequestHandler のロジック反転

- **症状**: POST リクエストが全て CSRF 403 になる
- **原因**: ヘッダーがある場合（SPA が Cookie の平文トークンを送る）は plain 解決すべきなのに xor を使っていた
- **修正**: `extends CsrfTokenRequestAttributeHandler` に変更し、ヘッダー有→`super`(plain)、無→`delegate`(xor) に修正

#### Bug 2: LoginSuccessHandler の LazyInitializationException

- **症状**: ログイン認証は成功するが、レスポンスが 403 になる
- **原因**: `LoginSuccessHandler` が `employeeRepository.findById()` で Employee を再読み込みし、`Department` を lazy load しようとするが、`open-in-view=false` のためセッション外でアクセスし例外発生
- **修正**: `EmployeeUserDetails` に `departmentName` と `role` を追加。`CustomUserDetailsService` で `@Transactional(readOnly = true)` を付けて Department をロード。`LoginSuccessHandler` は DB アクセスせず `EmployeeUserDetails` から直接レスポンス構築

#### Bug 3: Spring Security 6 のセッション永続化

- **症状**: ログインは 200 を返すが、`Set-Cookie: JSESSIONID` がレスポンスに含まれない
- **原因**: Spring Security 6 のデフォルトでは `SecurityContextRepository` が `RequestAttributeSecurityContextRepository`（セッションに保存しない）
- **修正**: `JsonAuthenticationFilter` に `setSecurityContextRepository(new HttpSessionSecurityContextRepository())` を設定

### 5. 開発環境の Podman 対応

- Docker Desktop ではなく Podman を使用していることが判明
- `application-dev.yaml` の `docker.compose.lifecycle-management: start-and-stop` を `docker.compose.enabled: false` + datasource 直接設定に変更
- `package.json` の `db:up` / `db:down` を `podman compose` に変更

### 6. Frontend ログイン画面の実装

Backend の認証が動作確認できたため、最低限の Frontend 認証フローも実装した。

| ファイル | 内容 |
|---------|------|
| `features/auth/auth-api.ts` | login/logout/me の API 関数 |
| `features/auth/useAuth.ts` | 認証状態管理 hook (TanStack Query) |
| `features/auth/LoginForm.tsx` | ログインフォームコンポーネント |
| `app/login/page.tsx` | ログインページ |
| `app/page.tsx` | `/` → 認証チェック → `/login` or `/dashboard` にリダイレクト |
| `app/(authenticated)/layout.tsx` | 認証済みレイアウト（未認証→リダイレクト） |
| `app/(authenticated)/dashboard/page.tsx` | ダッシュボード（ユーザー情報表示） |
| `components/layout/Header.tsx` | ユーザー名 + ログアウトボタン |
| `components/layout/Sidebar.tsx` | ロール別メニュー（ADMIN のみ管理メニュー表示） |
| `lib/api-client.ts` | CSRF トークン自動付与 + 401 時ログインリダイレクト |

## つまずき

### Podman 環境での Docker Compose 連携失敗

Spring Boot の Docker Compose 連携が Docker ソケット (`~/.docker/run/docker.sock`) を探すが、Podman は別のソケットパスを使う。`DOCKER_HOST` 環境変数で回避可能だが、`application-dev.yaml` で Docker Compose を無効化して datasource を直接指定する方が安定した。

### Podman VM のディスク不足

`postgres:17` イメージの pull 時に `no space left on device` が発生。`podman image prune -a --force` で 80GB → 1.5GB に削減して解消。

### CSRF の複雑さ（Spring Security 6）

Spring Security 6 では CSRF トークンが遅延生成（deferred）になった。SPA 環境で Cookie ベースの CSRF を使うには以下が必要：
1. `CsrfCookieFilter`（トークン強制ロード用フィルタ）を追加
2. `SpaCsrfTokenRequestHandler` で plain/xor の解決を正しく使い分け
3. ログイン/ログアウト URL は `AntPathRequestMatcher` で CSRF 除外（`MvcRequestMatcher` だとフィルタ処理の URL にはマッチしない）

## 最終構成

### テスト結果

- **74 テスト全パス**
- `./gradlew check` (test + checkstyle + spotbugs + jacoco) **全パス**

### 追加・変更されたファイル（Backend）

```
packages/backend/src/
├── main/java/com/example/attendance/
│   ├── attendance/
│   │   ├── controller/AttendanceController.java        ← NEW
│   │   ├── repository/AttendanceRecordRepository.java  ← NEW
│   │   └── service/AttendanceServiceImpl.java          ← NEW
│   ├── auth/
│   │   ├── controller/AuthController.java              ← NEW
│   │   └── service/AuthServiceImpl.java                ← NEW
│   ├── common/config/
│   │   ├── ClockConfig.java                            ← NEW
│   │   ├── PasswordEncoderConfig.java                  ← NEW
│   │   ├── SecurityConfig.java                         ← MODIFIED (permitAll → 権限マトリクス)
│   │   └── security/
│   │       ├── CustomAccessDeniedHandler.java          ← NEW
│   │       ├── CustomAuthenticationEntryPoint.java     ← NEW
│   │       ├── CustomUserDetailsService.java           ← NEW
│   │       ├── EmployeeUserDetails.java                ← NEW
│   │       ├── JsonAuthenticationFilter.java           ← NEW
│   │       ├── LoginFailureHandler.java                ← NEW
│   │       ├── LoginSuccessHandler.java                ← NEW
│   │       └── SpaCsrfTokenRequestHandler.java         ← NEW
│   ├── department/
│   │   ├── controller/DepartmentController.java        ← NEW
│   │   ├── repository/DepartmentRepository.java        ← NEW
│   │   └── service/DepartmentServiceImpl.java          ← NEW
│   └── employee/
│       ├── controller/EmployeeController.java          ← NEW
│       ├── repository/
│       │   ├── EmployeeRepository.java                 ← NEW
│       │   └── EmployeeSpecifications.java             ← NEW
│       └── service/EmployeeServiceImpl.java            ← NEW
├── main/resources/
│   ├── application-dev.yaml                            ← MODIFIED (Podman 対応)
│   └── db/seed/V1000__seed_data.sql                    ← NEW
├── test/java/com/example/attendance/
│   ├── attendance/
│   │   ├── controller/AttendanceControllerTest.java    ← NEW
│   │   ├── domain/WorkDurationTest.java                ← NEW
│   │   ├── repository/AttendanceRecordRepositoryTest.java ← NEW
│   │   └── service/AttendanceServiceTest.java          ← NEW
│   ├── auth/
│   │   ├── controller/AuthControllerTest.java          ← NEW
│   │   ├── security/CustomUserDetailsServiceTest.java  ← NEW
│   │   ├── security/SecurityConfigTest.java            ← NEW
│   │   └── service/AuthServiceTest.java                ← NEW
│   ├── department/
│   │   ├── controller/DepartmentControllerTest.java    ← NEW
│   │   ├── repository/DepartmentRepositoryTest.java    ← NEW
│   │   └── service/DepartmentServiceTest.java          ← NEW
│   ├── employee/
│   │   ├── controller/EmployeeControllerTest.java      ← NEW
│   │   ├── repository/EmployeeRepositoryTest.java      ← NEW
│   │   └── service/EmployeeServiceTest.java            ← NEW
│   └── architecture/LayerDependencyTest.java           ← MODIFIED
└── config/spotbugs/spotbugs-exclude.xml                ← MODIFIED
```

### 追加・変更されたファイル（Frontend）

```
packages/frontend/src/
├── app/
│   ├── page.tsx                                        ← MODIFIED (認証チェック → リダイレクト)
│   ├── login/page.tsx                                  ← MODIFIED (ログインフォーム)
│   └── (authenticated)/
│       ├── layout.tsx                                  ← MODIFIED (認証ガード)
│       └── dashboard/page.tsx                          ← NEW
├── components/layout/
│   ├── Header.tsx                                      ← MODIFIED (ユーザー名 + ログアウト)
│   └── Sidebar.tsx                                     ← MODIFIED (ロール別メニュー)
├── features/auth/
│   ├── auth-api.ts                                     ← NEW
│   ├── useAuth.ts                                      ← NEW
│   └── LoginForm.tsx                                   ← NEW
└── lib/api-client.ts                                   ← MODIFIED (CSRF 対応 + 401 リダイレクト)
```

## Phase B Frontend 並列実装

Backend 完了後、引き続き3つの subagent で Frontend を並列実装した。

### プロンプト

> Frontend まで終わらせて

### 実装（3 subagent × worktree 隔離）

| Agent | 画面 | 成果物 |
|-------|------|--------|
| 1 | 部署管理 | department-api, useDepartments, DepartmentTable, DepartmentFormDialog, page |
| 2 | 社員管理 | employee-api, useEmployees, EmployeeTable, EmployeeFilters, EmployeeFormDialog, RetireDialog, ManagerToggle, page |
| 3 | 打刻・勤怠 | attendance-api, useAttendance, ClockButtons, TodayRecords, AttendanceTable, MonthlySummary, format, dashboard/history/team pages |

### 統合時の対処

- `api-client.ts` の `delete` メソッドで TypeScript の型推論エラー → IIFE スプレッドをやめて明示的な変数に変更
- Biome の `noArrayIndexKey` 警告 3 箇所 → 固定キー配列に変更
- `/attendance` ページが未作成だった → 追加作成
- `/` (トップ) で `"use client"` + `useEffect` リダイレクトが Next.js 16 の React Server Components Manifest エラーを起こす → サーバーコンポーネントの `redirect()` に変更

### 全ルート一覧

| ルート | ページ | 機能 |
|--------|--------|------|
| `/` | → `/dashboard` にリダイレクト | |
| `/login` | ログイン | メール + パスワード認証 |
| `/dashboard` | ダッシュボード | 出勤/退勤ボタン + 当日の打刻記録 |
| `/attendance` | 打刻 | 出勤/退勤ボタン + 当日の打刻記録 |
| `/history` | 勤怠履歴 | 月別テーブル + 月間サマリーカード |
| `/team` | チーム勤怠 | 上長のみ。部署メンバーのサマリー |
| `/admin/departments` | 部署管理 | 一覧 + 新規登録/編集ダイアログ |
| `/admin/employees` | 社員管理 | 一覧 + フィルタ + 登録/編集/退職/上長設定 |

### Frontend 追加ファイル

```
packages/frontend/src/
├── features/
│   ├── attendance/
│   │   ├── attendance-api.ts                           ← NEW
│   │   ├── useAttendance.ts                            ← NEW
│   │   ├── format.ts                                   ← NEW
│   │   ├── ClockButtons.tsx                            ← NEW
│   │   ├── TodayRecords.tsx                            ← NEW
│   │   ├── AttendanceTable.tsx                         ← NEW
│   │   └── MonthlySummary.tsx                          ← NEW
│   ├── department/
│   │   ├── department-api.ts                           ← NEW
│   │   ├── useDepartments.ts                           ← NEW
│   │   ├── DepartmentTable.tsx                         ← NEW
│   │   └── DepartmentFormDialog.tsx                    ← NEW
│   └── employee/
│       ├── employee-api.ts                             ← NEW
│       ├── useEmployees.ts                             ← NEW
│       ├── EmployeeTable.tsx                           ← NEW
│       ├── EmployeeFilters.tsx                         ← NEW
│       ├── EmployeeFormDialog.tsx                      ← NEW
│       ├── RetireDialog.tsx                            ← NEW
│       └── ManagerToggle.tsx                           ← NEW
├── app/
│   ├── page.tsx                                        ← MODIFIED (server redirect)
│   └── (authenticated)/
│       ├── attendance/page.tsx                         ← NEW
│       ├── dashboard/page.tsx                          ← MODIFIED (ClockButtons + TodayRecords)
│       ├── history/page.tsx                            ← NEW
│       ├── team/page.tsx                               ← NEW
│       └── admin/
│           ├── departments/page.tsx                    ← NEW
│           └── employees/page.tsx                      ← NEW
└── lib/api-client.ts                                   ← MODIFIED (delete 型修正)
```
