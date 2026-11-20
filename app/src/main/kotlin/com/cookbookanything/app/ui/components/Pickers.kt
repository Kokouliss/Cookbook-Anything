@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cookbookanything.app.R
import com.cookbookanything.core.domain.model.MealSlot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class PhotoPicker(val fromGallery: () -> Unit, val fromCamera: () -> Unit)

/**
 * Gallery picking uses the system photo picker and the camera hands back a file, so neither needs a
 * storage or camera permission.
 */
@Composable
fun rememberPhotoPicker(newCameraTarget: () -> Uri, onPicked: (List<Uri>) -> Unit): PhotoPicker {
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }
    var cameraTarget by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val target = cameraTarget
        if (saved && target != null) onPicked(listOf(target))
    }
    return remember(gallery, camera) {
        PhotoPicker(
            fromGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            fromCamera = {
                val target = newCameraTarget()
                cameraTarget = target
                // A phone without any camera app throws here; the button then simply does nothing.
                runCatching { camera.launch(target) }
            },
        )
    }
}

@Composable
fun AddPhotoButton(
    picker: PhotoPicker,
    onBeforePick: () -> Unit = {},
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
    @DrawableRes icon: Int = R.drawable.ic_add_photo,
    label: String = stringResource(R.string.add_photo),
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (iconOnly) {
            AppIconButton(icon, label, onClick = { expanded = true })
        } else {
            OutlinedButton(onClick = { expanded = true }) {
                Text(label)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.photo_from_gallery)) },
                onClick = {
                    expanded = false
                    onBeforePick()
                    picker.fromGallery()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.photo_from_camera)) },
                onClick = {
                    expanded = false
                    onBeforePick()
                    picker.fromCamera()
                },
            )
        }
    }
}

/** Asks for the notification permission the first time a reminder is set (Android 13+). */
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Picks a date and then a time. When [date] is given, only the time is asked. A meal (breakfast, lunch,
 * snack, dinner) can be chosen with the time; it sets the usual hour, which can still be changed.
 */
@Composable
fun DateTimePickerDialog(
    onConfirm: (LocalDateTime, MealSlot?) -> Unit,
    onDismiss: () -> Unit,
    date: LocalDate? = null,
) {
    var pickedEpochDay by rememberSaveable { mutableStateOf(date?.toEpochDay()) }
    var slot by rememberSaveable { mutableStateOf<MealSlot?>(null) }
    val pickedDay = pickedEpochDay

    if (pickedDay == null) {
        val dateState = rememberDatePickerState(
            // The date picker works in UTC days.
            initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedEpochDay = dateState.selectedDateMillis?.let { millis ->
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        }
                    },
                    enabled = dateState.selectedDateMillis != null,
                ) {
                    Text(stringResource(R.string.next))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val context = LocalContext.current
        val timeState = rememberTimePickerState(
            initialHour = 13,
            initialMinute = 0,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MealSlot.entries.forEach { meal ->
                            FilterChip(
                                selected = slot == meal,
                                onClick = {
                                    slot = meal.takeIf { it != slot }
                                    timeState.hour = meal.hour
                                    timeState.minute = 0
                                },
                                label = { Text(stringResource(meal.label())) },
                            )
                        }
                    }
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(LocalDate.ofEpochDay(pickedDay).atTime(timeState.hour, timeState.minute), slot)
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

fun MealSlot.label(): Int = when (this) {
    MealSlot.BREAKFAST -> R.string.slot_breakfast
    MealSlot.LUNCH -> R.string.slot_lunch
    MealSlot.SNACK -> R.string.slot_snack
    MealSlot.DINNER -> R.string.slot_dinner
}
