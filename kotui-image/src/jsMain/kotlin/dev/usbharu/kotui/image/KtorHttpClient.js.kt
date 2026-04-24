package dev.usbharu.kotui.image

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

internal actual fun createKtorHttpClient(): HttpClient = HttpClient(Js)
