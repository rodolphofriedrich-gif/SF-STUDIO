package com.example.sequencer

import com.example.audio.AudioEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SequencerStep(
    val active: Boolean = false,
    val note: Int = 60,            // MIDI note
    val velocity: Int = 100,       // 1 to 127
    val gate: Float = 0.5f,        // Duration fraction (0.1 to 1.0)
    val probability: Float = 1.0f  // 0.0 to 1.0
)

class StepSequencer(
    private val audioEngine: AudioEngine,
    private val scope: CoroutineScope
) {

    // 16 tracks x 16 steps
    private val grid = Array(16) { trackId ->
        val defaultNote = when (trackId) {
            10 -> 36 // Kick
            11 -> 38 // Snare
            12 -> 42 // Closed Hat
            13 -> 46 // Open Hat
            14 -> 39 // Clap
            15 -> 49 // Crash
            1 -> 36  // Bass C2
            else -> 60 // C4
        }
        Array(16) { step ->
            SequencerStep(active = false, note = defaultNote, velocity = 100)
        }
    }

    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _swing = MutableStateFlow(0.0f) // 0.0 to 0.75
    val swing: StateFlow<Float> = _swing.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // Grid revision counter to trigger Compose recomposition
    private val _gridVersion = MutableStateFlow(0)
    val gridVersion: StateFlow<Int> = _gridVersion.asStateFlow()

    private var sequencerJob: Job? = null
    private var lastTapTimes = mutableListOf<Long>()

    init {
        loadPatternPreset("Synthwave")
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(40, 260)
    }

    fun setSwing(newSwing: Float) {
        _swing.value = newSwing.coerceIn(0.0f, 0.75f)
    }

    fun getStep(trackId: Int, step: Int): SequencerStep {
        return grid[trackId.coerceIn(0, 15)][step.coerceIn(0, 15)]
    }

    fun toggleStep(trackId: Int, step: Int) {
        val t = trackId.coerceIn(0, 15)
        val s = step.coerceIn(0, 15)
        val current = grid[t][s]
        grid[t][s] = current.copy(active = !current.active)
        _gridVersion.value++
    }

    fun updateStep(trackId: Int, step: Int, update: (SequencerStep) -> SequencerStep) {
        val t = trackId.coerceIn(0, 15)
        val s = step.coerceIn(0, 15)
        grid[t][s] = update(grid[t][s])
        _gridVersion.value++
    }

    fun clearTrack(trackId: Int) {
        val t = trackId.coerceIn(0, 15)
        for (s in 0..15) {
            grid[t][s] = grid[t][s].copy(active = false)
        }
        _gridVersion.value++
    }

    fun clearAll() {
        for (t in 0..15) {
            for (s in 0..15) {
                grid[t][s] = grid[t][s].copy(active = false)
            }
        }
        _gridVersion.value++
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        sequencerJob = scope.launch(Dispatchers.Default) {
            runSequencerLoop()
        }
    }

    fun pause() {
        _isPlaying.value = false
        sequencerJob?.cancel()
        sequencerJob = null
        audioEngine.allNotesOff()
    }

    fun stop() {
        pause()
        _currentStep.value = 0
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        lastTapTimes.add(now)
        if (lastTapTimes.size > 4) {
            lastTapTimes.removeAt(0)
        }
        if (lastTapTimes.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until lastTapTimes.size) {
                val diff = lastTapTimes[i] - lastTapTimes[i - 1]
                if (diff in 200..2000) intervals.add(diff)
            }
            if (intervals.isNotEmpty()) {
                val avgMs = intervals.average()
                val calculatedBpm = (60000.0 / avgMs).toInt().coerceIn(40, 260)
                setBpm(calculatedBpm)
            }
        }
    }

    private suspend fun runSequencerLoop() {
        while (scope.isActive && _isPlaying.value) {
            val step = _currentStep.value
            val currentBpm = _bpm.value
            val swingAmount = _swing.value

            // Duration of a 16th note in ms: (60,000 / BPM) / 4
            val baseStepMs = (60000.0 / currentBpm) / 4.0

            // Apply swing to even vs odd 16th steps
            val stepDurationMs = if (step % 2 == 1) {
                baseStepMs * (1.0 - swingAmount * 0.5)
            } else {
                baseStepMs * (1.0 + swingAmount * 0.5)
            }

            // Trigger active notes on all 16 tracks for this step
            for (t in 0..15) {
                val stepData = grid[t][step]
                if (stepData.active) {
                    if (stepData.probability >= 1.0f || Random.nextFloat() <= stepData.probability) {
                        val velocityFloat = stepData.velocity / 127.0f
                        audioEngine.noteOn(t, stepData.note, velocityFloat)
                    }
                }
            }

            // Note gate duration before sending note-off
            val gateMs = (stepDurationMs * 0.5).toLong().coerceAtLeast(10L)
            delay(gateMs)

            // Turn off notes that were triggered
            for (t in 0..15) {
                val stepData = grid[t][step]
                if (stepData.active) {
                    audioEngine.noteOff(t, stepData.note)
                }
            }

            val remainingMs = (stepDurationMs - gateMs).toLong().coerceAtLeast(1L)
            delay(remainingMs)

            _currentStep.value = (step + 1) % 16
        }
    }

    fun loadPatternPreset(presetName: String) {
        clearAll()
        when (presetName) {
            "Synthwave" -> {
                setBpm(110)
                // Kick on 0, 4, 8, 12 (four on the floor)
                for (s in listOf(0, 4, 8, 12)) grid[10][s] = grid[10][s].copy(active = true, note = 36)
                // Snare on 4, 12
                for (s in listOf(4, 12)) grid[11][s] = grid[11][s].copy(active = true, note = 38)
                // Closed hat on every 8th note
                for (s in listOf(0, 2, 4, 6, 8, 10, 12, 14)) grid[12][s] = grid[12][s].copy(active = true, note = 42)
                // Open hat on 6, 14
                for (s in listOf(6, 14)) grid[13][s] = grid[13][s].copy(active = true, note = 46)
                // 808 Bass groove (Track 1)
                val bassSteps = listOf(0 to 36, 3 to 36, 6 to 36, 8 to 34, 11 to 34, 14 to 39)
                for ((s, n) in bassSteps) grid[1][s] = grid[1][s].copy(active = true, note = n)
                // Synth Brass chords (Track 3)
                for (s in listOf(0, 6, 8, 14)) grid[3][s] = grid[3][s].copy(active = true, note = 60)
            }
            "Trap 808" -> {
                setBpm(140)
                // Kick
                for (s in listOf(0, 3, 8, 10)) grid[10][s] = grid[10][s].copy(active = true, note = 36)
                // Clap on 4, 12
                for (s in listOf(4, 12)) grid[14][s] = grid[14][s].copy(active = true, note = 39)
                // Rapid Hi-Hats
                for (s in 0..15) grid[12][s] = grid[12][s].copy(active = true, note = 42, velocity = if (s % 4 == 0) 115 else 85)
                // Sub Bass
                for (s in listOf(0, 3, 8, 11)) grid[1][s] = grid[1][s].copy(active = true, note = 36)
            }
            "House" -> {
                setBpm(124)
                for (s in listOf(0, 4, 8, 12)) grid[10][s] = grid[10][s].copy(active = true, note = 36)
                for (s in listOf(4, 12)) grid[11][s] = grid[11][s].copy(active = true, note = 38)
                for (s in listOf(2, 6, 10, 14)) grid[13][s] = grid[13][s].copy(active = true, note = 46) // Off-beat open hat
                for (s in listOf(0, 4, 8, 12)) grid[2][s] = grid[2][s].copy(active = true, note = 64) // Rhodes stab
            }
            "Lo-Fi" -> {
                setBpm(85)
                setSwing(0.35f)
                for (s in listOf(0, 6, 10)) grid[10][s] = grid[10][s].copy(active = true, note = 36)
                for (s in listOf(4, 12)) grid[11][s] = grid[11][s].copy(active = true, note = 38)
                for (s in listOf(0, 2, 4, 6, 8, 10, 12, 14)) grid[12][s] = grid[12][s].copy(active = true, note = 42)
                for (s in listOf(0, 4, 8, 12)) grid[0][s] = grid[0][s].copy(active = true, note = 65) // Piano chords
            }
        }
        _gridVersion.value++
    }

    fun getAllGrid(): Array<Array<SequencerStep>> = grid
}
