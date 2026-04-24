package dev.usbharu.kotui.image

/**
 * プラットフォーム非依存な画像ロードの状態。kotui-image の
 * プラットフォーム実装 (landscapist-core / Ktor+korim など) はすべて
 * この型の [kotlinx.coroutines.flow.Flow] を返す。
 */
internal sealed class ImageLoadState {
    object Loading : ImageLoadState()

    class Success(
        val rgba: ByteArray,
        val pixelWidth: Int,
        val pixelHeight: Int,
    ) : ImageLoadState()

    class Failure(val message: String) : ImageLoadState()
}
