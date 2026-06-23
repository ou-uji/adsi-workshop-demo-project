# 09: Phase B 完了時点の進捗サマリー

## 全体進捗

| 段階 | 状態 | 備考 |
| ---- | ---- | ---- |
| Unit 00: プロジェクト基盤整備 | **完了** | Actuator, Docker Compose, CI 基盤 |
| Phase A: インターフェース定義 | **完了** | Flyway V1-V3, Entity, Service interface, DTO, Enum |
| Phase B: Unit 01 部署管理 | **完了** | Backend + Frontend |
| Phase B: Unit 02 社員管理 | **完了** | Backend + Frontend |
| Phase B: Unit 03 認証 | **完了** | Backend + Frontend |
| Phase B: Unit 04 打刻・勤怠 | **完了** | Backend + Frontend |
| Phase B: 統合テスト | 未実施 | Phase B 完了後にまとめて実施する想定 |
| Phase C: Unit 05 勤怠修正 | **未着手** | Flyway V4 + corrections |
| Phase C: Unit 06 月次集計・帳票 | **未着手** | reports, CSV/PDF |

## Backend 実装状況

### テスト: 74 件全パス、`./gradlew check` 全パス

| ドメイン | Repository | ServiceImpl | Controller | テスト数 |
| -------- | ---------- | ----------- | ---------- | -------- |
| department | DepartmentRepository | DepartmentServiceImpl | DepartmentController | 13 |
| employee | EmployeeRepository + Specifications | EmployeeServiceImpl | EmployeeController | 24 |
| auth | — | AuthServiceImpl | AuthController | 11 |
| attendance | AttendanceRecordRepository | AttendanceServiceImpl | AttendanceController | 21 |
| 共通 (architecture) | — | — | — | 5 |

### セキュリティ

- SecurityFilterChain: 権限マトリクス適用済み（permitAll / authenticated / ADMIN）
- CSRF: CookieCsrfTokenRepository + SpaCsrfTokenRequestHandler（SPA 対応）
- セッション: HttpSessionSecurityContextRepository で永続化
- CORS: 環境変数 `app.cors.allowed-origins` で制御

### DB

- Flyway V1-V3: departments, employees, attendance_records
- V1000: シードデータ（管理者 1 名 + 一般社員 3 名）
- `ddl-auto: validate` で Flyway と Entity の整合性を検証

## Frontend 実装状況

### ファイル数: 60（.ts / .tsx）

### 画面一覧

| ルート | ページ | 状態 |
| ------ | ------ | ---- |
| `/login` | ログイン画面 | **動作確認済** |
| `/` | → `/dashboard` にリダイレクト | **動作確認済** |
| `/dashboard` | ダッシュボード（打刻 + 当日記録） | **動作確認済** |
| `/attendance` | 打刻画面 | 実装済 |
| `/history` | 勤怠履歴（月別テーブル + サマリー） | 実装済 |
| `/team` | チーム勤怠（上長のみ） | 実装済 |
| `/admin/departments` | 部署管理（一覧 + 登録/編集） | 実装済 |
| `/admin/employees` | 社員管理（一覧 + フィルタ + CRUD） | 実装済 |

### 認証フロー

1. `/` or 認証必要ページにアクセス → 未認証なら `/login` にリダイレクト
2. ログインフォームで email + password を入力
3. `POST /api/auth/login` → セッション Cookie 取得
4. `/dashboard` にリダイレクト
5. Header にユーザー名 + ログアウトボタン表示
6. Sidebar は ADMIN のみ「管理」セクション表示

### API クライアント

- Next.js rewrites で `/api/*` → `http://localhost:8080/api/*` にプロキシ
- CSRF トークン: Cookie から `XSRF-TOKEN` を読み取り `X-XSRF-TOKEN` ヘッダーに自動付与
- 401 レスポンス時に `/login` へ自動リダイレクト

## 起動方法

```bash
# 1. DB 起動（初回 or 停止中の場合）
npm run db:up

# 2. Backend 起動
npm run boot

# 3. Frontend 起動
npm run dev
```

- Backend: http://localhost:8080 (Swagger UI: http://localhost:8080/swagger-ui.html)
- Frontend: http://localhost:3000

### ログインアカウント

| 名前 | メール | パスワード | ロール |
| ---- | ------ | --------- | ------ |
| 佐藤 管理 | admin@example.com | demo1234 | ADMIN + 上長 |
| 田中 太郎 | tanaka@example.com | password123 | 一般社員 |
| 鈴木 花子 | suzuki@example.com | password123 | 営業部上長 |
| 山田 一郎 | yamada@example.com | password123 | 一般社員 |

## 残タスク

### Phase B 残り

- 統合テスト（`@SpringBootTest` で API エンドポイントの E2E 確認）
- Frontend テスト（Vitest + Testing Library でコンポーネントテスト）

### Phase C（次のステップ）

- Unit 05: 勤怠修正（Flyway V4 + corrections テーブル + 申請/承認フロー）
- Unit 06: 月次集計・帳票（月次レポート + CSV/PDF エクスポート）

### その他

- Frontend の細かい UI 改善（レスポンシブ対応、エラー表示の統一など）
- デプロイ（AWS CDK — `packages/infra/` に雛形あり）
