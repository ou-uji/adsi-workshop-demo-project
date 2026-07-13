# Bug② TDD 実践記録 — テスト設計（Red → Green）

> 作成日: 2026-07-13 / 関連 Issue: [#1](https://github.com/ou-uji/adsi-workshop-demo-project/issues/1)
> 対象ブランチ: `bug-fix/bug2-double-clock-in`
> 前提ドキュメント: [Bug2_修正方針.md](./Bug2_修正方針.md) / 検証編: [Bug2_テスト検証.md](./Bug2_テスト検証.md)

## この記録の目的

バグ②（出勤ボタン二重押し → 退勤不可）を **TDD で修正した実際の流れ**を、
テスト設計の観点で残す。後で「なぜこのテストをこう書いたか」を振り返れるようにする。

## TDD の基本サイクル（このプロジェクトの作法）

```
Red:      仕様をテストとして書く（この時点では失敗する）
Green:    テストが通る最小限の実装を書く
Refactor: テストを通したままコードを整理する
```

- テストが「詳細設計書」になる（`.claude/rules/common/development-process.md`）。
- **修正はテスト以外の実装で行う**（テスト変更は最終手段）。`.claude/rules/testing.md`。

## テスト設計の全体像（二重防御 → テストも二層）

修正方針が「Frontend / Backend の二重防御」だったので、テストも両層に置いた。

| 層 | テストファイル | 種類 | 検証内容 |
|----|--------------|------|---------|
| Backend | `AttendanceServiceTest` | ユニット（Mockito） | 出勤中に再 clockIn → 409、かつ **save が呼ばれない** |
| Backend | `AttendanceIntegrationTest` | 統合（MockMvc + 実DB） | 出勤→再出勤=409→退勤=200 の一連フロー |
| Frontend | `ClockButtons.test.tsx` | コンポーネント（Testing Library） | 3 状態でのボタン活性/非活性 |

### なぜユニットと統合の 2 本立てにしたか

- **Controller テストは不採用**: Controller は Service をモックするだけなので、
  「二重打刻ガード」というロジックを検証できない（意味の薄いテストになる）。
- 代わりに **Service ユニット**（ロジックの単体検証）＋**統合**（HTTP 経路・実DBでの挙動）で挟んだ。
- これは既存の `clockOut` の 409 テストが統合テストにあった構成と対称的で、コードベースの流儀に合わせた。

## Backend: Service ユニットテスト（Red 先行）

`packages/backend/.../attendance/service/AttendanceServiceTest.java` の `ClockIn` ネストに追加。

```java
@Test
@DisplayName("出勤中（未退勤レコードあり）に再度出勤打刻すると409エラーで新規レコードを作らない")
void clockIn_alreadyClockedIn_throwsConflict() {
    // Arrange: 未退勤レコードが存在する状態をモック
    when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
    var openRecord = AttendanceRecord.builder()
            .id(UUID.randomUUID()).employee(employee).workDate(TODAY_TOKYO)
            .clockIn(Instant.parse("2025-01-14T23:00:00Z")).build();
    when(attendanceRepository.findByEmployeeIdAndWorkDateAndClockOutIsNull(employee.getId(), TODAY_TOKYO))
            .thenReturn(Optional.of(openRecord));

    // Act & Assert: 409 かつ save されない
    assertThatThrownBy(() -> service.clockIn(employee.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Already clocked in");
    verify(attendanceRepository, never()).save(any(AttendanceRecord.class));
}
```

**設計のポイント**
- `never()` の import 追加が必要（`import static org.mockito.Mockito.never;`）。
- **「save されない」まで検証**したのが肝。単に例外を見るだけでなく「副作用が起きない」ことを保証する。
- 命名は規約どおり `メソッド名_シナリオ_期待結果`（`clockIn_alreadyClockedIn_throwsConflict`）。
- 既存テスト（`clockIn_normal_createsRecord`）と同じ Arrange パターン（Clock 固定、builder）を踏襲。

## Backend: 統合テスト（Red 先行）

`packages/backend/.../attendance/AttendanceIntegrationTest.java` に追加。

```java
@Test
@DisplayName("出勤中に再度出勤打刻すると409が返り、退勤打刻は正常に成功する")
void clockInTwice_thenClockOut_secondClockInReturns409() throws Exception {
    // 1回目: 201
    mockMvc.perform(post("/api/attendance/clock-in").session(employeeSession)
            .with(csrf()).param("employeeId", employeeId.toString()))
        .andExpect(status().isCreated());
    // 2回目: 409（二重打刻防止）
    mockMvc.perform(post("/api/attendance/clock-in").session(employeeSession)
            .with(csrf()).param("employeeId", employeeId.toString()))
        .andExpect(status().isConflict());
    // 退勤: 200（二重打刻が防がれているので正常）
    mockMvc.perform(post("/api/attendance/clock-out").session(employeeSession)
            .with(csrf()).param("employeeId", employeeId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clockOut").exists());
}
```

**設計のポイント**
- **バグの症状を 1 テストで再現・検証**: 「連続出勤」と「その後の退勤不可」は連鎖する症状なので、
  1 つのシナリオテストで「409 で止まる → だから退勤できる」を通しで示した。
- 既存の `clockInAgain_afterClockOut_succeeds`（退勤後の再出勤は OK）を壊さない位置・内容にした。
  → **未退勤があるときだけ 409**。退勤済みなら素通り、という仕様の境界をテストで固定。

## Backend: Green 実装（テスト以外で修正）

`AttendanceServiceImpl.clockIn()` の冒頭にガードを追加（既存メソッドを再利用）。

```java
attendanceRepository.findByEmployeeIdAndWorkDateAndClockOutIsNull(employeeId, today)
        .ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already clocked in");
        });
```

- 新規メソッド追加不要。`clockOut()` が既に使っている `findBy...ClockOutIsNull` を流用（DRY）。
- `clockOut` の 409 と対称的な書き味に揃えた。

## Frontend: コンポーネントテスト

`packages/frontend/src/features/attendance/ClockButtons.test.tsx`（新規）。

**設計方針**
- フック（`useTodayStatus` / `useClockIn` / `useClockOut`）を `vi.mock` で差し替え、
  **status を注入して UI の活性だけを検証**（API は叩かない）。既存 `LoginForm.test.tsx` の流儀に合わせた。
- ユーザー視点のクエリ `getByRole("button", { name: "出勤" })` を使用（`getByTestId` は使わない）。
- 3 状態を網羅:

| status | 出勤ボタン | 退勤ボタン |
|--------|:---------:|:---------:|
| `NOT_CLOCKED_IN` | enabled | disabled |
| `CLOCKED_IN` | **disabled**（二重打刻防止） | enabled |
| `CLOCKED_OUT` | disabled | disabled |

**ハマりどころ → 検証編に詳述**
- テスト間で DOM が累積し「ボタンが複数マッチ」する問題に遭遇。
  原因は accessible name ではなく **auto-cleanup 未登録**。`afterEach(cleanup)` で解決。
- 詳細な切り分け過程は [Bug2_テスト検証.md](./Bug2_テスト検証.md) 参照。

## Frontend: Green 実装

`ClockButtons.tsx:66`

```diff
- const canClockIn = status === "NOT_CLOCKED_IN" || status !== "CLOCKED_OUT";
+ const canClockIn = status === "NOT_CLOCKED_IN";
```

## 学び（TDD 観点）

1. **Red を必ず先に赤で確認する**: 実装前に Service テストを流し、期待どおり 1 件失敗するのを見てから Green に進んだ。
   「テストが実は何も検証していない」事故を防げる。
2. **副作用まで assert する**: 「例外が出る」だけでなく「save されない」まで見ると、ガードの意味が固定される。
3. **既存テストを仕様の境界として尊重する**: `clockInAgain_afterClockOut_succeeds` を壊さないよう
   「未退勤があるときだけ 409」に設計 → 過剰制約（1日1回制限）を避けられた。
4. **テスト種別は「何を検証したいか」で選ぶ**: Controller モックでは検証できないロジックは Service/統合へ。
