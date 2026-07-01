# 同梱フォントについて

`NotoSansJP-Regular.ttf` / `NotoSansJP-Bold.ttf` は、PDF レポート（月次勤怠レポート）で
日本語を表示するために JasperReports の Font Extension（`fonts.xml`）で PDF に埋め込む
日本語フォントです。

## 由来

- 元フォント: **Noto Sans CJK JP**（Google / Adobe）
- ライセンス: **SIL Open Font License 1.1 (OFL)**
- 加工内容:
  1. Noto Sans CJK の `.ttc` から JP フェイスを抽出
  2. CFF アウトラインを TrueType(glyf) アウトラインへ変換
     （OpenPDF での CID-keyed CFF 埋め込み不具合を回避するため）
  3. JIS X 0208（第1・第2水準）＋ かな・英数・記号にサブセットして軽量化

## 注意

- OFL では改変フォントに "Noto" の予約名をそのまま使うことは推奨されないが、
  デモ用途の内部利用のため family 名は元のまま（`Noto Sans JP`）としている。
  外部配布する場合は family 名のリネームを検討すること。
- OFL 全文は元パッケージ（Debian: `fonts-noto-cjk`）の copyright、または
  https://openfontlicense.org/ を参照。
