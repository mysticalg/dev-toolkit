package com.devtoolkit.core.domain

enum class ToolCategory(val displayName: String) {
    TEXT_DATA("Text & Data"),
    ENCODE_DECODE("Encode / Decode / Crypto"),
    UTILITIES("Developer Utilities"),
}

data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory,
    val icon: String,
    val isFree: Boolean = false,
)

object ToolRegistry {
    val tools = listOf(
        Tool("json", "JSON Formatter", "Format, validate & convert JSON/YAML/XML", ToolCategory.TEXT_DATA, "data_object", isFree = true),
        Tool("regex", "Regex Tester", "Test patterns with live highlighting", ToolCategory.TEXT_DATA, "manage_search"),
        Tool("diff", "Diff / Compare", "Compare two texts with highlighting", ToolCategory.TEXT_DATA, "compare_arrows"),
        Tool("texttransform", "Text Transform", "Chain text transformations", ToolCategory.TEXT_DATA, "transform"),
        Tool("base64", "Base64", "Encode & decode Base64", ToolCategory.ENCODE_DECODE, "code", isFree = true),
        Tool("url", "URL Encoder", "Encode/decode & parse URLs", ToolCategory.ENCODE_DECODE, "link"),
        Tool("hash", "Hash Generator", "MD5, SHA & HMAC hashing", ToolCategory.ENCODE_DECODE, "fingerprint"),
        Tool("jwt", "JWT Decoder", "Decode & inspect JSON Web Tokens", ToolCategory.ENCODE_DECODE, "token"),
        Tool("epoch", "Epoch Converter", "Unix timestamp conversions", ToolCategory.UTILITIES, "schedule"),
        Tool("color", "Colour Picker", "Pick colours & check contrast", ToolCategory.UTILITIES, "palette"),
        Tool("uuid", "UUID Generator", "Generate UUID, ULID & Nano ID", ToolCategory.UTILITIES, "casino", isFree = true),
        Tool("mockdata", "Mock Data", "Generate fake data & lorem ipsum", ToolCategory.UTILITIES, "dataset"),
    )

    fun getByCategory(category: ToolCategory) = tools.filter { it.category == category }

    fun ordered(toolOrder: List<String>): List<Tool> {
        if (toolOrder.isEmpty()) return tools
        val orderMap = toolOrder.withIndex().associate { (index, id) -> id to index }
        return tools.sortedWith(
            compareBy<Tool> { orderMap[it.id] ?: Int.MAX_VALUE }.thenBy { tools.indexOf(it) }
        )
    }
}
