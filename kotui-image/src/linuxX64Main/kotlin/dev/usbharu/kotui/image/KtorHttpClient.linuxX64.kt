package dev.usbharu.kotui.image

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl

internal actual fun createKtorHttpClient(): HttpClient = HttpClient(Curl)
