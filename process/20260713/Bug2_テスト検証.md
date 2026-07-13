# Bug② テスト検証・トラブルシュート記録

> 作成日: 2026-07-13 / 関連 Issue: [#1](https://github.com/ou-uji/adsi-workshop-demo-project/issues/1)
> 対象ブランチ: `bug-fix/bug2-double-clock-in`
> 設計編: [Bug2_TDD.md](./Bug2_TDD.md) / 方針: [Bug2_修正方針.md](./Bug2_修正方針.md)

## この記録の目的

テストを「書いた」後の**検証フェーズで実際にハマった罠と、その切り分け方**を残す。
テストは書けても「落ちたときにどう真因へ辿り着くか」が実力の差になる。今回の生々しい過程を記録する。

## 検証の全体フロー

```
Backend Service テスト（Red 確認） → Green 実装
  → Backend 対象テスト（Green 確認）
  → Frontend テスト（★ 罠1: DOM 未クリーンアップ）
  → Frontend 全テスト + lint（★ 罠2: lint 負債の切り分け）
  → Backend check 全体（★ 罠3: PDF テスト失敗の切り分け）
  → 実 API で二重打刻ガードを実証（curl）
```

最終結果:
- Backend: 触った attendance テスト **34 件すべて green**
- Frontend: 全 **56 件 green**、変更ファイルは lint クリーン
- 実 API: 出勤=201 → 再出勤=**409** → 退勤=**200**

---

## ★ 罠1: Frontend テストが「ボタンが複数マッチ」で落ちる（最重要）

### 症状

`ClockButtons.test.tsx` の 2・3 番目のテストだけが落ちる。1 番目は通る。

```
TestingLibraryElementError: Found multiple elements with the role "button" and name "出勤"
```

### 誤診断 → 迷走（記録として残す）

最初「退勤ボタンの accessible name に『出勤』が部分マッチしているのでは」と疑い、
name 指定を次のように変えたが **どれも効かなかった**:

1. `{ name: /出勤/ }` → 部分マッチ疑い
2. `{ name: "出勤" }` → 完全一致のはず → それでも複数マッチ
3. `{ name: (n) => n.trim() === "出勤" }` → 関数マッチ → それでも複数マッチ

### 真因の特定（事実ベースで潰す）

**推測をやめて、実際の DOM とアクセシブルネームをダンプした**。

```js
// 一時デバッグテストで computeAccessibleName を出力
const buttons = screen.getAllByRole("button");
// 結果: ACC=[name="出勤" text="出勤"] || [name="退勤" text="退勤"]
```

→ accessible name は **"出勤" / "退勤" で正しく別物**。name 指定は悪くなかった。

ここで「1 番目は通る／2・3 番目が落ちる」「常に前テストのボタンが残っている」ことに注目 →
**テスト間で DOM がクリーンアップされず、`render()` が累積**していた。
2 番目のテスト時点で「出勤」ボタンが 2 個（前テスト＋今回）存在 → 複数マッチ。

### 根本原因

`vitest.config.ts` に `globals: true` が無い。

```ts
test: {
  environment: "jsdom",
  setupFiles: ["./src/test/setup.ts"],   // ← jest-dom のみ。cleanup 登録なし
  include: ["src/**/*.test.{ts,tsx}"],
}
```

- `@testing-library/react` の **auto-cleanup は `globals: true` 環境で自動登録**される仕組み。
- このプロジェクトは `globals` 未設定（=false）なので **各テスト後の cleanup が走らない**。
- 既存テスト（MonthlySummary 等）が無事だったのは、たまたま「複数マッチを誘発するクエリ」を
  使っていなかっただけ。**潜在的な地雷**だった。

### 対処（スコープ最小・設定は変えない）

`vitest.config.ts` を触ると全テストに影響するため、**このファイル内で明示的に cleanup** した。

```ts
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, ... } from "vitest";

afterEach(() => {
  cleanup();
});
```

→ name 指定は素直な `{ name: "出勤" }` に戻して 3/3 green。

### 教訓

- **「複数マッチ」= 必ずしもクエリの問題ではない**。テスト間の状態リークをまず疑う。
- 推測で name 指定をいじり続けるより、**DOM / accessible name を一度ダンプして事実を見る**方が速い。
- 設定変更は影響範囲が広い。**まずファイルローカルで閉じた対処**を選ぶ（`afterEach(cleanup)`）。
- （改善余地）本来は `vitest.config.ts` に `globals: true` を入れて全体で auto-cleanup を効かせるのが
  正攻法。今回はデグレ回避を優先しスコープを絞った。別 Issue 化候補。

---

## ★ 罠2: Frontend lint が 18 errors（自分の変更のせい？）

### 症状

`npm run lint`（Biome）で 18 errors + 10 warnings。一見「自分の追加で壊した」ように見える。

### 切り分け

**自分が触った 2 ファイルだけを対象に lint** して分離した。

```bash
npx biome check src/features/attendance/ClockButtons.tsx \
                src/features/attendance/ClockButtons.test.tsx
# → Found 1 error（import 並び順の safe fix のみ）
```

→ 残り 17 件は**既存ファイルの lint 負債**で、今回の変更とは無関係。

### 対処

自分のファイルの 1 件だけ safe fix を適用。

```bash
npx biome check --write src/features/attendance/ClockButtons.tsx \
                        src/features/attendance/ClockButtons.test.tsx
# → import の型/値の並び替えが自動修正され、クリーンに
```

fix 後に **テスト再実行して壊れていないことを確認**（import 並びが変わるため）。

### 教訓

- **「全体で N errors」に驚かない**。自分の diff の範囲に絞って再計測すれば責任範囲が即わかる。
- 既存負債と自分の変更を混同しない。混ぜて直すと diff が膨らみレビュー困難になる。

---

## ★ 罠3: Backend `check` で 1 件失敗（PdfExportServiceTest）

### 症状

`./gradlew check`（全 171 テスト）で 1 件失敗。

```
PdfExportServiceTest > PDF がバイト配列として生成される FAILED
  net.sf.jasperreports.engine.JRRuntimeException: Error initializing graphic environment.
```

### 切り分け（デグレか環境問題か）

打刻ロジックと PDF は無関係のはずだが、**憶測で断定せず stash で実証**した。

```bash
# 自分の変更を退避して、元コードで PDF テストだけ実行
git stash push -- <変更した3ファイル>
./gradlew test --tests "...PdfExportServiceTest"
# → 元コードでも FAILED（同じグラフィック環境エラー）
git stash pop
```

→ **変更前から失敗する既存の環境依存問題**（JasperReports のフォント/グラフィック環境初期化）と確定。
Bug② の修正とは無関係＝デグレではない。

補足: 自分が触った attendance 系テストは全 34 件 green を test-results XML で個別確認済み。

### 教訓

- **「自分の変更で壊れた？」は stash で即断できる**。元コードで再現すれば環境/既存問題と切り分く。
- CI が赤でも、**赤の理由が自分の担当範囲かを必ず確認**。無関係な既存 flaky に引っ張られない。
- 環境依存テスト（PDF/フォント/グラフィック）はヘッドレス環境で落ちやすい。既知の弱点として認識。

---

## 実 API での最終実証（UI テストだけで満足しない）

ユニット/コンポーネントテストが green でも、**実際の HTTP 経路で挙動を確認**した。
CSRF は `CookieCsrfTokenRepository.withHttpOnlyFalse()` 方式（`XSRF-TOKEN` cookie → `X-XSRF-TOKEN` ヘッダ）。

```bash
# tanaka でログイン → cookie に XSRF-TOKEN
# 1回目 clock-in
POST /api/attendance/clock-in  → HTTP 201
# 2回目 clock-in（二重打刻ガード）
POST /api/attendance/clock-in  → HTTP 409  {"detail":"Already clocked in"}
# 退勤
POST /api/attendance/clock-out → HTTP 200
```

→ 修正前は 2 回目も 201 で退勤不可だったのが、狙いどおり 409 で止まり退勤も通るようになった。

### 教訓

- **テストの green と実機の正しさは別物**。特にバグ修正は「実際に症状が消えたか」を実経路で見る。
- API 直叩きは CSRF 方式の把握が要る。設定ファイル（SecurityConfig）を読んで方式を確認してから叩く。

---

## 検証で使ったコマンド早見表

```bash
# Backend: 特定テストだけ
cd packages/backend && ./gradlew test --tests "com.example.attendance.attendance.*"

# Backend: 全体（ArchUnit 含む）
./gradlew check

# Backend: 変更が原因かを stash で切り分け
git stash push -- <files>; ./gradlew test --tests "..."; git stash pop

# Frontend: 特定テスト / 全テスト
cd packages/frontend && npx vitest run src/features/attendance/ClockButtons.test.tsx
npx vitest run

# Frontend: 自分のファイルだけ lint（責任範囲の分離）
npx biome check <files>
npx biome check --write <files>   # safe fix 適用
```
