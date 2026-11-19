package com.cookbookanything.core.domain.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val ldJsonScript = Regex(
    """<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val metaTag = Regex("""<meta\s[^>]*>""", RegexOption.IGNORE_CASE)
private val attribute = Regex("""([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
private val htmlTag = Regex("<[^>]+>")
private val entity = Regex("""&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);""")
private val link = Regex("""https?://[^\s<>"']+""")
private val isoDuration = Regex("""P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?""")
private val stepMarker = Regex("""^\s*(?:\d+[.)]|[-•*])\s*""")
private val firstNumber = Regex("""\d+""")
// Instagram's title and description wrap the caption: `name on Instagram: "caption"`.
private val quotedCaption = Regex("""^[^"]*: "(.*)"\s*$""", RegexOption.DOT_MATCHES_ALL)
private val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }

private const val LINK_SURROUNDING_TEXT = 120

/** The link in shared or pasted text, when the text is little more than that link. */
fun linkIn(text: String): String? {
    val url = link.find(text)?.value ?: return null
    return url.takeIf { text.replace(it, "").trim().length <= LINK_SURROUNDING_TEXT }
}

/**
 * The recipe on a web page. Recipe sites carry a schema.org Recipe record for search engines, which
 * gives the name, servings, times, ingredients, steps and picture exactly. Pages without one (an
 * Instagram post or reel, a plain blog) give only their title and description, read like pasted text;
 * for Instagram that description is the caption, and the picture is the post's image or the reel's
 * thumbnail. Null when the page holds no recipe at all.
 */
fun parseRecipeHtml(html: String): ParsedRecipe? {
    val meta = metaTags(html)
    val record = ldJsonScript.findAll(html)
        .mapNotNull { match -> runCatching { lenientJson.parseToJsonElement(match.groupValues[1].trim()) }.getOrNull() }
        .firstNotNullOfOrNull(::findRecipe)
    if (record != null) {
        val parsed = record.toParsedRecipe()
        return if (parsed.imageUrl == null) parsed.copy(imageUrl = meta["og:image"]) else parsed
    }
    val description = meta["og:description"] ?: meta["description"] ?: return null
    val caption = quotedCaption.find(description)?.groupValues?.get(1)
    val text = if (caption != null) caption else listOfNotNull(meta["og:title"], description).joinToString("\n")
    val parsed = parseRecipeText(text)
    return if (parsed.isEmpty) null else parsed.copy(imageUrl = meta["og:image"])
}

private fun metaTags(html: String): Map<String, String> {
    val tags = HashMap<String, String>()
    for (tag in metaTag.findAll(html)) {
        val attributes = attribute.findAll(tag.value).associate { it.groupValues[1].lowercase() to (it.groups[2]?.value ?: it.groups[3]?.value.orEmpty()) }
        val key = attributes["property"] ?: attributes["name"] ?: continue
        val content = attributes["content"] ?: continue
        tags.putIfAbsent(key.lowercase(), content.decodeEntities())
    }
    return tags
}

private fun findRecipe(element: JsonElement): JsonObject? = when (element) {
    is JsonArray -> element.firstNotNullOfOrNull(::findRecipe)
    is JsonObject -> when {
        element.types().any { it.equals("Recipe", ignoreCase = true) } -> element
        else -> element["@graph"]?.let(::findRecipe) ?: element["mainEntity"]?.let(::findRecipe)
    }
    else -> null
}

private fun JsonObject.types(): List<String> = when (val type = this["@type"]) {
    is JsonPrimitive -> listOf(type.content)
    is JsonArray -> type.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    else -> emptyList()
}

private fun JsonObject.toParsedRecipe(): ParsedRecipe {
    val prep = minutes(string("prepTime"))
    val cook = minutes(string("cookTime")) ?: minutes(string("totalTime"))?.let { total -> (total - (prep ?: 0)).takeIf { it > 0 } }
    return ParsedRecipe(
        title = string("name")?.clean()?.ifBlank { null },
        servings = strings(this["recipeYield"]).firstNotNullOfOrNull { firstNumber.find(it)?.value?.toIntOrNull() },
        ingredients = strings(this["recipeIngredient"]).map { it.clean() }.filter { it.isNotBlank() },
        steps = instructions(this["recipeInstructions"]).map { it.clean().replace(stepMarker, "") }.filter { it.isNotBlank() },
        prepMinutes = prep,
        cookMinutes = cook,
        imageUrl = image(this["image"]),
    )
}

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun strings(element: JsonElement?): List<String> = when (element) {
    is JsonPrimitive -> listOf(element.content)
    is JsonArray -> element.flatMap(::strings)
    is JsonObject -> listOfNotNull(element.string("text") ?: element.string("name"))
    null -> emptyList()
}

/** Steps come as one text, a list of texts, HowToStep objects, or HowToSection groups of them. */
private fun instructions(element: JsonElement?): List<String> = when (element) {
    is JsonPrimitive -> element.content.replace(htmlTag, "\n").split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    is JsonArray -> element.flatMap(::instructions)
    is JsonObject -> element["itemListElement"]?.let(::instructions) ?: strings(element)
    null -> emptyList()
}

private fun image(element: JsonElement?): String? = when (element) {
    is JsonPrimitive -> element.contentOrNull
    is JsonArray -> element.firstNotNullOfOrNull(::image)
    is JsonObject -> element.string("url") ?: element.string("contentUrl")
    null -> null
}

/** "PT1H30M" to 90; null when there is no duration or it is zero. */
private fun minutes(iso: String?): Int? {
    val match = iso?.let { isoDuration.matchEntire(it.trim()) } ?: return null
    val (days, hours, mins, seconds) = match.destructured
    val total = (days.toIntOrNull() ?: 0) * 24 * 60 + (hours.toIntOrNull() ?: 0) * 60 + (mins.toIntOrNull() ?: 0) +
        if ((seconds.toIntOrNull() ?: 0) >= 30) 1 else 0
    return total.takeIf { it > 0 }
}

private fun String.clean(): String = replace(htmlTag, " ").decodeEntities().replace(Regex("\\s+"), " ").trim()

private val namedEntities = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ")

private fun String.decodeEntities(): String = entity.replace(this) { match ->
    val name = match.groupValues[1]
    when {
        name.startsWith("#x") -> name.drop(2).toIntOrNull(16)?.let { Character.toChars(it).concatToString() } ?: match.value
        name.startsWith("#") -> name.drop(1).toIntOrNull()?.let { Character.toChars(it).concatToString() } ?: match.value
        else -> namedEntities[name] ?: match.value
    }
}
