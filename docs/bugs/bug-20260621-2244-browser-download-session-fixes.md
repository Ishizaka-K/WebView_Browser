# Bug: ブラウザ/ダウンロード周りの潜在バグ群（#1〜#5）

## Status

fixing

## 概要

コード全体レビューで洗い出した潜在バグのうち、High/Medium の正しさ関連 5 件を対象とする。
Low（デッドコード等）は本対応の scope 外。

| # | 重要度 | 症状 | 主な該当 |
|---|--------|------|----------|
| 1 | 中〜高 | 正常完了したダウンロードが FAILED 扱いになり破棄される | `downloads/engine/DownloadTaskRunner.kt` |
| 2 | 中 | セッション保存の並行実行で古い構成が後勝ちし、タブ状態が巻き戻る | `browser/BrowserViewModel.kt`, `data/session/SessionRepository.kt` |
| 3 | 中 | タブ一覧サムネイルが空/不完全になる | `browser/webview/TabManager.kt` |
| 4 | 中 | `started` の可視性問題で停止判定がぶれる / 失敗時に通知が出ない | `downloads/service/DownloadService.kt` |
| 5 | 中 | 旧API(26-28)で同名ファイルを上書きし既存DLを破壊 | `downloads/engine/FileStore.kt` |

---

## #1 DL完了検証が enqueue 時推定サイズを使い、正常DLを FAILED 扱いにする

### Expected Behavior
サーバから全データを取得できたダウンロードは COMPLETED になる。

### Actual Behavior
HTTP 200（Range 非対応で先頭取得）時、完了検証が enqueue 時の推定サイズと実体長を突き合わせ、
食い違うと完全取得でも FAILED になり `.part` が無駄になる。

### Root Cause
- `execute()` 冒頭で `var total = entity.totalBytes`（= enqueue 時の `request.contentLength`、WebView `DownloadListener` 由来で不正確になりやすい）。
- HTTP 200 分岐（`DownloadTaskRunner.kt:90-94`）で `total` を応答 body 長へ**再設定していない**。
- `total > 0` だと後段の `if (total <= 0)` 再計算（`:118-124`）もスキップ。
- 最終検証 `if (total > 0 && actualLength != total) -> FAILED`（`:157-160`）。
- 特に「WebView が見た Content-Length（圧縮値など）」と「runner が `Accept-Encoding: identity` で取得した実体長」が食い違うと発生。

### Fix Strategy
- 200 分岐では `total` を応答ベース（`body.contentLength()`、不明なら 0）で再評価し、enqueue 時推定値を完了検証の真値に使わない。
- 206 は従来どおり Content-Range を真値とする。
- total 不明（0）のときは実体長を完了サイズとして許容する既存ロジックを維持。

### Files to Change
- `app/src/main/java/com/example/webviewbrowser/downloads/engine/DownloadTaskRunner.kt`

### Verification Plan
- 既存 `DownloadTaskRunnerTest` を拡張：200 応答で enqueue 時 totalBytes が実体と異なるケースで COMPLETED になること。
- 206 レジューム成功、416 完了判定の既存ケースが回帰しないこと。

---

## #2 persistSession の並行実行で古いスナップショットが後勝ちする

### Expected Behavior
連続したタブ操作後、最後の状態が永続化される。

### Actual Behavior
新規タブ→切替→移動などを連続で行うと、まれに古いタブ構成で全置換され状態が巻き戻る。

### Root Cause
- `BrowserViewModel.persistSession()`（`:243-258`）が操作ごとに `viewModelScope.launch` で `SessionRepository.save` を発行。
- `save` は `TabDao.replaceAll`（`clear()` + `upsertAll()`）。
- コルーチン実行・DB 書込の順序が保証されず、古いスナップショットが後に適用され得る。

### Fix Strategy
- セッション保存を直列化する。案: `Mutex` で `save` を直列化、または最新のみ反映（`MutableStateFlow` + `conflate`/単一保存コルーチン）。
- スナップショットは呼び出し時点で確定済みのため、適用順だけを保証すればよい。

### Files to Change
- `app/src/main/java/com/example/webviewbrowser/browser/BrowserViewModel.kt`
- 必要なら `app/src/main/java/com/example/webviewbrowser/data/session/SessionRepository.kt`

