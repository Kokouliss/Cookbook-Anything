package com.cookbookanything.core.domain.text

import com.cookbookanything.core.domain.search.searchKey
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
private val invisible = Regex("""(?is)<(script|style|noscript|svg|template)[^>]*>.*?</\1>|<!--.*?-->""")
private val lineBreak = Regex("""(?i)<br\s*/?>|</(?:p|li|h\d|div|tr|section|article|dd|dt)>""")
private val entity = Regex("""&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);""")
private val link = Regex("""https?://[^\s<>"']+""")
private val isoDuration = Regex("""P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?""")
private val stepMarker = Regex("""^\s*(?:\d+[.)]|[-•*])\s*""")
private val firstNumber = Regex("""\d+""")
private val onlyNumber = Regex("""^\d{1,2}[.)]?$""")
private val pageTitle = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
// Instagram's title and description wrap the caption: `name on Instagram: "caption"`.
private val quotedCaption = Regex("""^[^"]*: "(.*)"\s*$""", RegexOption.DOT_MATCHES_ALL)
private val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }
private val instagramPost = Regex("""instagram\.com/(?!share/)(?:[\w.]+/)?(?:p|reels?|tv)/([\w-]+)""")
private val captionBox = Regex("""<div[^>]*\bclass\s*=\s*["']Caption["'][^>]*>""")
private val captionExtras = Regex("""(?is)<a[^>]*\bCaptionUsername\b[^>]*>.*?</a>|<div[^>]*\bCaptionComments\b.*""")
private val embeddedImage = Regex("""<img[^>]*\bEmbeddedMediaImage\b[^>]*>""")

private const val LINK_SURROUNDING_TEXT = 120
private const val HEADING_LENGTH = 40

/** The link in shared or pasted text, when the text is little more than that link. */
fun linkIn(text: String): String? {
    val url = link.find(text)?.value ?: return null
    return url.takeIf { text.replace(it, "").trim().length <= LINK_SURROUNDING_TEXT }
}

private val addressNoise = Regex("""^https?://(?:www\.|m\.)?|[?#].*$""")

/**
 * What makes two links point at the same recipe: an Instagram post's code, or the address without
 * scheme, "www.", query and trailing slash. Used to spot a recipe already saved from the same page.
 */
fun sourceKey(url: String): String =
    instagramPost.find(url)?.groupValues?.get(1)?.let { "instagram/$it" }
        ?: url.trim().replace(addressNoise, "").trimEnd('/').lowercase()

/**
 * The address of an Instagram post's embed page, or null for any other link. Asked for the post
 * itself, Instagram hands a signed-out app an error page and pushes a browser into its own app; the
 * embed page is the one it shows to everybody.
 * shortcut: instagram.com/share/… links hold no post code and are left alone, follow their redirect first if they turn up.
 */
fun instagramEmbedUrl(url: String): String? =
    instagramPost.find(url)?.let { "https://www.instagram.com/p/${it.groupValues[1]}/embed/captioned/" }

/**
 * The recipe on a web page, tried in this order:
 * 1. a schema.org Recipe record in JSON-LD, which recipe sites carry for search engines;
 * 2. the same record as microdata (`itemprop="recipeIngredient"` and friends) in the page's HTML;
 * 3. the caption of an Instagram embed page, read like pasted text;
 * 4. the visible text from its "Ingredients"/"Υλικά" heading on, read like pasted text;
 * 5. the title and description (for a social post, the caption), read like pasted text.
 * The picture is the record's image or the page's og:image (a reel's thumbnail). Null when the page
 * holds no recipe at all.
 */
