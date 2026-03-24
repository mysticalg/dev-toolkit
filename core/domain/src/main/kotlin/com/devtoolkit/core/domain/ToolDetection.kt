package com.devtoolkit.core.domain

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun detectBestTool(text: String?): String? {
    val value = text?.trim().orEmpty()
    if (value.isEmpty()) return null
    return when {
        looksLikeJwt(value) -> "jwt"
        looksLikeJson(value) || looksLikeYaml(value) || looksLikeXml(value) -> "json"
        looksLikeUrl(value) || value.startsWith("intent:", ignoreCase = true) -> "url"
        looksLikeEpoch(value) -> "epoch"
        looksLikeBase64(value) -> "base64"
        else -> null
    }
}

fun looksLikeJwt(text: String): Boolean {
    val parts = text.split(".")
    return parts.size == 3 && parts.take(2).all { it.isNotBlank() && looksLikeBase64UrlChunk(it) }
}

fun looksLikeJson(text: String): Boolean {
    val value = text.trim()
    return (value.startsWith("{") && value.endsWith("}")) || (value.startsWith("[") && value.endsWith("]"))
}

fun looksLikeYaml(text: String): Boolean =
    text.lineSequence().count { it.contains(":") && !it.trimStart().startsWith("//") } >= 2

fun looksLikeXml(text: String): Boolean {
    val value = text.trim()
    return value.startsWith("<") && value.endsWith(">")
}

fun looksLikeUrl(text: String): Boolean =
    text.startsWith("http://", ignoreCase = true) ||
        text.startsWith("https://", ignoreCase = true) ||
        text.startsWith("mailto:", ignoreCase = true)

fun looksLikeEpoch(text: String): Boolean =
    text.all(Char::isDigit) && text.length in 10..13

fun looksLikeBase64(text: String): Boolean {
    val compact = text.filterNot(Char::isWhitespace)
    if (compact.length < 8 || compact.length % 4 != 0) return false
    return compact.matches(Regex("^[A-Za-z0-9+/=]+$"))
}

private fun looksLikeBase64UrlChunk(value: String): Boolean =
    value.matches(Regex("^[A-Za-z0-9_-]+$"))

fun decodeUrlComponent(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
