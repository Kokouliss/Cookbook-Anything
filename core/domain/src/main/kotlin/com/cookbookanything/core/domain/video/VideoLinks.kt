package com.cookbookanything.core.domain.video

import java.net.URLEncoder

/** How a video link plays inside the app: [url] is the embeddable player page, or the video file itself when [file] is true. */
data class VideoPlayer(val url: String, val file: Boolean)

private val youtube = Regex("""(?:youtube(?:-nocookie)?\.com/(?:watch\?(?:[^#]*&)?v=|shorts/|embed/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})""")
private val vimeo = Regex("""vimeo\.com/(?:video/)?(\d+)""")
private val instagram = Regex("""instagram\.com/(?:[A-Za-z0-9_.]+/)?(?:p|reels?|tv)/([A-Za-z0-9_-]+)""")
private val instagramVideo = Regex("""instagram\.com/(?:[A-Za-z0-9_.]+/)?(?:reels?|tv)/""")
private val tiktok = Regex("""tiktok\.com/(?:@[^/]+/video|embed(?:/v2)?|v)/(\d+)""")
private val facebook = Regex("""(?:facebook\.com/(?:.+/videos/|watch/?\?v=|reel/)|fb\.watch/)""")
private val videoFile = Regex("""\.(?:mp4|webm|m3u8|mov)(?:[?#]|$)""", RegexOption.IGNORE_CASE)

/**
 * A link the cook typed or pasted, made into a web address: a missing "https://" is added, and
 * anything that is not http(s) (javascript:, file:, intent:) is refused with null.
 */
fun videoLinkOrNull(input: String): String? {
    val link = input.trim()
    if (link.isEmpty() || link.any(Char::isWhitespace)) return null
    val scheme = Regex("""^([A-Za-z][A-Za-z0-9+.-]*):""").find(link)?.groupValues?.get(1)?.lowercase()
    return when {
        scheme == null -> if ('.' in link) "https://$link" else null
        scheme == "http" || scheme == "https" -> link
        else -> null
    }
}

/**
 * The player for [link]: the embed page of YouTube, Vimeo, Instagram, TikTok or Facebook, or the file
 * for a direct .mp4, .webm, .m3u8 or .mov link. Null for any other page, which the app shows as is.
 */
fun videoPlayer(link: String): VideoPlayer? {
    val url = videoLinkOrNull(link) ?: return null
    youtube.find(url)?.let { return VideoPlayer("https://www.youtube.com/embed/${it.groupValues[1]}?playsinline=1", file = false) }
    vimeo.find(url)?.let { return VideoPlayer("https://player.vimeo.com/video/${it.groupValues[1]}", file = false) }
    instagram.find(url)?.let { return VideoPlayer("https://www.instagram.com/p/${it.groupValues[1]}/embed/", file = false) }
    tiktok.find(url)?.let { return VideoPlayer("https://www.tiktok.com/embed/v2/${it.groupValues[1]}", file = false) }
    if (facebook.containsMatchIn(url)) {
        return VideoPlayer("https://www.facebook.com/plugins/video.php?href=${URLEncoder.encode(url, "UTF-8")}&show_text=false", file = false)
    }
    if (videoFile.containsMatchIn(url)) return VideoPlayer(url, file = true)
    return null
}

/**
 * True when the page at [url] is itself a video: a YouTube or Vimeo video, a TikTok, an Instagram
 * reel or a Facebook video. An Instagram post (/p/) can be a photo, so it only counts as a video
 * when its page carried one.
 */
fun isVideoPost(url: String): Boolean {
    val player = videoPlayer(url) ?: return false
    return !player.file && (!instagram.containsMatchIn(url) || instagramVideo.containsMatchIn(url))
}
