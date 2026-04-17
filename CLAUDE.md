# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

kotui は Kotlin Multiplatform を使ったターミナルUI (TUI) 構築ライブラリ。ANSIエスケープシーケンスとrawモード制御によりクロスプラットフォームなTUIアプリケーションを構築する。

## ビルドと実行

```bash
# 全体ビルド
./gradlew build

# JVMで実行（stdinが必要なため standardInput = System.in を設定済み）
./gradlew jvmRun -DmainClass=dev.usbharu.kotui.MainKt --quiet

# macOS Native実行バイナリのビルド
./gradlew linkDebugExecutableMacosArm64
# 実行: build/bin/macosArm64/debugExecutable/kotui.kexe

# JS (Node.js) で実行
./gradlew jsNodeRun

# テスト
./gradlew allTests                    # 全プラットフォーム
./gradlew jvmTest                     # JVMのみ
./gradlew macosArm64Test              # macOS Nativeのみ
./gradlew jsTest                      # JSのみ
```

## アーキテクチャ

Kotlin Multiplatform の `expect`/`actual` パターンでプラットフォーム差異を吸収する構造:

- **commonMain** (`dev.usbharu.kotui`): `expect` 宣言とメインロジック。ターミナル制御の3つの `expect fun` を定義:
  - `enableRawMode()` / `disableRawMode()` — ターミナルのrawモード切替
  - `onKeyPressed(onKey: (Char) -> Unit)` — キー入力のイベントループ
- **jvmMain**: `stty` コマンド経由でrawモード制御、`System.in` でキー読み取り
- **linuxMain / macosArm64Main**: POSIX `termios` API (`tcgetattr`/`tcsetattr`) でrawモード制御、`getchar()` でキー読み取り（`toUInt()` vs `toULong()` のプラットフォーム差あり）
- **mingwX64Main**: Windows Console API (`GetConsoleMode`/`SetConsoleMode`/`ReadConsoleW`) で制御
- **jsMain**: Node.js の `process.stdin` で `setRawMode` / `on("data")` によるイベント駆動

## ターゲットプラットフォーム

JVM, JS (Node.js), Linux x64, macOS ARM64, Windows x64 (mingw)

## 技術的な注意点

- Kotlin 2.3.10, Gradle 9.2.1
- Native ターゲットは `kotlinx.cinterop` の `ExperimentalForeignApi` を使用
- JS ターゲットは `ExperimentalMainFunctionArgumentsDsl` を使用
- TUIアプリのため実行時にターミナルの標準入力が必要（CI等ではテストに注意）
