# 有給休暇申請機能 — テスト仕様（TDD / Red）

> 設計: [新規機能_有給休暇申請_設計.md](./新規機能_有給休暇申請_設計.md)
> ブランチ: `feature/skill-auto-dev`
> 方針: correction の既存テスト（`CorrectionServiceTest` 等）をテンプレートに、有給の受け入れ基準を Red テストとして先に定義する。各テストは**どの受け入れ基準（AC）を満たすか**を明示。

## テスト観点マップ（AC → テスト）

| 受け入れ基準（仕様書） | テスト種別 | テスト |
|----------------------|-----------|--------|
| 申請すると PENDING で保存（LEAVE-01） | Service Unit | `create_validRequest_createsPendingLeaveRequest` |
| 理由空/500超は 400 | Controller WebMvc / DTO | `create_blankReason_returns400`, `create_tooLongReason_returns400` |
| 過去日でも申請できる | Service Unit | `create_pastDate_succeeds` |
| 自分の申請一覧（新しい順） | Service Unit + Repo | `findByRequester_returnsOwnRequests`, Repo: `findByRequesterId...` |
| ステータスフィルタ | Service Unit | `findByRequester_withStatusFilter_returnsPendingOnly` |
| 承認待ち一覧＝自部署の PENDING | Service Unit + Repo | `findPending_returnsOwnDepartmentPending`, Repo test |
| 承認で APPROVED・approver 記録（LEAVE-04） | Service Unit | `approve_byManager_setsApproved` |
| 上長の自己承認（LEAVE-05） | Service Unit | `approve_selfApproval_succeeds` |
| 他部署/非上長は 403 | Service Unit | `approve_notManager_throwsForbidden` |
| **承認で attendance_records を触らない（Q9=A）** | Service Unit | `approve_doesNotTouchAttendance`（AttendanceRecordRepository を DI しない設計で保証） |
| 却下で REJECTED・理由保存 | Service Unit | `reject_setsStatusAndReason` |
| 却下理由空は 400 | Controller WebMvc | `reject_blankReason_returns400` |
| version 不一致は 409（LEAVE-06） | Service Unit | `approve_versionMismatch_throwsConflict` |
| API 契約（各エンドポイント status） | Controller WebMvc | 下表 |

---

## Backend テスト

### 1. Repository（`LeaveRequestRepositoryTest`, `@DataJpaTest` + `@ActiveProfiles("test")`）

correction の Repository テストを踏襲。Department/Employee を保存してから LeaveRequest を保存し、派生クエリを検証。

| # | テスト | 内容 |
|---|--------|------|
| R1 | `findByRequesterId_returnsOwnRequests` | requester_id で自分の申請のみ取得（新しい順） |
| R2 | `findByRequesterIdAndStatus_filtersbyStatus` | requester_id + PENDING で絞り込み |
| R3 | `findByRequesterDepartmentIdAndStatus_returnsDeptPending` | 部署 ID + PENDING で自部署メンバーの申請を取得 |

### 2. Service（`LeaveRequestServiceTest`, Mockito `@ExtendWith(MockitoExtension.class)`）

DI は `LeaveRequestRepository` + `EmployeeRepository` の**2 mock のみ**（AttendanceRecordRepository を渡さない＝設計上「承認で勤怠を触らない」を型で保証）。`setUp` は correction テストと同じく department / employee(非上長) / manager(上長・同一部署) を用意。

| # | テスト（`@DisplayName`） | Arrange → Act → Assert 要点 |
|---|------|------|
| S1 | 有給申請すると PENDING で保存される<br>`create_validRequest_createsPendingLeaveRequest` | requester を mock → `create(requesterId, {targetDate, reason})` → status=PENDING・reason 一致・requesterId 一致。ArgumentCaptor で保存 Entity の targetDate 検証 |
| S2 | 過去日でも申請できる<br>`create_pastDate_succeeds` | targetDate=過去日 → 例外なく PENDING 作成（Q8=制約なし） |
| S3 | 自分の申請のみ返す<br>`findByRequester_returnsOwnRequests` | `findByRequesterIdOrderByCreatedAtDesc` を mock → size 1・requesterId 一致 |
| S4 | ステータスフィルタで PENDING のみ<br>`findByRequester_withStatusFilter_returnsPendingOnly` | `findByRequesterIdAndStatus...(id, PENDING)` mock → status=PENDING |
| S5 | 自部署メンバーの PENDING のみ<br>`findPending_returnsOwnDepartmentPending` | manager mock + `findByRequesterDepartmentIdAndStatus...(deptId, PENDING)` mock → size 1 |
| S6 | 同一部署の上長が承認すると APPROVED<br>`approve_byManager_setsApproved` | correction mock（version 0）+ manager mock + save passthrough → status=APPROVED・approverId=manager |
| S7 | 上長の自己承認が動作する<br>`approve_selfApproval_succeeds` | requester=manager の申請 → manager が承認 → APPROVED・approverId=manager |
| S8 | 他部署/非上長は 403<br>`approve_notManager_throwsForbidden` | 別部署の上長 or 非上長で承認 → `ResponseStatusException` "403" |
| S9 | **承認しても勤怠レコードを作成/更新しない**<br>`approve_doesNotCreateAttendanceRecord` | 承認後、`leaveRequestRepository.save` は呼ばれるが Attendance 系リポジトリは DI されていない。approve が LeaveRequest のみ返すことを確認（Q9=A の保証。correction との差分の要） |
| S10 | 却下で REJECTED・理由保存<br>`reject_setsStatusAndReason` | reject(id, managerId, "不備あり", 0) → status=REJECTED・rejectReason 一致 |
| S11 | version 不一致で 409<br>`approve_versionMismatch_throwsConflict` | correction.version=1、approve(..., 0) → `ResponseStatusException` "409" |
| S12 | 却下も他部署/非上長は 403<br>`reject_notManager_throwsForbidden` | 別部署上長で reject → 403（対称性の担保） |

