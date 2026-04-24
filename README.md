# kotui

Kotlin Multiplatform で動作する宣言的なターミナルUI (TUI) フレームワーク。Jetpack Compose Runtime をターミナル上にホストし、`Modifier` ベースのレイアウトと差分レンダリングで TUI アプリを構築する。

> ステータス: 実験段階 (1.0-SNAPSHOT)。API は予告なく変更されることがある。

## 特長

- **Compose Runtime ベース**: `@Composable` で UI を宣言し、状態変更を recomposition で反映。
- **Flex 風レイアウト**: `Row` / `Column` / `Box` と `Modifier.weight` / `flexBasis` / `width` / `height` などで実用的な伸縮レイアウトを構成。
- **フォーカス管理**: `Tab` での巡回、`Modifier.focusable()` / `focusScope()`、フォーカス時スタイル切替。
- **豊富なウィジェット**: `Text` / `Button` / `TextInput` / `Checkbox` / `RadioGroup` / `Select` / `SelectableList` / `MultiSelectList` / `Modal` / `Panel` / `Spinner` / `ProgressBar` / `Badge` / `Divider` / `Image` など。
- **ターミナル画像表示**: Sixel / Kitty graphics プロトコルを自動検出し、対応端末では実画像、非対応端末ではテキストフォールバック。
- **クロスプラットフォーム**: JVM / Node.js / Linux x64 / macOS ARM64 / Windows x64。
- **リサイズ追従**: SIGWINCH (Unix) / `resize` イベント (Node) / ポーリング (Windows) を自動選択。
- **Bracketed paste 対応**、**システムクリップボード連携** (`LocalClipboard`)。

## 対応プラットフォーム

| ターゲット | 実装 |
| --- | --- |
| JVM | `stty` でrawモード制御、`System.in` で入力 |
| Linux x64 | POSIX `termios` + `getchar()` |
| macOS ARM64 | POSIX `termios` + `getchar()` |
| Windows x64 (mingw) | Windows Console API (`GetConsoleMode` / `ReadConsoleW`) |
| JS (Node.js) | `process.stdin.setRawMode` + `data` イベント |

## モジュール構成

| モジュール | 内容 |
| --- | --- |
| `:` (kotui) | コアランタイム、レイアウトエンジン、レンダラ、ウィジェット |
| `:kotui-image` | URL からの画像読み込み (Ktor + korim)、`NetworkImage` Composable |
| `:kotui-markdown` | [`markdown-kt`](https://github.com/usbharu/markdown-kt) を用いた Markdown レンダリング |

## クイックスタート

```kotlin
import androidx.compose.runtime.*
import dev.usbharu.kotui.compose.runtime.LocalQuit
import dev.usbharu.kotui.compose.runtime.onKey
import dev.usbharu.kotui.compose.runtime.runTui
import dev.usbharu.kotui.compose.widget.*
import dev.usbharu.kotui.compose.modifier.*

fun main() = runTui(fullscreen = true) {
    var count by remember { mutableStateOf(0) }
    val quit = LocalQuit.current

    onKey { ev -> if (ev.char == 'q') quit() }

    Column(gap = 1, modifier = Modifier.focusScope()) {
        Text("Counter: $count")
        Row(gap = 1) {
            Button("+1") { count++ }
            Button("Reset") { count = 0 }
        }
        Text(" Tab=focus  Enter=click  q=quit")
    }
}
```

## ビルドと実行

```bash
# 全体ビルド
./gradlew build

# JVM 実行 (TUI のため stdin が必要 — IDE 内のターミナルか実ターミナルから)
./gradlew jvmRun -DmainClass=dev.usbharu.kotui.MainKt --quiet

# Node.js (JS) 実行
./gradlew jsNodeRun

# macOS ARM64 ネイティブバイナリ
./gradlew linkDebugExecutableMacosArm64
./build/bin/macosArm64/debugExecutable/kotui.kexe

# テスト
./gradlew allTests        # 全プラットフォーム
./gradlew jvmTest         # JVM のみ
```

## サブモジュールの取得

`kotui-markdown` は git submodule として `markdown-kt` を参照する:

```bash
git clone --recurse-submodules https://github.com/usbharu/kotui.git
# 既存クローンの場合
git submodule update --init --recursive
```

## アーキテクチャ概要

```
@Composable content
       │
       ▼
  Composition  ──►  TuiApplier  ──►  TuiNode tree
                                          │
                                          ▼
                                    LayoutEngine  (Flex 風レイアウト)
                                          │
                                          ▼
                                     TuiRenderer   ──►  RenderBuffer (前フレームと差分)
                                          │
                                          ▼
                                    ANSI シーケンス出力
```

- `runTui` がメインループを起動し、入力 (`onInputEvent`) と再描画タイマーを `select` で待機する。
- 入力イベントは `LocalKeyEvent` 経由で配信、または `Modifier.onKeyEvent { ... }` でノード単位にバブル。
- `expect fun enableRawMode()` / `disableRawMode()` / `onInputEvent(...)` / `terminalSize()` / `watchTerminalResize(...)` がプラットフォーム差異の境界。

## ライセンス

未指定 (TODO)。
