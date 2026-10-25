package com.excp.podroid.ui.screens.settings

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.excp.podroid.R
import com.excp.podroid.engine.avf.AvfDiskImage
import com.excp.podroid.ui.components.PodroidDestructiveButton
import com.excp.podroid.ui.components.PodroidGhostButton
import com.excp.podroid.ui.components.PodroidListRow
import com.excp.podroid.ui.components.PodroidSectionLabel
import com.excp.podroid.ui.theme.PodroidTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Import or remove the raw disk image the AVF backend boots instead of Alpine
 * (see AvfDiskImage). The copy runs in this screen's scope, so leaving
 * Settings cancels it and keeps the previous image.
 */
@Composable
fun DiskImageSection(enabled: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var present by remember { mutableStateOf(AvfDiskImage.isPresent(ctx)) }
    var copied by remember { mutableStateOf<Long?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            copied = 0L
            try {
                AvfDiskImage.import(ctx, uri) { copied = it }
                present = true
                Toast.makeText(ctx, R.string.disk_image_imported, Toast.LENGTH_SHORT).show()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Toast.makeText(ctx, ctx.getString(R.string.disk_image_import_failed, e.message), Toast.LENGTH_LONG).show()
            } finally {
                copied = null
            }
        }
    }

    PodroidSectionLabel(stringResource(R.string.disk_image))
    val status = when {
        copied != null -> stringResource(R.string.disk_image_importing, Formatter.formatShortFileSize(ctx, copied!!))
        present -> Formatter.formatShortFileSize(ctx, AvfDiskImage.file(ctx).length())
        else -> stringResource(R.string.disk_image_none)
    }
    PodroidListRow(label = stringResource(R.string.disk_image_boot), value = status)
    Spacer(Modifier.height(PodroidTokens.Spacing.SM))
    PodroidGhostButton(
        text = stringResource(R.string.disk_image_import),
        onClick = { picker.launch(arrayOf("*/*")) },
        enabled = enabled && copied == null,
    )
    if (present) {
        Spacer(Modifier.height(PodroidTokens.Spacing.SM))
        PodroidDestructiveButton(
            text = stringResource(R.string.disk_image_remove),
            onClick = { present = !AvfDiskImage.remove(ctx) },
            enabled = enabled && copied == null,
        )
    }
    Spacer(Modifier.height(PodroidTokens.Spacing.MD))
}
