# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## プロジェクト概要

kotui は Kotlin Multiplatform 上で動作する宣言的 TUI フレームワーク。Jetpack Compose Runtime をターミナルにホストし、独自の `TuiApplier` がノードツリーを構築、`LayoutEngine` (Flex 風) がレイアウトを計算、`TuiRenderer` が前フレームとの差分のみを ANSI で出力する構造。

主要な公開 API は `dev.usbharu.kotui.compose.*` 配下に集約される（`runtime` / `widget` / `modifier` / `layout` / `focus` / `applier` / `node` / `render` / `clipboard`）。プラットフォーム I/O ( raw モード制御、入力ループ、ターミナルサイズ、リサイズ監視) のみ `expect`/`actual` で吸収する。

## モジュール

- `:` (root, artifact 名 `kotui`) — コアランタイム + ウィジェット一式。エントリポイント `dev.usbharu.kotui.MainKt` はデモアプリ。
- `:kotui-image` — Ktor + korim による画像読み込みと `NetworkImage` Composable。Sixel / Kitty 自動切替、テキストフォールバック付き。
- `:kotui-markdown` — `third-party/markdown-kt` (git submodule, `includeBuild`) を使った Markdown レンダラ。

## ビルドと実行

```bash
# 全体ビルド
./gradlew build

# JVM 実行 (TUI なので実ターミナルが必要 — standardInput = System.in を全 JavaExec に設定済み)
./gradlew jvmRun -DmainClass=dev.usbharu.kotui.MainKt --quiet

# JS (Node.js)
./gradlew jsNodeRun

# Native バイナリ (macOS ARM64 例)
./gradlew linkDebugExecutableMacosArm64
# 実行: build/bin/macosArm64/debugExecutable/kotui.kexe

# テスト
./gradlew allTests           # 全プラットフォーム
./gradlew jvmTest            # JVM
./gradlew macosArm64Test     # macOS Native
./gradlew jsTest             # JS

# サブモジュールの取得 (clone 後に必須)
git submodule update --init --recursive
```

`kotui-image` / `kotui-markdown` の JS テストは Gradle 側で無効化されている（`compileTestDevelopmentExecutableKotlinJs` 等）。新たに JS テストを足す場合はこの除外を見直すこと。

## ターゲットプラットフォーム

JVM, JS (Node.js), Linux x64, macOS ARM64, Windows x64 (mingw)

## アーキテクチャ

### ランタイム層 (`compose/runtime/`)

- `runTui(...)` がエントリポイント。`Recomposer` + `BroadcastFrameClock` を立て、`Composition(TuiApplier(rootNode), recomposer)` でツリーを構築する。
- メインループは `runMainLoop { ... }` (`expect`) 内で `select` を回し、`inputChannel` (Channel<InputEvent>, DROP_OLDEST) と `resizeChannel` (CONFLATED) を待機。awaiter があれば 16ms、なければ 50ms のタイムアウトで idle 描画スキップ。
- 入力は `onInputEvent { ev -> ... }` (`expect`, JVM/Native では blocking thread, JS では event-driven) が `KeyEvent` / `PasteEvent` を流す。
- 入力ディスパッチは「フォーカスノード → 親ノードに `onKeyEvent` をバブル → グローバルに `LocalKeyEvent` 更新」の順。`Tab` は `FocusManager.focusNext` に予約。
- `onKey { ev -> ... }` ヘルパは `LocalKeyEvent` を参照しつつ参照同一性で重複処理を防ぐ (アニメーションフレーム時の二重発火対策)。
- `LocalQuit` / `LocalFocusManager` / `LocalKeyEvent` / `LocalClipboard` を CompositionLocal で公開。

### レイアウト層 (`compose/layout/`, `compose/node/`)

- `TuiNode` が中間表現。`layoutPolicy` (`LEAF` / `BOX` / `ROW` / `COLUMN`) と `preferredWidth` / `preferredHeight` / `flexGrow` / `flexBasis` / `layoutGap` 等を持つ。
- `LayoutEngine.layout(root, w, h)` が再帰的に bounds を割り当てる。Flex は CSS 風で `weight` (= flex-grow) と `flexBasis` を見る。
- `Modifier` は Compose 同様の chain。`Modifier.width / height / size / style / focusedStyle / focusable / focusScope / zIndex / gap / weight / flexGrow / flexBasis / offset / onKeyEvent` が定義済 (`compose/modifier/Modifier.kt`)。

