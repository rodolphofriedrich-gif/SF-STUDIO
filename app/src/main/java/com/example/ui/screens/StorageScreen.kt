package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.soundfont.SoundBankManager
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioHighlight
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StorageScreen(
    soundBankManager: SoundBankManager,
    audioEngine: AudioEngine,
    selectedTrackId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFontIndex by remember { mutableIntStateOf(0) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "arquivo.sf2"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }

            val result = soundBankManager.importSoundFont(uri, fileName)
            result.fold(
                onSuccess = { sf ->
                    importStatusMessage = "Sucesso: ${sf.filename} (${sf.sampleCount} amostras carregadas)"
                    statusIsError = false
                },
                onFailure = { err ->
                    importStatusMessage = "Erro ao carregar: ${err.localizedMessage}"
                    statusIsError = true
                }
            )
        }
    }

    val soundFonts = soundBankManager.getAllLoadedSoundFonts()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Column: SoundFont / SFZ Libraries List & Import Action
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(StudioSurface, RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BANCOS SOUNDFONT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${soundFonts.size} BANCOS",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(soundFonts) { idx, sf ->
                        val isSelected = idx == selectedFontIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) StudioHighlight else StudioCard)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else StudioBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedFontIndex = idx }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sf.filename,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${sf.sampleCount} amostras · ${sf.presets.size} presets",
                                    fontSize = 8.sp,
                                    color = TextMuted
                                )
                            }

                            // Format Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (sf.format) {
                                            "SF2" -> NeonCyan.copy(alpha = 0.2f)
                                            "SF3" -> NeonPurple.copy(alpha = 0.2f)
                                            "SFZ" -> NeonAmber.copy(alpha = 0.2f)
                                            else -> NeonGreen.copy(alpha = 0.2f)
                                        },
                                        RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = sf.format,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (sf.format) {
                                        "SF2" -> NeonCyan
                                        "SF3" -> NeonPurple
                                        "SFZ" -> NeonAmber
                                        else -> NeonGreen
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Import Button & Status Notification
            Column {
                importStatusMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (statusIsError) NeonRed else NeonGreen,
                        maxLines = 2,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        // Open file picker allowing .sf2, .sf3, .sfz and audio files
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Importar", tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "IMPORTAR ARQUIVO SF2 / SF3 / SFZ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Right Column: Presets & Samples inside Selected SoundFont
        val currentFont = soundFonts.getOrNull(selectedFontIndex) ?: soundFonts.firstOrNull()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(StudioSurface, RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "INSTRUMENTOS & AMOSTRAS · ${currentFont?.filename ?: ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                    Text(
                        text = "Vincule qualquer instrumento à Faixa ${selectedTrackId + 1} ou Pad selecionado",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (currentFont != null && currentFont.samples.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(currentFont.samples) { sample ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioCard)
                                .border(0.5.dp, StudioBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(StudioHighlight, RoundedCornerShape(4.dp))
                                        .clickable {
                                            // Play quick preview note
                                            audioEngine.noteOn(selectedTrackId, sample.rootKey, 1.0f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = sample.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${sample.sourceFormat} · ${sample.sampleRate}Hz · Nota raiz ${sample.rootKey} · ${sample.samples.size} amostras",
                                        fontSize = 8.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Action button: Load into current track
                            Button(
                                onClick = {
                                    audioEngine.updateTrack(selectedTrackId) {
                                        it.copy(
                                            soundId = sample.id,
                                            soundName = sample.name,
                                            name = sample.name.take(12)
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioHighlight),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = "CARREGAR NA FAIXA ${selectedTrackId + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma amostra disponível neste banco.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
