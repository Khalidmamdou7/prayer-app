package com.azan.prayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.azan.prayer.R
import com.azan.prayer.audio.AdhanVoice
import com.azan.prayer.audio.AdhanVoices
import com.azan.prayer.data.AppSettings
import kotlinx.coroutines.launch

/** Built-in (downloadable) voices, the device alarm sound, or a file the user picks. */
@Composable
fun AdhanVoicePicker(settings: AppSettings, onChange: (AppSettings) -> Unit, onChooseFile: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress = remember { mutableStateMapOf<String, Float>() }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadedVersion by remember { mutableIntStateOf(0) }
    val failedText = stringResource(R.string.download_failed)

    val selectedVoice = AdhanVoices.forUri(context, settings.adhanUri)
    val customFile = settings.adhanUri != null && selectedVoice == null

    fun select(voice: AdhanVoice) =
        onChange(settings.copy(adhanUri = AdhanVoices.uri(context, voice), adhanName = voice.title))

    fun download(voice: AdhanVoice) {
        if (voice.id in progress) return
        progress[voice.id] = 0f
        error = null
        scope.launch {
            try {
                AdhanVoices.download(context, voice) { p -> progress[voice.id] = p }
                downloadedVersion++
                select(voice)
            } catch (e: Exception) {
                error = failedText.format(e.message ?: e.javaClass.simpleName)
            } finally {
                progress.remove(voice.id)
            }
        }
    }

    Column {
        Choice(
            selected = settings.adhanUri == null,
            title = stringResource(R.string.adhan_default),
            subtitle = null,
            onClick = { onChange(settings.copy(adhanUri = null, adhanName = null)) },
        )
        for (voice in AdhanVoices.all) {
            val downloaded = remember(downloadedVersion) { AdhanVoices.isDownloaded(context, voice) }
            val p = progress[voice.id]
            Choice(
                selected = selectedVoice == voice,
                title = voice.title,
                subtitle = voice.credit,
                onClick = { if (downloaded) select(voice) else download(voice) },
                trailing = {
                    when {
                        p != null -> CircularProgressIndicator(progress = { p }, modifier = Modifier.size(24.dp))
                        !downloaded -> TextButton(onClick = { download(voice) }) {
                            Text(stringResource(R.string.download_size, voice.sizeKb / 1000.0))
                        }
                    }
                },
            )
        }
        Choice(
            selected = customFile,
            title = stringResource(R.string.own_file),
            subtitle = if (customFile) settings.adhanName else stringResource(R.string.own_file_hint),
            onClick = onChooseFile,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun Choice(
    selected: Boolean,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}
