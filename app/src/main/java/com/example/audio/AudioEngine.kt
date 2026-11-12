package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.example.soundfont.SoundBankManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Real-time low-latency audio engine driving 16 tracks, polyphonic voices,
 * dynamic ADSR, resonant biquad filters, 3-band parametric EQ, and reverb/delay effects.
 */
class AudioEngine(private val soundBankManager: SoundBankManager) {

    private val sampleRate = 44100
    private val bufferFrames = 512 // ~11.6ms buffer at 44.1kHz for ultra-low latency
    private val bufferShorts = bufferFrames * 2 // Stereo

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var engineJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default)

    // 16 Audio Tracks
    private val tracks = Array(16) { id ->
        val defaultSound = when (id) {
            0 -> "piano_grand"
            1 -> "bass_808"
            2 -> "rhodes_tine"
            3 -> "synth_brass"
            4 -> "warm_pad"
            5 -> "pluck_marimba"
            6 -> "funk_bass"
            7 -> "string_ensemble"
            8 -> "acid_lead"
            9 -> "flute_breath"
            10 -> "drum_kick_808"
            11 -> "drum_snare_crisp"
            12 -> "drum_hihat_closed"
            13 -> "drum_hihat_open"
            14 -> "drum_clap_rim"
            15 -> "drum_crash_cymbal"
            else -> "piano_grand"
        }
        val defaultName = when (id) {
            0 -> "Piano"
            1 -> "808 Bass"
            2 -> "Rhodes"
            3 -> "Brass"
            4 -> "Analog Pad"
            5 -> "Marimba"
            6 -> "Slap Bass"
            7 -> "Strings"
            8 -> "Acid Lead"
            9 -> "Flute"
            10 -> "Kick"
            11 -> "Snare"
            12 -> "Closed Hat"
            13 -> "Open Hat"
            14 -> "Clap"
            15 -> "Cymbal"
            else -> "Track ${id + 1}"
        }
        val choke = when (id) {
            12, 13 -> 1 // Hi-hats choke each other
            else -> 0
        }
        TrackChannel(
            id = id,
            name = defaultName,
            volume = 0.85f,
            pan = 0.0f,
            midiChannel = id + 1, // Track 0 = MIDI 1, Track 9 = MIDI 10 (drums standard), etc.
            soundId = defaultSound,
            soundName = defaultName,
            chokeGroup = choke
        )
    }

    // Per-channel filters and DSP
    private val channelFilters = Array(16) { BiquadFilter(sampleRate.toFloat()) }
    private val channelEqLow = Array(16) { BiquadFilter(sampleRate.toFloat()) }
    private val channelEqMid = Array(16) { BiquadFilter(sampleRate.toFloat()) }
    private val channelEqHigh = Array(16) { BiquadFilter(sampleRate.toFloat()) }
    private val channelDelays = Array(16) { StereoDelay(1000, sampleRate) }

    // Master DSP
    private val masterReverb = ReverbEngine(sampleRate)
    var masterVolume: Float = 0.85f

    // Polyphonic active voices (max 48)
    private val activeVoices = mutableListOf<ActiveVoice>()
    private val voiceLock = Any()

    // Real-time VU meter peak levels (0.0 to 1.0)
    private val _trackVUMeters = MutableStateFlow(FloatArray(16) { 0f })
    val trackVUMeters: StateFlow<FloatArray> = _trackVUMeters.asStateFlow()

    private val _masterVUMeter = MutableStateFlow(Pair(0f, 0f))
    val masterVUMeter: StateFlow<Pair<Float, Float>> = _masterVUMeter.asStateFlow()

    // Tracks state flow for UI
    private val _tracksFlow = MutableStateFlow(tracks.toList())
    val tracksFlow: StateFlow<List<TrackChannel>> = _tracksFlow.asStateFlow()

    fun getTrack(id: Int): TrackChannel = synchronized(tracks) { tracks[id.coerceIn(0, 15)] }

    fun updateTrack(id: Int, update: (TrackChannel) -> TrackChannel) {
        synchronized(tracks) {
            val safeId = id.coerceIn(0, 15)
            val updated = update(tracks[safeId])
            tracks[safeId] = updated
            // Update filter DSP coefficients
            channelFilters[safeId].let { f ->
                when (updated.filter.type) {
                    FilterType.LOW_PASS -> f.setLowPass(updated.filter.cutoffHz, updated.filter.resonance)
                    FilterType.HIGH_PASS -> f.setHighPass(updated.filter.cutoffHz, updated.filter.resonance)
                    FilterType.BAND_PASS -> f.setBandPass(updated.filter.cutoffHz, updated.filter.resonance)
                }
            }
            channelEqLow[safeId].setLowShelf(120f, updated.fx.eq.lowShelfGainDb)
            channelEqMid[safeId].setPeaking(updated.fx.eq.midFreqHz, updated.fx.eq.midGainDb, updated.fx.eq.midQ)
            channelEqHigh[safeId].setHighShelf(7500f, updated.fx.eq.highShelfGainDb)
            _tracksFlow.value = tracks.toList()
        }
    }

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufferSize = max(minBufSize, bufferShorts * 4)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(trackBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack?.play()

        engineJob = engineScope.launch(Dispatchers.Default) {
            // Elevate thread priority for audio real-time rendering
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            } catch (_: Exception) {}

            renderAudioLoop()
        }
    }

    fun stop() {
        isPlaying = false
        engineJob?.cancel()
        engineJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        synchronized(voiceLock) {
            activeVoices.clear()
        }
    }

    /**
     * Trigger a note on a specific track channel.
     * Velocity is normalized 0.0 to 1.0 (or MIDI 1..127).
     */
    fun noteOn(trackId: Int, note: Int, velocity: Float) {
        val safeTrackId = trackId.coerceIn(0, 15)
        val track = synchronized(tracks) { tracks[safeTrackId] }
        val sample = soundBankManager.getSample(track.soundId) ?: return

        // Transpose and tuning
        val totalNote = note + track.transpose
        val centsFactor = 2.0.pow((track.fineTuneCents / 1200.0))
        val pitchFactor = 2.0.pow((totalNote - sample.rootKey) / 12.0) * centsFactor
        val step = (sample.sampleRate.toDouble() / sampleRate.toDouble()) * pitchFactor

        val safeVelocity = velocity.coerceIn(0.05f, 1.0f)

        synchronized(voiceLock) {
            // Choke group check
            if (track.chokeGroup > 0) {
                val it = activeVoices.iterator()
                while (it.hasNext()) {
                    val v = it.next()
                    val vTrack = synchronized(tracks) { tracks[v.trackId] }
                    if (vTrack.chokeGroup == track.chokeGroup) {
                        v.stage = EnvelopeStage.RELEASE
                        v.isReleased = true
                    }
                }
            }

            // Cap max polyphony to 48 voices (steal oldest)
            if (activeVoices.size >= 48) {
                activeVoices.sortByDescending { it.ageFrames }
                activeVoices.removeLastOrNull()
            }

            activeVoices.add(
                ActiveVoice(
                    trackId = safeTrackId,
                    note = note,
                    velocity = safeVelocity,
                    sample = sample,
                    sampleStep = step
                )
            )
        }
    }

    /**
     * Release note for a specific track channel.
     */
    fun noteOff(trackId: Int, note: Int) {
        synchronized(voiceLock) {
            for (voice in activeVoices) {
                if (voice.trackId == trackId && voice.note == note && !voice.isReleased) {
                    voice.stage = EnvelopeStage.RELEASE
                    voice.isReleased = true
                }
            }
        }
    }

    fun allNotesOff() {
        synchronized(voiceLock) {
            activeVoices.clear()
        }
    }

    /**
     * High performance real-time audio synthesis and rendering loop.
     */
    private fun renderAudioLoop() {
        val outShorts = ShortArray(bufferShorts)
        val mixedL = FloatArray(bufferFrames)
        val mixedR = FloatArray(bufferFrames)
        val trackPeaks = FloatArray(16)
        var meterReportCounter = 0

        while (isPlaying) {
            mixedL.fill(0f)
            mixedR.fill(0f)
            trackPeaks.fill(0f)

            // Any soloed tracks?
            var hasSolo = false
            for (t in tracks) {
                if (t.isSolo) {
                    hasSolo = true
                    break
                }
            }

            synchronized(voiceLock) {
                val voiceIterator = activeVoices.iterator()
                while (voiceIterator.hasNext()) {
                    val voice = voiceIterator.next()
                    val track = tracks[voice.trackId]

                    // Mute and Solo handling
                    if (track.isMuted || (hasSolo && !track.isSolo)) {
                        voiceIterator.remove()
                        continue
                    }

                    val pcm = voice.sample.samples
                    val pcmLen = pcm.size
                    if (pcmLen == 0) {
                        voiceIterator.remove()
                        continue
                    }

                    val adsr = track.adsr
                    val attackStep = 1.0f / (sampleRate * max(0.002f, adsr.attack))
                    val decayStep = 1.0f / (sampleRate * max(0.002f, adsr.decay))
                    val releaseStep = 1.0f / (sampleRate * max(0.005f, adsr.release))
                    val sustainLevel = adsr.sustain.coerceIn(0.0f, 1.0f)

                    val pan = track.pan.coerceIn(-1.0f, 1.0f)
                    val panL = sqrt((1.0f - pan) * 0.5f)
                    val panR = sqrt((1.0f + pan) * 0.5f)
                    val volume = track.volume * voice.velocity

                    var voiceEnded = false

                    for (frame in 0 until bufferFrames) {
                        // ADSR calculation
                        when (voice.stage) {
                            EnvelopeStage.ATTACK -> {
                                voice.currentEnvelopeLevel += attackStep
                                if (voice.currentEnvelopeLevel >= 1.0f) {
                                    voice.currentEnvelopeLevel = 1.0f
                                    voice.stage = EnvelopeStage.DECAY
                                }
                            }
                            EnvelopeStage.DECAY -> {
                                voice.currentEnvelopeLevel -= decayStep
                                if (voice.currentEnvelopeLevel <= sustainLevel) {
                                    voice.currentEnvelopeLevel = sustainLevel
                                    voice.stage = EnvelopeStage.SUSTAIN
                                }
                            }
                            EnvelopeStage.SUSTAIN -> {
                                voice.currentEnvelopeLevel = sustainLevel
                            }
                            EnvelopeStage.RELEASE -> {
                                voice.currentEnvelopeLevel -= releaseStep
                                if (voice.currentEnvelopeLevel <= 0.001f) {
                                    voice.currentEnvelopeLevel = 0f
                                    voiceEnded = true
                                    break
                                }
                            }
                            EnvelopeStage.OFF -> {
                                voiceEnded = true
                                break
                            }
                        }

                        // Sample interpolation (linear)
                        val posInt = voice.position.toInt()
                        val posFrac = (voice.position - posInt).toFloat()

                        if (posInt >= pcmLen - 1) {
                            if (voice.sample.isLooped && voice.sample.loopEnd > voice.sample.loopStart) {
                                voice.position = voice.sample.loopStart.toDouble()
                            } else {
                                voiceEnded = true
                                break
                            }
                        }

                        val s0 = pcm[posInt].toFloat() / 32768.0f
                        val s1 = if (posInt + 1 < pcmLen) pcm[posInt + 1].toFloat() / 32768.0f else s0
                        val rawSample = s0 + (s1 - s0) * posFrac

                        val voiceSample = rawSample * voice.currentEnvelopeLevel * volume

                        mixedL[frame] += voiceSample * panL
                        mixedR[frame] += voiceSample * panR

                        val absSample = abs(voiceSample)
                        if (absSample > trackPeaks[voice.trackId]) {
                            trackPeaks[voice.trackId] = absSample
                        }

                        voice.position += voice.sampleStep
                        voice.ageFrames++
                    }

                    if (voiceEnded) {
                        voiceIterator.remove()
                    }
                }
            }

            // Apply per-channel EQ, Filter, Drive, and Delay
            for (ch in 0..15) {
                val track = tracks[ch]
                if (track.isMuted) continue

                val filter = channelFilters[ch]
                val eqLow = channelEqLow[ch]
                val eqMid = channelEqMid[ch]
                val eqHigh = channelEqHigh[ch]
                val delay = channelDelays[ch]
                val drive = track.fx.drive
                val delayTime = track.fx.delayTimeMs
                val delayFb = track.fx.delayFeedback
                val delayWet = track.fx.delayWet

                for (frame in 0 until bufferFrames) {
                    var sL = mixedL[frame]
                    var sR = mixedR[frame]

                    // EQ
                    sL = eqHigh.process(eqMid.process(eqLow.process(sL)))
                    sR = eqHigh.process(eqMid.process(eqLow.process(sR)))

                    // Filter
                    sL = filter.process(sL)
                    sR = filter.process(sR)

                    // Saturation
                    if (drive > 0.01f) {
                        sL = DriveProcessor.process(sL, drive)
                        sR = DriveProcessor.process(sR, drive)
                    }

                    // Delay
                    if (delayWet > 0.01f) {
                        val (delL, delR) = delay.process(sL, sR, delayTime, delayFb, delayWet)
                        sL = delL
                        sR = delR
                    }

                    mixedL[frame] = sL
                    mixedR[frame] = sR
                }
            }

            // Master Reverb, Master Volume and Soft Limiter
            var masterPeakL = 0f
            var masterPeakR = 0f

            for (frame in 0 until bufferFrames) {
                var sL = mixedL[frame]
                var sR = mixedR[frame]

                // Master Reverb
                val (revL, revR) = masterReverb.process(sL, sR, 0.6f, 0.15f)
                sL = revL * masterVolume
                sR = revR * masterVolume

                // Soft limiter / tape saturation to prevent harsh digital clipping
                sL = DriveProcessor.process(sL, 0.05f).coerceIn(-1.0f, 1.0f)
                sR = DriveProcessor.process(sR, 0.05f).coerceIn(-1.0f, 1.0f)

                if (abs(sL) > masterPeakL) masterPeakL = abs(sL)
                if (abs(sR) > masterPeakR) masterPeakR = abs(sR)

                outShorts[frame * 2] = (sL * 32767.0f).toInt().toShort()
                outShorts[frame * 2 + 1] = (sR * 32767.0f).toInt().toShort()
            }

            // Stream to AudioTrack
            audioTrack?.write(outShorts, 0, bufferShorts)

            // Update VU meters periodically (~30 fps)
            meterReportCounter++
            if (meterReportCounter >= 3) {
                meterReportCounter = 0
                _trackVUMeters.value = trackPeaks.clone()
                _masterVUMeter.value = Pair(masterPeakL, masterPeakR)
            }
        }
    }
}
