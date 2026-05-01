package dev.usbharu.kotui.image

import io.ktor.client.HttpClient
import io.ktor.client.engine.winhttp.WinHttp

internal actual fun createKtorHttpClient(): HttpClient = HttpClient(WinHttp)