### 3. Controller（`LeaveRequestControllerTest`, `@WebMvcTest(LeaveRequestController.class)` + `@MockitoBean LeaveRequestService`）

correction の WebMvc テストを踏襲。CSRF・認証は `@WithMockUser` 等（既存 correction テストの流儀に合わせる）。Service はモックし、HTTP 契約（ステータス・JSON マッピング・バリデーション）を検証。

| # | テスト | メソッド/パス | 期待 |
|---|--------|--------------|------|
| C1 | 申請作成は 201 | POST `/api/leave-requests?requesterId=` + body | 201・service.create 呼び出し |
| C2 | 理由空は 400 | POST（reason="") | 400（`@Size(min=1)` 違反） |
| C3 | 理由 500 超は 400 | POST（reason=501字） | 400 |
| C4 | targetDate 欠落は 400 | POST（targetDate なし） | 400（`@NotNull`） |
| C5 | 自分の申請一覧は 200 | GET `/api/leave-requests?requesterId=` | 200・JSON 配列 |
| C6 | ステータス指定は 200 | GET `...&status=PENDING` | 200・service に status 伝播 |
| C7 | 承認待ち一覧は 200 | GET `/api/leave-requests/pending?managerId=` | 200 |
| C8 | 承認は 200 | PATCH `/{id}/approve?approverId=&version=` | 200 |
| C9 | 却下は 200 | PATCH `/{id}/reject?approverId=` + body`{reason,version}` | 200 |
| C10 | 却下理由空は 400 | PATCH reject（reason="") | 400 |

> 注: `@RequestParam` のアイデンティティ（requesterId/approverId/managerId）は correction 同様セッション非照合。これは既存アーキ踏襲の**既知事項**（レビューで HIGH 指摘され得るが、correction と同一方針で現状維持）。テストでもパラメータで渡す。

---

## Frontend テスト（Vitest + Testing Library）

correction には frontend テストが無いが、有給では**打刻メモ（前ハンズオン）で確立した Form/Actions テスト**の流儀に倣い最小限を用意。API はモック（`vi.mock` で hook or api-client をモック）。`afterEach(cleanup)`（前回のハマり所＝auto-cleanup 未登録対策）を忘れない。

### `LeaveRequestForm.test.tsx`

| # | テスト | 内容 |
|---|--------|------|
| F1 | 必須未入力だと送信ボタンが disabled | targetDate/ reason 空 → 「申請する」ボタン disabled |
| F2 | 入力すると送信ボタンが有効 | targetDate 選択 + reason 入力 → ボタン活性 |
| F3 | 送信で create API が正しい引数で呼ばれる | 入力 → submit → `createLeaveRequest`(or mutation) が `{targetDate, reason}` で呼ばれる |
| F4 | reason は maxLength 500 | textarea の maxLength 属性を検証 |

### `LeaveApprovalActions.test.tsx`

| # | テスト | 内容 |
|---|--------|------|
| A1 | 承認ボタンで approve mutation が `{id, version}` で呼ばれる | 承認クリック → mutate 引数検証 |
| A2 | 却下ダイアログで理由入力→却下で reject mutation が `{id, reason, version}` で呼ばれる | 却下→理由入力→送信 |
| A3 | 却下理由が空だと送信できない | ダイアログ内 textarea 空 → 送信ボタン disabled or required |

> StatusBadge・DataTable・List 系は既存共有コンポーネントの再利用でロジックが薄いため、コンポーネントテストは Form/Actions に集中（打刻メモと同じ判断）。

---

## Red → Green → Refactor の進め方

1. **Red**: 上記テストを先に書く（実装クラス・関数が無いのでコンパイルエラー/失敗）
2. **Green**: 設計ドキュメントの実装順序で最小実装 → テストを緑に
3. **Refactor**: correction との重複を確認しつつ整理（ただし過度な共通化はしない＝ correction を無理に抽象化しない。YAGNI）

## 完了条件（このフェーズ）

- [ ] 上記 Backend テスト（Repo 3 + Service 12 + Controller 10）が定義されている
- [ ] Frontend テスト（Form 4 + Actions 3）が定義されている
- [ ] すべて Green（実装後）
- [ ] leave パッケージのカバレッジが実質 80% 以上（Service/Controller を網羅）
- [ ] 既存テストにデグレが無い（correction/attendance 等が全 green のまま）
