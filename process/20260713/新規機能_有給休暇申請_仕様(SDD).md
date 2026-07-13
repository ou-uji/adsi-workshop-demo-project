# 有給休暇申請機能 — 確定仕様（ゲート①）

> 元 Q&A: [新規機能_有給休暇申請_QA.md](./新規機能_有給休暇申請_QA.md)
> ブランチ: `feature/skill-auto-dev`
> 位置づけ: 研修用デモ機能。既存 **勤怠修正（correction / Unit 05）** と同じ「申請 → 承認/却下 → 反映」の型を再現する。**短時間で作る・完成しなくても OK** が前提。

## 概要

社員が**有給休暇を申請**し、**所属部署の上長が承認/却下**する。承認された申請は `APPROVED` として記録され、本人が状態（申請中/承認/却下）を確認できる。
残日数管理・半休・期間指定・種別は**今回のスコープ外**（将来拡張として記録済み。末尾マトリクス参照）。

## ユーザーストーリー

- **LEAVE-01**: 社員として、有給休暇を申請したい（対象日・理由を入力）
- **LEAVE-02**: 社員として、自分が出した有給申請の状態（申請中/承認/却下）を確認したい
- **LEAVE-03**: 上長として、自部署メンバーの有給申請を一覧で確認したい
- **LEAVE-04**: 上長として、自部署メンバーの有給申請を承認または却下したい（却下時は理由を入力）
- **LEAVE-05**: 承認者は申請者と同一部署の上長に限定する（上長自身の申請は自己承認）
- **LEAVE-06**: 承認/却下の競合は楽観ロックで制御する（後の操作が 409）

## 確定仕様

| # | 項目 | 決定 |
|---|------|------|
| 1 | 残日数管理 | **しない**（申請を通すだけ。付与・消化・残数チェックなし） |
| 2 | 申請単位 | **1日単位（全休）のみ** |
| 3 | 申請範囲 | **単一日**（1申請=1日） |
| 4 | 休暇種別 | **有給休暇のみ**（種別カラムなし） |
| 5 | 承認者 | **申請者と同一部署の上長（`isManager=true`）**。上長本人の申請は**自己承認**。ADMIN 特権承認なし |
| 6 | 却下理由 | **必須**（最大 500 文字） |
| 7 | 取り下げ | **なし**（申請/承認/却下の3操作のみ） |
| 8 | 対象日制約 | **なし**（過去日・当日・未来日いずれも可） |
| 9 | 承認後の反映 | **`leave_requests` に `APPROVED` で残す**（`attendance_records` は触らない） |
| 10 | 重複チェック | **なし** |
| 11 | 入力項目 | **対象日（必須）＋ 理由（必須・最大 500 文字）** |
| 12 | 閲覧範囲 | **本人（自分の申請）＋ 自部署の上長（承認待ち）**。管理者全社一覧なし |
| 13 | 楽観ロック | **`@Version` ＋ version チェック（不一致で 409）**。ステータス PENDING/APPROVED/REJECTED |

## 受け入れ基準（Acceptance Criteria）

### 申請（LEAVE-01）
- [ ] 対象日と理由を入力して有給申請すると `PENDING` で保存される（201）
- [ ] 理由が空、または 500 文字超は 400（バリデーションエラー）
- [ ] 理由 500 文字ちょうどは保存できる
- [ ] 過去日・未来日いずれの対象日でも申請できる（制約なし）

### 自分の申請確認（LEAVE-02）
- [ ] 本人は自分の有給申請一覧を取得できる（新しい順）
- [ ] ステータス（PENDING/APPROVED/REJECTED）でフィルタできる
- [ ] 他人の申請は自分の一覧に出ない

### 承認待ち一覧（LEAVE-03）
- [ ] 上長は自部署メンバーの `PENDING` 申請一覧を取得できる
- [ ] 他部署の申請は承認待ち一覧に出ない
- [ ] 一般社員（非上長）が承認待ち一覧にアクセスしても業務上意味を持たない（UI では非表示。API は authenticated）

### 承認（LEAVE-04 / 05）
- [ ] 同一部署の上長が承認すると `status=APPROVED`・`approver` が記録される（200）
- [ ] 上長本人の申請を本人が承認できる（自己承認）
- [ ] 他部署の社員／非上長が承認しようとすると 403
- [ ] `attendance_records` は変更されない（Q9=A）

### 却下（LEAVE-04）
- [ ] 同一部署の上長が却下すると `status=REJECTED`・`reject_reason` が保存される（200）
- [ ] 却下理由が空だと 400
- [ ] 他部署の社員／非上長が却下しようとすると 403

### 楽観ロック（LEAVE-06）
- [ ] 承認/却下時に version が不一致だと 409
- [ ] 先に承認された申請へ古い version で再操作すると 409

## API（correction に準拠）

