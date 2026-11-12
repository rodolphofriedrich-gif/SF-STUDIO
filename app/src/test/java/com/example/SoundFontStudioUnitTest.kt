package com.example

import com.example.audio.ADSR
import com.example.audio.BiquadFilter
import com.example.audio.FilterSettings
import com.example.audio.FilterType
import com.example.audio.ParametricEQ
import com.example.audio.TrackChannel
import com.example.midi.MidiExporter
import com.example.sequencer.SequencerStep
import com.example.soundfont.SFZParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class SoundFontStudioUnitTest {

    @Test
    fun testBiquadFilterProcessing() {
        val filter = BiquadFilter(44100f)
        filter.setLowPass(1000f, 1.0f)

        // Process a test impulse
        val out1 = filter.process(1.0f)
        assertTrue("Output should not be NaN", !out1.isNaN())
        assertTrue("Output should not be infinite", !out1.isInfinite())

        // Ensure decay of filter response
        var last = out1
        for (i in 0 until 100) {
            last = filter.process(0.0f)
        }
        assertTrue("Filter output should decay towards zero", kotlin.math.abs(last) < 0.1f)
    }

    @Test
    fun testParametricEQCalculation() {
        val filter = BiquadFilter(44100f)
        filter.setPeaking(1000f, 6.0f, 1.5f)
        val out = filter.process(1.0f)
        assertTrue("EQ output should be valid float", !out.isNaN())
    }

    @Test
    fun testSFZParserBasic() {
        val sfzContent = """
            // Test SFZ Instrument
            <group>
            ampeg_attack=0.05
            ampeg_decay=0.25
            ampeg_sustain=75
            ampeg_release=0.4
            cutoff=4500
            resonance=2.5

            <region>
            sample=piano_c4.wav
            key=60
            pitch_keycenter=60
        """.trimIndent()

        val parser = SFZParser()
        val parsed = parser.parse(ByteArrayInputStream(sfzContent.toByteArray()), "GrandPiano.sfz")

        assertEquals("GrandPiano", parsed.name)
        assertEquals(1, parsed.regions.size)
        val region = parsed.regions[0]
        assertEquals(60, region.key)
        assertEquals(0.05f, region.attack, 0.001f)
        assertEquals(0.25f, region.decay, 0.001f)
        assertEquals(0.75f, region.sustain, 0.001f)
        assertEquals(0.4f, region.release, 0.001f)
        assertEquals(4500f, region.cutoff, 0.001f)
        assertEquals(2.5f, region.resonance, 0.001f)
        assertEquals(1, parsed.samples.size)
        assertEquals(60, parsed.samples[0].rootKey)
    }

    @Test
    fun testSequencerGridStepIntegrity() {
        val step = SequencerStep(active = true, note = 60, velocity = 100, gate = 0.5f)
        assertTrue(step.active)
        assertEquals(60, step.note)
        assertEquals(100, step.velocity)
    }
}
