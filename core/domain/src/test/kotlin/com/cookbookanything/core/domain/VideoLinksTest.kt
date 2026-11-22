package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.video.isVideoPost
import com.cookbookanything.core.domain.video.videoLinkOrNull
import com.cookbookanything.core.domain.video.videoPlayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoLinksTest {
    @Test
    fun `only web links are kept, a missing scheme is added`() {
        assertEquals("https://youtu.be/abcdefghijk", videoLinkOrNull("  youtu.be/abcdefghijk "))
        assertEquals("http://example.com/a.mp4", videoLinkOrNull("http://example.com/a.mp4"))
        assertNull(videoLinkOrNull("javascript:alert(1)"))
        assertNull(videoLinkOrNull("file:///sdcard/a.mp4"))
        assertNull(videoLinkOrNull("intent://x#Intent;end"))
        assertNull(videoLinkOrNull("not a link"))
        assertNull(videoLinkOrNull(""))
    }

    @Test
    fun `known sites play through their embed page`() {
        val embed = "https://www.youtube.com/embed/dQw4w9WgXcQ?playsinline=1"
        assertEquals(embed, videoPlayer("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42")?.url)
        assertEquals(embed, videoPlayer("https://m.youtube.com/watch?feature=share&v=dQw4w9WgXcQ")?.url)
        assertEquals(embed, videoPlayer("https://youtu.be/dQw4w9WgXcQ?si=x")?.url)
        assertEquals(embed, videoPlayer("https://youtube.com/shorts/dQw4w9WgXcQ")?.url)
        assertEquals("https://player.vimeo.com/video/76979871", videoPlayer("https://vimeo.com/76979871")?.url)
        assertEquals("https://www.instagram.com/p/DAbc_12-x/embed/", videoPlayer("https://www.instagram.com/reel/DAbc_12-x/?igsh=1")?.url)
        assertEquals("https://www.instagram.com/p/DAbc_12-x/embed/", videoPlayer("https://www.instagram.com/chef/p/DAbc_12-x/")?.url)
        assertEquals("https://www.tiktok.com/embed/v2/7301234567890123456", videoPlayer("https://www.tiktok.com/@chef/video/7301234567890123456?lang=el")?.url)
        val facebook = videoPlayer("https://www.facebook.com/chef/videos/123456789/")
        assertTrue(facebook!!.url.startsWith("https://www.facebook.com/plugins/video.php?href=https%3A%2F%2Fwww.facebook.com%2Fchef%2Fvideos%2F123456789%2F"))
        assertEquals(true, videoPlayer("https://cdn.example.com/clip.MP4?token=1")?.file)
        assertNull(videoPlayer("https://www.example.com/best-moussaka"))
        assertNull(videoPlayer("javascript:alert(1)"))
    }

    @Test
    fun `an Instagram post is a video only when it is a reel`() {
        assertTrue(isVideoPost("https://www.instagram.com/reel/DAbc/"))
        assertFalse(isVideoPost("https://www.instagram.com/p/DAbc/"))
        assertTrue(isVideoPost("https://www.tiktok.com/@chef/video/7301234567890123456"))
        assertTrue(isVideoPost("https://youtu.be/dQw4w9WgXcQ"))
        assertFalse(isVideoPost("https://cdn.example.com/clip.mp4"))
        assertFalse(isVideoPost("https://www.example.com/best-moussaka"))
    }
}
