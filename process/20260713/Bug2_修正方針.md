# Bug② 出勤ボタン二重押し — 原因調査・修正方針（ドラフト）

> 作成日: 2026-07-13 / ステータス: **調査完了・実装前（壁打ち中）**
> 関連 Issue: [#1](https://github.com/ou-uji/adsi-workshop-demo-project/issues/1)
> ※ これは振り返り用ドラフト。確定仕様ではない。

## バグ概要

出勤打刻をした後も「出勤」ボタンが押せてしまい、連続して出勤打刻が行える。
さらにその後、退勤打刻ができなくなる。

## 再現手順

1. 一般社員（例: `tanaka@example.com` / `password123`）でログイン
2. サイドメニュー「打刻」を開く
3. 「出勤」ボタンを押して出勤打刻
4. もう一度「出勤」ボタンを押す（＝連続で出勤打刻できてしまう）
5. 「退勤」ボタンで退勤打刻を試みる → 失敗する

## 根本原因（2 箇所）

Frontend / Backend の**両方**に原因があり、両方直さないと完全には解消しない。

### ① Frontend: 出勤ボタンの活性判定ロジックが壊れている 🔴 主因

`packages/frontend/src/features/attendance/ClockButtons.tsx:66`

    const canClockIn = status === "NOT_CLOCKED_IN" || status !== "CLOCKED_OUT";

- `||` の右辺 `status !== "CLOCKED_OUT"` が、`CLOCKED_IN`（勤務中）のときも **true**。
- 結果 `canClockIn` が実質**常に true** → 出勤打刻後（CLOCKED_IN）も「出勤」ボタンが活性のまま。
- 本来は「未出勤のときだけ出勤可」なので `status === "NOT_CLOCKED_IN"` であるべき。

### ② Backend: clockIn に二重打刻ガードが無い 🟠 副因（退勤不可の元凶）

`packages/backend/.../attendance/service/AttendanceServiceImpl.java:53` `clockIn()`

- 既存の「未退勤（clockOut = null）」レコードをチェックせず毎回新規 save。
- Frontend の穴（①）を通り抜けたリクエストをそのまま受理。
- → 同日に **clockOut = null のレコードが 2 件以上**発生。

退勤処理 `clockOut()`（同 `:73`）は

    findByEmployeeIdAndWorkDateAndClockOutIsNull(employeeId, today)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, ...));

と `Optional`（単一件）前提。未退勤が複数あると `NonUniqueResultException`
（`IncorrectResultSizeDataAccessException`）が発生し、**退勤打刻が失敗**する。
→ 「もう一度押すと退勤不可」の症状に一致。

## 影響範囲

| 層 | ファイル | 影響 |
|----|---------|------|
| Frontend | `ClockButtons.tsx` | 出勤ボタンの活性判定 |
| Backend | `AttendanceServiceImpl.clockIn` | 二重打刻を受け入れてしまう |
| Backend | `AttendanceRecordRepository` | 未退勤レコード存在チェック用メソッド（既存の `findBy...ClockOutIsNull` 再利用可） |
| データ | `attendance_record` | 二重打刻の不整合レコードが残る場合あり（H2 インメモリは再起動でクリア） |

## 修正方針（案）

**方針: Frontend / Backend の二重防御。**（UI で防ぎ、サーバでも最終ガード）

1. **Frontend** — `canClockIn` を `status === "NOT_CLOCKED_IN"` に修正。
   （`canClockOut` は現状の `status === "CLOCKED_IN"` で正しい）
2. **Backend** — `clockIn()` 冒頭で当日の未退勤レコードの存在を確認し、
   あれば `409 CONFLICT`（例: 「既に出勤打刻済みです」）を返す。
3. **回帰テスト**（`.claude/rules/testing.md`: テスト以外の実装で修正）
   - Backend: 「出勤中に再度 clockIn すると 409」を `AttendanceServiceTest` / `AttendanceControllerTest` に追加。
   - Frontend: `ClockButtons` で `CLOCKED_IN` のとき出勤ボタン `disabled`・退勤ボタン活性、を検証。
4. **既存データ** — 二重打刻レコードの残存確認（デモは H2 再起動でクリアのため通常不要）。

## 壁打ちポイント（人がチェックする論点）

- 二重防御（Front+Back 両方）は過剰か? → Backend ガードは UI すり抜け対策として必須と判断。
- 既存不整合データ対応は不要と判断（H2 インメモリ、再起動でクリア）。
- 「なぜその方針か / 他の原因は?」の問い返し歓迎。

## デグレ確認結果（実装前の裏取り）

呼び出し箇所と既存テストを全て洗った結果、**既存機能へのデグレはなし**と判断。

### 1. Frontend の影響範囲は閉じている

- `canClockIn` の使用箇所は `ClockButtons.tsx:86` の `disabled` 属性 **1 箇所のみ**。他ファイル参照なし。
- `canClockOut`（`status === "CLOCKED_IN"`）は元々正しいので変更しない。→ 波及ゼロ。

### 2. Backend の既存テストは「あるべき仕様」と一致

| 既存テスト | 内容 | 修正後 |
|-----------|------|:---:|
| `clockInThenClockOut_succeeds` | 出勤→退勤 | ✅ 通る |
| `clockInAgain_afterClockOut_succeeds` ⭐ | 退勤**後**の再出勤は許可 | ✅ 通る |
| `clockOut_noClockedIn_returns409` | 未出勤で退勤は 409 | ✅ 影響なし |
| `getTodayStatus_afterClockIn` | 出勤後 `records` は `hasSize(1)` | ✅ 通る |

- ⭐ 肝: `AttendanceIntegrationTest.java:147` は「一度**退勤してから**の再出勤は OK」というテスト。
  今回のガードは「**未退勤（clockOut=null）レコードがある時だけ** 409」なので、退勤済みケースは素通り → **壊れない**。
  （＝「1 日 1 回しか出勤不可」という過剰制約にはしない）
- `AttendanceIntegrationTest.java:191` は出勤後 `records` が `hasSize(1)` を期待 → 現状バグ（二重で 2 件化）を
  既に正しい仕様として記述済み。今回の修正はこのテストの意図に沿う方向。

### 3. 他ドメインへの影響なし

- `CorrectionServiceImpl` の `.clockIn(...)` は Entity builder のフィールド設定で、Service の `clockIn()` とは無関係。
- Report / WorkDuration 系は Entity を組み立てるだけ。

### 唯一の注意点（デグレではなく想定挙動）

- Backend ガード追加で `clockIn()` は「常に成功」→「409 を投げうる」に変わる。
- 既存の `clockIn_returns201` / `clockIn_validRequest_returns201` は**未出勤状態から呼ぶ**テストなので通る。
- TDD の作法どおり「出勤中に再 clockIn で 409」の**新規テストを Red から追加**し、新挙動を仕様として固定する。

## 次工程

壁打ちで方針合意 → テスト追加（Red）→ 実装（Green）→ リファクタ → レビュー → PR（Closes #1）。
