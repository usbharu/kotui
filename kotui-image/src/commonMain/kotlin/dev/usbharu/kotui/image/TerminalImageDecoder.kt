package dev.usbharu.kotui.image

import kotlinx.coroutines.flow.Flow

/**
 * URL から画像を非同期に読み込み、RGBA のバイト列へデコードするローダ。
 *
 * プラットフォーム実装の割り当て:
 *   - jvm        : landscapist-core + javax.imageio.ImageIO
 *   - macosArm64 : landscapist-core + CoreGraphics / ImageIO.framework
 *   - linuxX64 / mingwX64 / js : ktor-client + korim
 */
internal expect fun loadImage(
    url: String,
    maxPixelWidth: Int?,
    maxPixelHeight: Int?,
): Flow<ImageLoadState>
