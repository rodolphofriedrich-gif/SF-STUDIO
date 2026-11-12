package com.example.midi

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.audio.TrackChannel
import com.example.sequencer.SequencerStep
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Standard MIDI File (.mid) Type 1 exporter.
 */
object MidiExporter {

    private const val TICKS_PER_BEAT = 480
    // 16th note = 480 / 4 = 120 ticks
    private const val TICKS_PER_16TH = TICKS_PER_BEAT / 4

    fun exportPatternToMidiFile(
        context: Context,
        grid: Array<Array<SequencerStep>>,
        tracks: List<TrackChannel>,
        bpm: Int,
        filename: String = "SF_Studio_Pattern.mid"
    ): Result<File> {
        return try {
            val file = File(context.cacheDir, filename)
            val fos = FileOutputStream(file)
            val dos = DataOutputStream(fos)

            val trackCount = 17 // Track 0 (Tempo/Meta) + 16 Track Channels

            // 1. MThd Header Chunk
            dos.writeBytes("MThd")
            dos.writeInt(6) // Header length
            dos.writeShort(1) // Format 1 (multi-track)
            dos.writeShort(trackCount)
            dos.writeShort(TICKS_PER_BEAT)

            // 2. Track 0: Tempo & Time Signature
            val tempoTrackBytes = ByteArrayOutputStream()
            val tempoDos = DataOutputStream(tempoTrackBytes)

            // Delta 0: Time Signature 4/4
            writeVlq(tempoDos, 0)
            tempoDos.writeByte(0xFF)
            tempoDos.writeByte(0x58)
            tempoDos.writeByte(0x04)
            tempoDos.writeByte(0x04) // numerator
            tempoDos.writeByte(0x02) // denominator (2^2 = 4)
            tempoDos.writeByte(0x18) // 24 MIDI clocks per metronome click
            tempoDos.writeByte(0x08) // 8 32nd notes per quarter note

            // Delta 0: Set Tempo (micro-seconds per quarter note)
            val mpqn = 60_000_000 / bpm.coerceIn(30, 300)
            writeVlq(tempoDos, 0)
            tempoDos.writeByte(0xFF)
            tempoDos.writeByte(0x51)
            tempoDos.writeByte(0x03)
            tempoDos.writeByte((mpqn shr 16) and 0xFF)
            tempoDos.writeByte((mpqn shr 8) and 0xFF)
            tempoDos.writeByte(mpqn and 0xFF)

            // End of Track
            writeVlq(tempoDos, TICKS_PER_16TH * 16)
            tempoDos.writeByte(0xFF)
            tempoDos.writeByte(0x2F)
            tempoDos.writeByte(0x00)

            writeTrackChunk(dos, tempoTrackBytes.toByteArray())

            // 3. Tracks 1 to 16
            for (t in 0..15) {
                val trackData = tracks.getOrNull(t)
                val trackBytes = ByteArrayOutputStream()
                val trkDos = DataOutputStream(trackBytes)
                val midiCh = ((trackData?.midiChannel?.minus(1) ?: t) and 0x0F)

                // Track Name Meta Event
                val name = trackData?.name ?: "Track ${t + 1}"
                writeVlq(trkDos, 0)
                trkDos.writeByte(0xFF)
                trkDos.writeByte(0x03)
                writeVlq(trkDos, name.length)
                trkDos.writeBytes(name)

                // Sequence events
                var currentTick = 0
                for (step in 0..15) {
                    val stepData = grid[t][step]
                    val stepStartTick = step * TICKS_PER_16TH
                    val noteDurationTicks = (TICKS_PER_16TH * stepData.gate).toInt().coerceAtLeast(20)

                    if (stepData.active) {
                        // Delta to Note On
                        val deltaOn = stepStartTick - currentTick
                        writeVlq(trkDos, deltaOn)
                        trkDos.writeByte(0x90 or midiCh)
                        trkDos.writeByte(stepData.note.coerceIn(0, 127))
                        trkDos.writeByte(stepData.velocity.coerceIn(1, 127))
                        currentTick = stepStartTick

                        // Delta to Note Off
                        writeVlq(trkDos, noteDurationTicks)
                        trkDos.writeByte(0x80 or midiCh)
                        trkDos.writeByte(stepData.note.coerceIn(0, 127))
                        trkDos.writeByte(0x00)
                        currentTick += noteDurationTicks
                    }
                }

                // End of track event at pattern end (16 steps)
                val totalTicks = 16 * TICKS_PER_16TH
                val finalDelta = (totalTicks - currentTick).coerceAtLeast(0)
                writeVlq(trkDos, finalDelta)
                trkDos.writeByte(0xFF)
                trkDos.writeByte(0x2F)
                trkDos.writeByte(0x00)

                writeTrackChunk(dos, trackBytes.toByteArray())
            }

            dos.flush()
            dos.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeTrackChunk(dos: DataOutputStream, data: ByteArray) {
        dos.writeBytes("MTrk")
        dos.writeInt(data.size)
        dos.write(data)
    }

    private fun writeVlq(dos: DataOutputStream, value: Int) {
        var buffer = value and 0x7F
        var v = value ushr 7
        while (v > 0) {
            buffer = buffer shl 8
            buffer = buffer or ((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        while (true) {
            dos.writeByte(buffer and 0xFF)
            if ((buffer and 0x80) != 0) {
                buffer = buffer ushr 8
            } else {
                break
            }
        }
    }

    fun shareMidiFile(context: Context, file: File) {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            // Fallback for simple share
            android.net.Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/midi"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Padrão MIDI exportado do SF Studio")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Exportar / Compartilhar Arquivo MIDI")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