fun parseRecipeHtml(html: String): ParsedRecipe? {
    val meta = metaTags(html)
    val title = meta["og:title"] ?: pageTitle.find(html)?.groupValues?.get(1)?.clean()?.ifBlank { null }
    val image = meta["og:image"]

    val record = ldJsonScript.findAll(html)
        .mapNotNull { match -> runCatching { lenientJson.parseToJsonElement(match.groupValues[1].trim()) }.getOrNull() }
        .firstNotNullOfOrNull(::findRecipe)
    if (record != null) {
        val parsed = record.toParsedRecipe()
        return parsed.copy(title = parsed.title ?: title, imageUrl = parsed.imageUrl ?: image)
    }

    val microdata = parseMicrodata(html)
    if (microdata != null) return microdata.copy(title = title, imageUrl = microdata.imageUrl ?: image)

    val caption = instagramCaption(html)?.let(::parseRecipeText)
    if (caption != null && !caption.isEmpty) return caption.copy(imageUrl = instagramImage(html) ?: image)

    val lines = textLines(html)
    val from = lines.indexOfFirst { it.removeSuffix(":").searchKey() in INGREDIENT_HEADERS }
    if (from >= 0) {
        val parsed = parseRecipeText(lines.drop(from).joinToString("\n"))
        if (parsed.ingredients.isNotEmpty()) return parsed.copy(title = title ?: parsed.title, imageUrl = image)
    }

    val description = meta["og:description"] ?: meta["description"] ?: return null
    val quoted = quotedCaption.find(description)?.groupValues?.get(1)
    val parsed = parseRecipeText(quoted ?: listOfNotNull(title, description).joinToString("\n"))
    return if (parsed.isEmpty) null else parsed.copy(imageUrl = image)
}

private fun metaTags(html: String): Map<String, String> {
    val tags = HashMap<String, String>()
    for (tag in metaTag.findAll(html)) {
        val attributes = attributes(tag.value)
        val key = attributes["property"] ?: attributes["name"] ?: attributes["itemprop"] ?: continue
        val content = attributes["content"] ?: continue
        tags.putIfAbsent(key.lowercase(), content.decodeEntities())
    }
    return tags
}

private fun attributes(tag: String): Map<String, String> =
    attribute.findAll(tag).associate { it.groupValues[1].lowercase() to (it.groups[2]?.value ?: it.groups[3]?.value.orEmpty()) }

// ---- JSON-LD ----

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
    // A HowToSection's name ("For the sauce") becomes a heading over its steps.
    is JsonObject -> element["itemListElement"]?.let { listOfNotNull(element.string("name")?.clean()?.ifBlank { null }?.let { name -> name.trimEnd(':') + ":" }) + instructions(it) }
        ?: strings(element)
    null -> emptyList()
}

private fun image(element: JsonElement?): String? = when (element) {
    is JsonPrimitive -> element.contentOrNull
    is JsonArray -> element.firstNotNullOfOrNull(::image)
    is JsonObject -> element.string("url") ?: element.string("contentUrl")
    null -> null
}

// ---- Instagram embed ----

/** The caption is either drawn in a "Caption" box, after the author's name and before the comments, or only held in the page's data. */
private fun instagramCaption(html: String): String? {
    // The author link next to it tells Instagram's box from any other site's "Caption".
    val box = captionBox.find(html)?.takeIf { "CaptionUsername" in html } ?: return jsonString(html, "edge_media_to_caption", "text")
    return textLines(innerHtml(html, box.range.last + 1, "div").replace(captionExtras, "")).joinToString("\n").ifBlank { null }
}

private fun instagramImage(html: String): String? =
    embeddedImage.find(html)?.let { attributes(it.value)["src"]?.decodeEntities() } ?: jsonString(html, "display_url", "display_url")

/**
 * The first string under [key] at or after [anchor], in JSON found anywhere in the page. Instagram
 * also ships its data as JSON inside a JSON string, where every quote is `\"`; that is read too.
 */
