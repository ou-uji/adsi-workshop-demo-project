# 14. SageMaker Code Editor でのブラウザプレビュー対応

## プロンプト

> npm run dev:sagemaker を実行して、 port:3000 に紐づいたこれにアクセスしても Unsupported URL Path になる
> https://<studio-domain>.studio.ap-northeast-1.sagemaker.aws/ports/3000/ports/3000

## 背景

`packages/frontend`（Next.js 16）を SageMaker Studio **Code Editor**（code-server ベース、
base-path `/codeeditor/default`、port 8888）上で起動し、ブラウザでプレビューしたい。

## 切り分け（推測を排し、実測で確定）

### プロキシ本体の解析

`out/vs/server/node/proxyServer.js`（`server-main.js`）を直接読み、転送ロジックを確定:

```js
K2 = ["ports", "absports"];          // 許可セグメント
const [i, n, r] = s.split("/", 3);   // s = code-server が base-path を剥がした後のパス
if (!K2.includes(n)) return;
let o;
return n === "ports" && (o = `/ports/${r}`, s = s.slice(o.length)),
  { base: o, port: r, target: resolve(`http://0.0.0.0:${r}/`, s) };

// 応答処理: base があるとき（= ports）だけ location に前置
e.headers.location.startsWith("/") && t.base &&
  (e.headers.location = t.base + e.headers.location);
```

- `ports`: `/ports/<port>` を剥がして転送し、**応答 location に `/ports/<port>` を前置**
  → basePath 付き SPA だと location が二重化（`/ports/3000/ports/3000` → "Unsupported URL path"）。
  **PORTS タブの地球儀ボタンはこの `ports` 形式を生成するため SPA では使えない。**
- `absports`: プレフィックスを剥がさず・location 前置もしない（`base=undefined`）。

### プローブサーバーで「アプリに届く生パス」を実測

ターミナルからは外側ゲートウェイ経路を再現できない（ポート登録がブラウザ WS セッション単位）。
そこで「届いたパスを表示するだけの極小サーバー」を 3000 に立て、ブラウザで開いて実測した:

| 開き方 | URL | 結果 | アプリに届く `req.url` |
|---|---|---|---|
| 地球儀ボタン | `/ports/3000/ports/3000` | Unsupported URL path | （到達せず） |
| 手打ち | `/codeeditor/default/absports/3000/` | プローブ表示 | **`/absports/3000/`** |

判明した事実:
1. 正しい入口は `/codeeditor/default/absports/3000/`。
2. code-server が `/codeeditor/default` を**剥がし**、`absports` は剥がさず転送 → アプリには
   `/absports/3000/...` が届く。

## つまずき: basePath を `/absports/3000` 単独にすると Unsupported URL path 再発

最初は basePath を `/absports/3000` にして `next start` した。プローブ（200 を返すだけ）は表示できたが、
実アプリでは Next.js がルート `/` で `/dashboard` へ **307 リダイレクト**する。その `Location` は
basePath 由来の `/absports/3000/dashboard`（`/codeeditor/default` が付かない）。ブラウザはこれを
`https://<domain>/absports/3000/dashboard`（codeeditor プレフィックス欠落）へ解決 → ゲートウェイが
知らないパス → **Unsupported URL path**。

**根本原因（basePath 一つでは両立不能）:**
- 受信マッチには basePath = `/absports/3000` が必要（code-server が `/codeeditor/default` を剥がすため）
- ブラウザが出す URL（リダイレクト先・アセット・fetch）は `/codeeditor/default/absports/3000/...` で
  ないとゲートウェイを通らない
- `absports` は応答 location を書き換えない → 一つの basePath で受信と送信を両立できない

## 解決: フル basePath + 復元プロキシ

basePath を**フルパス** `/codeeditor/default/absports/3000` に統一し、code-server が剥がした
`/codeeditor/default` を**復元する極小リバースプロキシ**を 3000 に挟む。

```
ブラウザ  https://<domain>/codeeditor/default/absports/3000/...
  └ ゲートウェイ → code-server(8888) が "/codeeditor/default" を剥がす
    └ absports は剥がさず転送 → proxy(3000) に "/absports/3000/..." が届く
      └ "/codeeditor/default" を前置して next(3001) へ転送
```

next の basePath がフルパスなので、next が返すリダイレクト Location・アセット URL・fetch 先は
すべて `/codeeditor/default/absports/3000/...` となり、ブラウザから見て正しいパスになる。
`absports` は応答 location を書き換えないので、プレフィックス復元は proxy の「受信時」だけでよい。

## やったこと

### `packages/frontend/scripts/sagemaker-proxy.mjs`（新規）
- 3000 で受け、`/codeeditor/default` を前置して 3001 の `next start` へ転送する極小プロキシ。

