package com.cookbookanything.app.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * What the app says through the phone's vibration: one kind of tap per kind of event, the same on
 * every screen. The system plays them, so they follow the phone's own touch-feedback setting, and a
 * phone that can only buzz plays nothing rather than a buzz. Never on scrolling or plain taps.
 */
object Touch {
    /** A tick put on: an ingredient, a step, a favourite. */
    val Check = HapticFeedbackType.ToggleOn
    val Uncheck = HapticFeedbackType.ToggleOff

    /** Something is now kept: a recipe saved, a meal planned, a dish cooked. */
    val Done = HapticFeedbackType.Confirm

    /** The app will not do it as asked: a recipe without a name. */
    val Refused = HapticFeedbackType.Reject

    /** A row leaves its place under the finger. */
    val PickUp = HapticFeedbackType.GestureThresholdActivate

    /** One notch: a row passes another, a star, a serving more or less. */
    val Notch = HapticFeedbackType.SegmentTick
}

fun HapticFeedback.toggle(on: Boolean) = performHapticFeedback(if (on) Touch.Check else Touch.Uncheck)
