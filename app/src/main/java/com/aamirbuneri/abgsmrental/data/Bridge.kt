package com.aamirbuneri.abgsmrental.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** A message the website would have shown at the top of the page ("Tool created.", "Slot updated."). */
@Serializable
data class BridgeFlash(val type: String = "info", val message: String = "")

/**
 * Answer of /api/v1/bridge: the website page's data ([view] + [data]), the result of a form
 * ([redirect] + [flash]) or an internal JSON answer ([json]).
 */
@Serializable
data class BridgeResult(
    val ok: Boolean = false,
    val kind: String = "",
    val status: Int = 200,
    val view: String? = null,
    val title: String = "",
    val data: JsonElement? = null,
    val json: JsonElement? = null,
    val redirect: String? = null,
    val flash: List<BridgeFlash> = emptyList(),
    val error: String? = null,
    /** The page a form went to, opened right away (it may show something only once, like a new API key). */
    val page: BridgePage? = null,
) {
    /** The first success / info message, for a toast. */
    val message: String?
        get() = flash.firstOrNull { it.type == "success" }?.message ?: flash.firstOrNull { it.type != "danger" }?.message

    /** The id at the end of where a form went ("/admin/tools/12/slots" → 12 for "tools"). */
    fun redirectId(after: String): Int? = redirect?.let { Regex("/$after/(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
}

@Serializable
data class BridgePage(val view: String? = null, val title: String = "", val data: JsonElement? = null)

/** A picture (or other file) sent with a form. */
class FilePart(val field: String, val fileName: String, val mime: String, val bytes: ByteArray)

// ── reading the website's raw page data (database rows: numbers may arrive as text) ──

operator fun JsonElement?.get(key: String): JsonElement? = (this as? JsonObject)?.get(key)?.takeIf { it !is JsonNull }

fun JsonElement?.str(key: String, default: String = ""): String = this[key].text() ?: default

fun JsonElement?.text(): String? = when (this) {
    is JsonPrimitive -> if (this is JsonNull) null else contentOrNull
    else -> null
}

fun JsonElement?.int(key: String, default: Int = 0): Int = this[key].text()?.let { it.toIntOrNull() ?: it.toDoubleOrNull()?.toInt() } ?: default

fun JsonElement?.intOrNull(key: String): Int? = this[key].text()?.let { it.toIntOrNull() ?: it.toDoubleOrNull()?.toInt() }

fun JsonElement?.dbl(key: String, default: Double = 0.0): Double = this[key].text()?.toDoubleOrNull() ?: default

fun JsonElement?.dblOrNull(key: String): Double? = this[key].text()?.toDoubleOrNull()

/** "1", 1, true → true. */
fun JsonElement?.bool(key: String): Boolean = when (val p = this[key]) {
    is JsonPrimitive -> p.booleanOrNull ?: (p.contentOrNull?.let { it == "1" || it.equals("true", true) || it.equals("yes", true) } ?: false)
    else -> false
}

fun JsonElement?.list(key: String): List<JsonElement> = when (val v = this[key]) {
    is JsonArray -> v.toList()
    is JsonObject -> v.values.toList() // PHP arrays with non-sequential keys arrive as objects
    else -> emptyList()
}

fun JsonElement?.obj(key: String): JsonObject? = this[key] as? JsonObject

/** Every item of a JSON array/object. */
fun JsonElement?.items(): List<JsonElement> = when (this) {
    is JsonArray -> toList()
    is JsonObject -> values.toList()
    else -> emptyList()
}

/** Form fields for [Api.submit]: strings, numbers, booleans ("1" / "0") and lists. */
fun form(vararg pairs: Pair<String, Any?>): JsonObject = JsonObject(pairs.mapNotNull { (k, v) ->
    when (v) {
        null -> null
        is Boolean -> k to JsonPrimitive(if (v) "1" else "0")
        is Number -> k to JsonPrimitive(v)
        is List<*> -> k to JsonArray(v.map { JsonPrimitive(it?.toString() ?: "") })
        else -> k to JsonPrimitive(v.toString())
    }
}.toMap())