### Verification Plan
- ViewModel テストで連続操作後に最終構成が保存されることを確認（既存 `BrowserReducerTest`/`SessionRepositoryTest` 方針に合わせる）。

---

## #3 captureThumbnail の software draw が空サムネになり得る

### Expected Behavior
タブ一覧で各タブの現在表示がサムネイルとして見える。

### Actual Behavior
ハードウェアアクセラレーション有効な WebView では `Canvas#draw` でコンテンツが描画されず、空/不完全なサムネイルになる端末がある。

### Root Cause
- `TabManager.captureThumbnail()`（`:86-97`）が `webView.draw(canvas)` を使用。
- HW レイヤのコンテンツは software canvas に描画されないことがある。

### Fix Strategy
- `PixelCopy` ベースのキャプチャに統一する（既存の `putThumbnail`/`WebViewCapture` 経路に寄せる）。
- 非同期 API のため、呼び出し側（`captureActiveThumbnail` → タブ一覧表示）のタイミング調整を検討。

### Files to Change
- `app/src/main/java/com/example/webviewbrowser/browser/webview/TabManager.kt`
- 必要なら `app/src/main/java/com/example/webviewbrowser/browser/webview/WebViewCapture.kt`、`BrowserViewModel.kt`

### Verification Plan
- 実機/エミュレータでタブ一覧サムネイルが描画されることを確認（自動テスト困難なため手動確認＋ロジック単体テスト）。

---

## #4 DownloadService の started 可視性 / 失敗時無通知

### Expected Behavior
- アクティブな DL が無くなったら確実に前面解除・停止する。
- 失敗時もユーザーが状態を把握できる。

### Actual Behavior
- `started`（plain var）を main(`onStartCommand`) と IO(observe) から読み書きしており、可視性問題で停止判定がまれにぶれる。
- FAILED 終了時は完了通知も失敗通知も出ない（`publishNewCompletionNotifications` は COMPLETED のみ）。

### Root Cause
- `DownloadService.kt:57,71-72,83` の `started` が非 volatile。
- `:208-230` が COMPLETED のみ通知対象。

### Fix Strategy
- `started` を `@Volatile` か `AtomicBoolean` 化。
- （任意）失敗時の通知追加。最小修正としては可視性修正を必須、失敗通知は scope を確認のうえ実施。

### Files to Change
- `app/src/main/java/com/example/webviewbrowser/downloads/service/DownloadService.kt`

### Verification Plan
- 既存 `DownloadNotificationTest` 方針に沿って、失敗通知を追加する場合はファクトリの単体テストを追加。
- 停止判定は手動確認中心。

---

## #5 旧API(26-28)で同名ファイルを上書き

### Expected Behavior
既存ダウンロードを破壊せず保存する。

### Actual Behavior
API 26-28 で同名ファイルがあると `copyTo(dest, overwrite = true)` で上書きし、既存を破壊。

### Root Cause
- `FileStore.publishToLegacyDownloads()`（`:74-81`）が無条件上書き。
- MediaStore 経路(API29+)は自動採番されるため対象は旧 API のみ。

### Fix Strategy
- 重複時に連番付与（`name (1).ext` など）で衝突回避。

### Files to Change
- `app/src/main/java/com/example/webviewbrowser/downloads/engine/FileStore.kt`

### Verification Plan
- `FileStore` のファイル名衝突回避ロジックを単体テスト（旧 API 経路は `Build.VERSION` 分岐のため、命名関数を切り出してテスト可能にする）。

---

## Affected Scope
ダウンロード機能、ブラウザのタブ/セッション。UI 表示（サムネイル）と永続化、前面サービス挙動。

## Regression Risks
- #1: 206/416 既存ケースのレジューム・完了判定。
- #2: セッション保存の競合解消が新たなデッドロック/保存漏れを生まないこと。
- #3: PixelCopy 非同期化でサムネイル取得タイミングがずれないこと。
- #4: 停止判定変更でサービスが落ちない/前面解除漏れがないこと。
- #5: 命名変更が MediaStore 経路に影響しないこと。

## Codex Investigation Summary
Claude によるコード全体レビューで根本原因を証拠付きで特定済み（各 # の Root Cause 参照）。
Codex worker は本書の Fix Strategy/Files/Verification に従い、worktree/sandbox 内で実装・検証する。

## Resolution
（実装・レビュー完了後に記入）