private fun jsonString(html: String, anchor: String, key: String): String? {
    // Two back, so the anchor may be the key itself: its opening quote, escaped or not, is still ahead.
    val from = (html.indexOf(anchor).takeIf { it >= 0 } ?: return null).minus(2).coerceAtLeast(0)
    val opening = Regex("""\\?"$key\\?"\s*:\s*(\\?)"""").find(html, from) ?: return null
    val nested = opening.groupValues[1].isNotEmpty()
    // The closing quote is the first one not escaped at its own level: after 0, 2, 4… backslashes, or nested after 1, 5, 9…
    val closing = Regex(if (nested) """(?<!\\)(?:\\{4})*\\"""" else """(?<!\\)(?:\\{2})*"""").find(html, opening.range.last + 1) ?: return null
    var text = html.substring(opening.range.last + 1, closing.range.last - if (nested) 1 else 0)
    repeat(if (nested) 2 else 1) {
        text = runCatching { (lenientJson.parseToJsonElement("\"$text\"") as JsonPrimitive).content }.getOrNull() ?: return null
    }
    return text.ifBlank { null }
}

// ---- microdata ----

/**
 * schema.org as HTML attributes. Each `itemprop` is either a `<meta content>`/`<img src>`, one
 * element per item (`<li itemprop="recipeIngredient">`), or one box holding the whole list.
 */
private fun parseMicrodata(html: String): ParsedRecipe? {
    val ingredients = itemprop(html, "recipeIngredient").flatMap(::textLines)
        .filterNot { it.removeSuffix(":").searchKey() in INGREDIENT_HEADERS }
    if (ingredients.isEmpty()) return null
    val steps = itemprop(html, "recipeInstructions").flatMap(::textLines)
        .filterNot { it.removeSuffix(":").searchKey() in STEP_HEADERS || onlyNumber.matches(it) }
        .map { it.replace(stepMarker, "") }
        // A short line without a full stop between the steps is a section heading, kept as one.
        .map { if (it.length <= HEADING_LENGTH && !it.endsWith(".") && !it.endsWith("!")) it.trimEnd(':') + ":" else it }
    val prep = minutes(itemprop(html, "prepTime").firstOrNull())
    val cook = minutes(itemprop(html, "cookTime").firstOrNull())
        ?: minutes(itemprop(html, "totalTime").firstOrNull())?.let { total -> (total - (prep ?: 0)).takeIf { it > 0 } }
    return ParsedRecipe(
        servings = itemprop(html, "recipeYield").firstNotNullOfOrNull { firstNumber.find(it)?.value?.toIntOrNull() },
        ingredients = ingredients,
        steps = steps,
        prepMinutes = prep,
        cookMinutes = cook,
        imageUrl = itemprop(html, "image").firstOrNull()?.takeIf { it.startsWith("http") },
    )
}

/** The content of every element carrying `itemprop="name"`: an attribute value, or the element's inner HTML. */
private fun itemprop(html: String, name: String): List<String> {
    val opening = Regex("""<(\w+)\b[^>]*\bitemprop\s*=\s*["']$name["'][^>]*>""", RegexOption.IGNORE_CASE)
    return opening.findAll(html).map { match ->
        val attributes = attributes(match.value)
        attributes["content"]?.decodeEntities()
            ?: attributes["src"]
            ?: attributes["datetime"]
            ?: innerHtml(html, match.range.last + 1, match.groupValues[1])
    }.toList()
}

/** From [start] to the close tag that matches an open `<tag>` just before it; the rest of the page if none. */
private fun innerHtml(html: String, start: Int, tag: String): String {
    val tags = Regex("""<(/?)$tag\b[^>]*>""", RegexOption.IGNORE_CASE)
    var depth = 1
    for (match in tags.findAll(html, start)) {
        depth += if (match.groupValues[1].isEmpty()) 1 else -1
        if (depth == 0) return html.substring(start, match.range.first)
    }
    return html.substring(start)
}

/** The visible text of a piece of HTML, one line per block or line break. */
private fun textLines(html: String): List<String> =
    html.replace(invisible, "").replace(lineBreak, "\n").replace(htmlTag, " ").decodeEntities()
        .lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }

// ---- shared ----

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