| メソッド | パス | パラメータ / Body | 返却 | ステータス | 権限 |
|---------|------|------------------|------|-----------|------|
| POST | `/api/leave-requests` | `?requesterId=UUID` + body `{ targetDate, reason }` | `LeaveRequestResponse` | 201 | authenticated |
| GET | `/api/leave-requests` | `?requesterId=UUID&status=?` | `List<LeaveRequestResponse>` | 200 | authenticated |
| GET | `/api/leave-requests/pending` | `?managerId=UUID` | `List<PendingLeaveRequestResponse>` | 200 | authenticated |
| PATCH | `/api/leave-requests/{id}/approve` | `?approverId=UUID&version=Long` | `LeaveRequestResponse` | 200 | authenticated |
| PATCH | `/api/leave-requests/{id}/reject` | `?approverId=UUID` + body `{ reason, version }` | `LeaveRequestResponse` | 200 | authenticated |

- パス命名は correction の `/api/corrections` に対して `/api/leave-requests`（複数形・kebab-case）。
- アイデンティティ（requester/approver/manager）は correction と同じく `@RequestParam UUID` で受ける（※ 既存アーキの既知事項：セッションと非照合。correction と揃える）。
- 認可は SecurityConfig で各ルートを `authenticated()` に列挙（末尾 `denyAll()` の前）。承認権限はサービス層 `validateApprover` で担保。

## データモデル（新規テーブル `leave_requests`）

correction の `attendance_corrections` を土台に、有給に不要なカラム（`attendance_record_id`・`corrected_clock_in/out`）を落とし、`target_date`＋`reason` に絞る。

| カラム | 型 | 制約 | 備考 |
|--------|----|------|------|
| `id` | UUID | PK | アプリ採番（UUIDv7） |
| `requester_id` | UUID | NOT NULL, FK→employees | 申請者 |
| `approver_id` | UUID | FK→employees, nullable | 承認/却下時に設定 |
| `target_date` | DATE | NOT NULL | 有給取得日（単一日） |
| `reason` | VARCHAR(500) | NOT NULL | 申請理由 |
| `status` | VARCHAR(20) | NOT NULL, CHECK IN (PENDING/APPROVED/REJECTED) | 初期値 PENDING |
| `reject_reason` | VARCHAR(500) | nullable | 却下時のみ |
| `version` | BIGINT | NOT NULL DEFAULT 0 | `@Version` |
| `created_at` | TIMESTAMPTZ | NOT NULL | 監査 |
| `updated_at` | TIMESTAMPTZ | NOT NULL | 監査 |

- インデックス: `requester_id`、`status`（correction に同じ）
- Flyway: **`V6__create_leave_requests.sql`**（現状 V5 まで。`ddl-auto` 禁止のため必須）
- 種別カラム・残数・期間の2カラム化は将来拡張（下記マトリクス）。

## スコープ外（今回やらない）

- 有給残日数の付与・消化・チェック（Q1）
- 半休・時間単位（Q2）／期間・複数日指定（Q3）
- 有給以外の休暇種別（Q4）
- 申請の取り下げ／承認取消（Q7）
- 対象日の制約・申請締切（Q8）
- 承認済み有給の勤怠履歴/月次集計への反映（Q9 の B/C）
- 重複・打刻突合チェック（Q10）
- 管理者向け全社一覧（Q12）
- 代理承認・多段承認（Q5）

## 将来拡張マトリクス（再設計の起点）

> 研修デモとして最小に倒した各判断を、**拡張が必要になったときに何を再設計するか**で一覧化。詳細は QA の各「▸ 拡張時」。

| 拡張トリガー | 関連 Q | 再設計の勘所（最低限） |
|-------------|--------|----------------------|
| 残日数を管理したい | Q1 | `leave_balances` テーブル新設＋付与ロジック（労基法・勤続年数）＋年度繰越＋申請時チェック/承認時減算。**波及最大** |
| 半休・時間有給 | Q2 | `leave_unit`(FULL/AM/PM) or 時刻カラム＋`WorkDuration` 連携＋0.5日/時間の残数計算 |
| 期間・飛び石申請 | Q3 | `start_date`/`end_date` 2カラム化 or 日付明細テーブル正規化＋営業日数計算 |
| 有給以外の種別 | Q4 | `leave_type` enum＋種別ごとの集計/残数分岐 |
| 代理・多段承認 | Q5 | 部署に代理者＋`validateApprover` 分岐 or 承認ステップ状態機械化 |
| 取り下げ/承認取消 | Q7 | `CANCELED` ステータス＋`/cancel` API＋（残数管理時は）残数戻し |
| 対象日制約・締切 | Q8 | `targetDate>=今日` バリデーション（Front/Back）＋営業日締切計算 |
| 履歴/集計へ反映 | Q9 | `attendance_records` に有給フラグ or 月次集計に有給日数列＋**欠勤集計の再定義**＋CSV/PDF 反映 |
| 重複防止 | Q10 | `create` で同一日・未却下の存在チェック→409（バグ②修正と同手法） |
| 管理者全社一覧 | Q12 | `GET /api/leave-requests/all`（`hasRole("ADMIN")`）＋管理者画面 |

---

## 次ステップ

ゲート①（仕様確定）通過 → **設計・実装 Plan（ゲート②）**（`新規機能_有給休暇申請_設計.md`）。
correction の Backend/Frontend ファイル構成をテンプレートに、新パッケージ `com.example.attendance.leave` と `src/features/leave/` を設計する。
