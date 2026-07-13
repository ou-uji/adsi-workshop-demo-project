# 打刻メモ（備考）入力 — テスト仕様（TDD）

> 仕様（ゲート①）: [新規機能_打刻メモ_仕様(SDD).md](./新規機能_打刻メモ_仕様(SDD).md)
> 設計（ゲート②）: [新規機能_打刻メモ_設計.md](./新規機能_打刻メモ_設計.md)
> ブランチ: `feature/attendance-memo`

## 方針

固めた仕様の各項目を **「失敗するテスト」（Red）** に翻訳する。
Q&A で決めた内容（200文字上限・空欄OK・出退勤両方・本人のみ編集・承認不要）が、そのままテストになる。

**流れ**: Red（テストを書く・失敗を確認）→ Green（最小実装で通す）→ Refactor（整える）。

---

## 仕様 → テストの対応表

| 仕様項目（受け入れ基準） | テスト | レイヤ |
|---|---|---|
| 出勤メモを保存できる | `clockIn_withMemo_savesClockInMemo` | Service |
| 退勤メモを保存できる | `clockOut_withMemo_savesClockOutMemo` | Service |
| 空欄OK（未入力で打刻できる） | `clockIn_withoutMemo_savesNull` | Service |
| 空文字は null 化 | `clockIn_blankMemo_savesNull` | Service |
| 200文字ちょうどは保存OK | `updateMemo_exactly200Chars_succeeds` / Controller 201 | Service / Controller |
| 201文字は 400 | `clockIn_memoOver200_returns400` / `updateMemo_memoOver200_returns400` | Controller |
| 本人はメモを編集できる | `updateMemo_owner_updatesBothMemos` | Service |
| 本人はメモを削除（空に）できる | `updateMemo_emptyString_clearsMemoToNull` | Service |
| 編集は本人のみ（他人は 403） | `updateMemo_notOwner_throwsForbidden` | Service |
| 承認不要・即時反映 | `updateMemo_owner_persistsImmediately`（承認フローを経ない） | Service / 統合 |
| 楽観ロック: 古い version は 409 | `updateMemo_staleVersion_throwsConflict` | Service |
| 履歴でメモが返る | 統合: 打刻→履歴取得で memo が含まれる | 統合 |
| 打刻 API 後方互換（body なしで 201） | `clockIn_noBody_returns201` | Controller |
| 打刻時にメモを送れる（UI） | `ClockButtons`: textarea 入力→mutate が memo 付きで呼ばれる | Frontend |
| 履歴行から編集・削除できる（UI） | `MemoEditDialog`: 保存/削除で updateMemo が正しい payload で呼ばれる | Frontend |

---

## Backend テスト

### 1. AttendanceServiceTest（ユニット・Spring コンテキストなし）
`packages/backend/src/test/java/.../attendance/service/AttendanceServiceTest.java`（既存拡張）

```java
@Test @DisplayName("出勤打刻: メモ付きで打刻すると clockInMemo が保存される")
void clockIn_withMemo_savesClockInMemo() { ... }

@Test @DisplayName("退勤打刻: メモ付きで打刻すると clockOutMemo が保存される")
void clockOut_withMemo_savesClockOutMemo() { ... }

@Test @DisplayName("出勤打刻: メモ未指定(null)でも打刻でき memo は null")
void clockIn_withoutMemo_savesNull() { ... }

@Test @DisplayName("出勤打刻: 空文字メモは null として保存される")
void clockIn_blankMemo_savesNull() { ... }

@Test @DisplayName("メモ編集: 本人が出勤/退勤メモを更新できる")
void updateMemo_owner_updatesBothMemos() { ... }

@Test @DisplayName("メモ編集: 空文字を送るとメモが null にクリアされる")
void updateMemo_emptyString_clearsMemoToNull() { ... }

@Test @DisplayName("メモ編集: 他人のレコードを編集しようとすると FORBIDDEN")
void updateMemo_notOwner_throwsForbidden() { ... }

@Test @DisplayName("メモ編集: version が古いと CONFLICT(409)")
void updateMemo_staleVersion_throwsConflict() { ... }
```
- 依存（Repository / EmployeeRepository / Clock）はコンストラクタでモックを渡す（`@MockitoBean` は使わない）。
- 所有者チェックは `record.getEmployee().getId()` と引数 employeeId の比較。

