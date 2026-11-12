package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.audio.AudioEngine
import com.example.midi.MidiManagerHelper
import com.example.sequencer.StepSequencer
import com.example.soundfont.SoundBankManager
import com.example.ui.components.TransportBar
import com.example.ui.screens.MidiBleScreen
import com.example.ui.screens.MixerScreen
import com.example.ui.screens.PadsScreen
import com.example.ui.screens.SequencerScreen
import com.example.ui.screens.SoundEditorScreen
import com.example.ui.screens.StorageScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioHighlight
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class StudioTab(val label: String, val icon: ImageVector) {
    PADS("16 PADS", Icons.Default.GridView),
    SEQUENCER("SEQUENCIADOR", Icons.Default.ViewModule),
    MIXER("MIXER (16 CANAIS)", Icons.Default.GraphicEq),
    SOUND_EDITOR("EDITOR ADSR & FILTRO", Icons.Default.Tune),
    STORAGE("SOUNDFONTS SF2/SFZ", Icons.Default.Folder),
    MIDI_BLE("MIDI & BLUETOOTH", Icons.Default.Bluetooth)
}

class MainActivity : ComponentActivity() {

    private lateinit var soundBankManager: SoundBankManager
    private lateinit var audioEngine: AudioEngine
    private lateinit var sequencer: StepSequencer
    private lateinit var midiHelper: MidiManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()

        soundBankManager = SoundBankManager(applicationContext)
        audioEngine = AudioEngine(soundBankManager)
        sequencer = StepSequencer(audioEngine, lifecycleScope)
        midiHelper = MidiManagerHelper(applicationContext, audioEngine, sequencer)

        audioEngine.start()

        setContent {
            MyApplicationTheme {
                MainStudioApp(
                    audioEngine = audioEngine,
                    sequencer = sequencer,
                    soundBankManager = soundBankManager,
                    midiHelper = midiHelper
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        audioEngine.start()
        midiHelper.refreshDevices()
    }

    override fun onPause() {
        super.onPause()
        sequencer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        sequencer.stop()
        audioEngine.stop()
        midiHelper.release()
    }
}

@Composable
fun MainStudioApp(
    audioEngine: AudioEngine,
    sequencer: StepSequencer,
    soundBankManager: SoundBankManager,
    midiHelper: MidiManagerHelper
) {
    var activeTab by remember { mutableStateOf(StudioTab.PADS) }
    var selectedTrackId by remember { mutableIntStateOf(0) }

    val bpm by sequencer.bpm.collectAsState()
    val swing by sequencer.swing.collectAsState()
    val isPlaying by sequencer.isPlaying.collectAsState()
    val currentStep by sequencer.currentStep.collectAsState()
    val masterPeak by audioEngine.masterVUMeter.collectAsState()
    val connectedMidi by midiHelper.connectedDevices.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = StudioBackground,
        topBar = {
            TransportBar(
                bpm = bpm,
                onBpmChange = { sequencer.setBpm(it) },
                onTapTempo = { sequencer.tapTempo() },
                swing = swing,
                onSwingChange = { sequencer.setSwing(it) },
                isPlaying = isPlaying,
                onPlay = { sequencer.play() },
                onPause = { sequencer.pause() },
                onStop = { sequencer.stop() },
                currentStep = currentStep,
                masterVolume = audioEngine.masterVolume,
                onMasterVolumeChange = { audioEngine.masterVolume = it },
                masterPeakL = masterPeak.first,
                masterPeakR = masterPeak.second,
                isMidiConnected = connectedMidi.isNotEmpty(),
                onMidiClick = { activeTab = StudioTab.MIDI_BLE }
            )
        },
        bottomBar = {
            // Main Studio Navigation Tabs
            Surface(
                color = StudioSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .border(width = 1.dp, color = StudioBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) StudioHighlight else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { activeTab = tab }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) NeonCyan else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tab.label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                StudioTab.PADS -> {
                    PadsScreen(
                        audioEngine = audioEngine,
                        selectedTrackId = selectedTrackId,
                        onSelectTrack = { selectedTrackId = it }
                    )
                }
                StudioTab.SEQUENCER -> {
                    SequencerScreen(
                        sequencer = sequencer,
                        audioEngine = audioEngine,
                        selectedTrackId = selectedTrackId,
                        onSelectTrack = { selectedTrackId = it }
                    )
                }
                StudioTab.MIXER -> {
                    MixerScreen(
                        audioEngine = audioEngine,
                        selectedTrackId = selectedTrackId,
                        onSelectTrack = { selectedTrackId = it }
                    )
                }
                StudioTab.SOUND_EDITOR -> {
                    SoundEditorScreen(
                        audioEngine = audioEngine,
                        soundBankManager = soundBankManager,
                        selectedTrackId = selectedTrackId,
                        onSelectTrack = { selectedTrackId = it }
                    )
                }
                StudioTab.STORAGE -> {
                    StorageScreen(
                        soundBankManager = soundBankManager,
                        audioEngine = audioEngine,
                        selectedTrackId = selectedTrackId
                    )
                }
                StudioTab.MIDI_BLE -> {
                    MidiBleScreen(
                        midiHelper = midiHelper,
                        audioEngine = audioEngine,
                        sequencer = sequencer
                    )
                }
            }
        }
    }
}