### `packages/frontend/next.config.ts`
- `SAGEMAKER=1` のとき（`NODE_ENV` と独立した専用フラグ）: static export を無効化、
  `basePath`/`assetPrefix` = `NEXT_PUBLIC_BASE_PATH`、`skipTrailingSlashRedirect`、
  `/api/*` → `localhost:8080` の rewrites を有効化。
- 本番デプロイ（`output: "export"`）は dev / SAGEMAKER 以外のときだけ。

### `packages/frontend/src/lib/api-client.ts`（バグ修正）
- `fetch("/api/...")` の絶対パスはブラウザが basePath を付与しないため、プロキシ経路を通らず
  ゲートウェイ直下に飛んでいた（全 API が失敗）。`withBasePath()` を追加し、全 fetch と
  401 時の `window.location` 遷移に適用。本番（basePath 空）では無影響。

### `packages/frontend/src/features/report/report-api.ts`
- CSV/PDF ダウンロードの `fetch` も `withBasePath` 対応。

### `package.json` の `dev:sagemaker`
```
cd packages/frontend
  && SAGEMAKER=1 NEXT_PUBLIC_BASE_PATH=/codeeditor/default/absports/3000 npx next build
  && ( SAGEMAKER=1 NEXT_PUBLIC_BASE_PATH=/codeeditor/default/absports/3000 npx next start -p 3001
       & node scripts/sagemaker-proxy.mjs )
```

## 起動手順

- backend: `npm run boot:workshop`（H2、Docker 不要）
- frontend: `npm run dev:sagemaker`
- ブラウザ: `https://<domain>/codeeditor/default/absports/3000/` を**直接開く**
  （PORTS タブの地球儀ボタンは `ports` 形式で二重化するため使わない）

## ローカル検証結果（proxy 3000 → next 3001、code-server 剥がし後を模擬）

- `GET /absports/3000/` → 307、Location = `/codeeditor/default/absports/3000/dashboard`（欠落なし）
- アセット `/codeeditor/default/absports/3000/_next/static/chunks/*.js` → 剥がし後 200
- `/absports/3000/api/auth/me` → 401（rewrites でバックエンド到達、未認証の正常応答）

ブラウザで `/codeeditor/default/absports/3000/` を開いてログイン画面が表示・動作することを確認済み。

## 再セットアップ時の落とし穴（環境を作り直したとき）

環境をクローンし直した・別マシンで動かすなど、フロントエンドを再セットアップした直後に
`/ports/3000/dashboard` が Unsupported URL Path、`/codeeditor/default/absports/3000` が 404 に
なる場合、原因はほぼ以下のどちらか。**症状は同じでも起動状態が違う**ので順に確認する。

### 1. 通常の `npm run dev` で起動している（→ 404）

`npm run dev` は basePath なしで 3000 に直接 `next dev` を立てる。この状態では
`/codeeditor/default/absports/3000/...` にマッチするルートが存在せず **404**（`/ports/3000/...` は
そもそも SPA で二重化して Unsupported URL Path）。

SageMaker プレビューでは**必ず `npm run dev:sagemaker` を使う**。これがフル basePath
（`/codeeditor/default/absports/3000`）でビルドし、3001 に `next start`、3000 に復元プロキシ、
という上記の正しい構成を立てる。`npm run dev` と `npm run dev:sagemaker` を取り違えないこと。

確認コマンド（code-server が `/codeeditor/default` を剥がした後を模擬）:

```bash
# 307 で Location が /codeeditor/default/absports/3000/dashboard（接頭辞欠落なし）なら正しい構成
curl -s -o /dev/null -w "status=%{http_code} location=%{redirect_url}\n" \
  http://localhost:3000/absports/3000/
```

### 2. フロントエンドの依存が未インストール（→ build 失敗）

`packages/frontend` で `npm install` していないと `dev:sagemaker` が失敗する。症状は2通り:

- `sh: 1: next: not found` — `node_modules/.bin/next` が無い。
- `Next.js inferred your workspace root ... couldn't find the Next.js package (next/package.json)
  from ... /packages/frontend/src/app` — `npx next build` が一時DLした next を使い、Turbopack が
  ローカルの `next/package.json` を解決できず `src/app` をルート誤認する。

いずれも **`cd packages/frontend && npm install`** で解決する（ルートの `npm install` とは別に必要）。
`next.config.ts` の `turbopack.root` は既に正しく設定済みなので、触らず依存だけ入れること。

### 補足: プロキシスクリプトは2つある

`dev:sagemaker` が使うのは **`packages/frontend/scripts/sagemaker-proxy.mjs`**（フル basePath 復元版）。
リポジトリ直下の `scripts/sagemaker-proxy.mjs` は旧 `/ports/3000` 版で **使われていない**。
編集する際は前者を対象にすること。