### レンダリング層 (`compose/render/`, `render/`)

- `TuiRenderer` がレイアウト済みツリーを `RenderBuffer` に書き、前フレームバッファと差分を取って ANSI を出力する。
- 画像は `RenderBuffer` 上に「占有領域 + プレースメント情報」として置かれ、レンダラが Sixel / Kitty を選んで端末に送信する。`utils/SixelSupport.kt` が DA1 / CSI 16 t を使った能力検出を担当 (`runTui` 起動時に一度だけ probe)。

### プラットフォーム境界 (`Terminal.kt` の `expect`)

```kotlin
expect fun enableRawMode()
expect fun disableRawMode()
expect fun onInputEvent(onEvent: (InputEvent) -> Boolean)   // ブロッキングループ
expect fun terminalSize(): TerminalSize?
expect fun watchTerminalResize(onResize: (TerminalSize) -> Unit): TerminalResizeWatcher
```

実装の選択:
- **jvmMain**: `stty -icanon -echo` でrawモード、`System.in.read()` でバイト単位読み取り、`AnsiKeyDecoder` でデコード。
- **linuxMain / macosArm64Main**: POSIX `termios` (`tcgetattr` / `tcsetattr`)。`getchar()` の戻り型がプラットフォームで `UInt` / `ULong` で異なる点に注意。
- **mingwX64Main**: Windows Console API (`GetConsoleMode` / `SetConsoleMode` / `ReadConsoleW`)。
- **jsMain**: Node.js `process.stdin.setRawMode(true)` + `on("data", ...)`。リサイズは `process.stdout.on("resize")`。

その他の `expect`:
- `compose/runtime/Time.kt`, `compose/runtime/MainLoop.kt` — 時刻取得とメインループの駆動方法。
- `compose/clipboard/SystemClipboardWrite.kt` — OSC 52 / プラットフォーム API でクリップボード書き込み。
- `kotui-image/` の `TerminalImageDecoder.kt` / `KtorHttpClient.kt` — JVM/macOS は landscapist、その他は Ktor + korim。

## 技術スタック

- Kotlin 2.3.10, Gradle 9.2.1
- Jetpack Compose Runtime 1.8.0 (`org.jetbrains.compose.runtime:runtime`)
- kotlinx.coroutines 1.10.2
- Native: `kotlinx.cinterop` の `ExperimentalForeignApi` を使用
- JS: `ExperimentalMainFunctionArgumentsDsl` を使用
- `kotui-image` のみ Ktor 3.4.3 + korim 4.0.10 (`linuxX64` / `mingwX64` / `js`) と landscapist 2.9.7 (`jvm` / `macosArm64`)

## 開発時の注意

- **TUI 実行には実ターミナルが必要**。CI 上で `runTui` 系を直接走らせるテストは書かない。ロジックは `compose/widget/*Ops*.kt` のように純粋関数へ切り出してテストする (`TextEditOpsTest`, `VisualWidgetsTest` を参照)。
- **rawモードの後始末**: `runTui` 内 `cleanup()` で `disableRawMode` / 代替スクリーン off / カーソル復帰 / Bracketed paste off / Kitty 画像消去をまとめて行う。新しい端末状態を変える機能を足す場合は同 `cleanup()` に対応を入れる。
- **キーイベント重複問題**: `LocalKeyEvent` は `neverEqualPolicy` で更新される。直接 `when (LocalKeyEvent.current?.char)` を書くと recomposition ごとに発火するので、必ず `onKey { ... }` ヘルパか `Modifier.onKeyEvent { ... }` を経由する。
- **画像のサポート判定**: `SixelSupport.cached` を読む前に `runTui` が一度 probe している前提。テストや単発ユーティリティで使う場合は明示的に `SixelSupport.detect()` する。
- **ウィジェットの追加方針**: `compose/widget/` の既存ファイルにならい、`ComposeNode<TuiNode, TuiApplier>` で `TuiNode` を生成し、`update` ブロックで modifier 適用 + 状態反映を行う。複雑な振る舞いは Pure Kotlin な `*Ops` ファイルに切り出してテスト可能にする。
- **submodule の編集**: `third-party/markdown-kt` は外部リポジトリ。kotui 側の都合で勝手に編集せず、上流に PR を出すか、kotui-markdown のラッパー側で吸収する。