### 2. AttendanceControllerTest（@WebMvcTest + @AutoConfigureMockMvc）
`packages/backend/src/test/java/.../attendance/controller/AttendanceControllerTest.java`（新規 or 既存拡張）

```java
@Test @DisplayName("POST /clock-in: body なしでも 201（後方互換）")
void clockIn_noBody_returns201() { ... }

@Test @DisplayName("POST /clock-in: 200文字メモは 201")
void clockIn_memo200_returns201() { ... }

@Test @DisplayName("POST /clock-in: 201文字メモは 400（バリデーション）")
void clockIn_memoOver200_returns400() { ... }

@Test @DisplayName("PATCH /records/{id}/memo: 正常更新で 200")
void updateMemo_valid_returns200() { ... }

@Test @DisplayName("PATCH /records/{id}/memo: 201文字メモは 400")
void updateMemo_memoOver200_returns400() { ... }
```
- Service は `@MockitoBean`。400 は `GlobalExceptionHandler.handleMethodArgumentNotValid` 経由（`errors` マップ確認）。

### 3. AttendanceIntegrationTest（@SpringBootTest, test プロファイル）
`packages/backend/src/test/java/.../attendance/AttendanceIntegrationTest.java`（既存拡張）

```java
@Test @DisplayName("統合: メモ付き出勤→履歴取得で memo が返る")
void clockInWithMemo_thenHistory_returnsMemo() { ... }

@Test @DisplayName("統合: メモ編集→再取得で更新が反映される（承認不要・即時）")
void updateMemo_thenRefetch_reflectsChange() { ... }

@Test @DisplayName("統合: 201文字メモの打刻は 400")
void clockIn_memoOver200_returns400() { ... }
```

---

## Frontend テスト（Vitest + Testing Library）

### 4. ClockButtons.test.tsx（既存拡張）
`packages/frontend/src/features/attendance/ClockButtons.test.tsx`

```
- textarea にメモを入力して「出勤」→ clockInMutation.mutate が入力値付きで呼ばれる
- メモ未入力で「出勤」→ mutate が空/undefined で呼ばれる（空欄OK）
- 打刻成功後に textarea がクリアされる
```
- `vi.mock("./useAttendance")` で hook をモック、`afterEach(() => cleanup())` 必須。

### 5. MemoEditDialog.test.tsx（新規）
`packages/frontend/src/features/attendance/MemoEditDialog.test.tsx`

```
- 既存メモが textarea に初期表示される（出勤/退勤の2欄）
- 編集して保存 → updateMemo が {clockInMemo, clockOutMemo, version} 付きで呼ばれる
- 空にして保存（削除）→ updateMemo が空文字で呼ばれる
- 200文字超は maxLength で入力不可（native）
```
- `useUpdateMemo` を `vi.mock`、UI コンポーネント（FormDialog/Button）は軽量スタブ、`afterEach(cleanup)`。

---

## Red → Green → Refactor 手順

1. **Red**: 上記テストを先に書き、`npm run check:backend` / frontend Vitest で **失敗**を確認（実装がまだ無い）。
2. **Green**: 設計ドキュメントの Backend/Frontend 変更を最小実装し、テストを通す。テストコードは変えない。
3. **Refactor**: `blankToNull` ヘルパ抽出・命名整理・重複除去。テストは通ったまま。

## 完了条件
- Backend attendance テスト全 green / Frontend attendance テスト全 green。
- lint（Biome）green。
- 実 API で 201（body 有無）/ 200（PATCH）/ 400（201字）/ 403（他人）/ 409（stale version）を確認。
