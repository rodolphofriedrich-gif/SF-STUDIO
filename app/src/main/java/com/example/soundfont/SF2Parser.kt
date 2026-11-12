package com.example.soundfont

import com.example.audio.AudioSampleData
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * SoundFont 2 (SF2) parser extracting samples, headers, and presets.
 */
class SF2Parser {

    data class SF2Header(
        val name: String,
        val start: Int,
        val end: Int,
        val loopStart: Int,
        val loopEnd: Int,
        val sampleRate: Int,
        val originalPitch: Int,
        val pitchCorrection: Int,
        val sampleType: Int
    )

    data class SF2Preset(
        val name: String,
        val patchNumber: Int,
        val bankNumber: Int,
        val sampleHeaders: List<SF2Header> = emptyList()
    )

    data class ParsedSoundFont(
        val fontName: String,
        val presets: List<SF2Preset>,
        val samples: List<AudioSampleData>
    )

    fun parse(inputStream: InputStream, filename: String = "SoundFont.sf2"): ParsedSoundFont {
        val bytes = inputStream.readBytes()
        if (bytes.size < 12) {
            throw IllegalArgumentException("Arquivo inválido ou muito curto")
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Verify RIFF header
        val riff = readFourCC(buffer, 0)
        val form = readFourCC(buffer, 8)
        if (riff != "RIFF" || form != "sfbk") {
            throw IllegalArgumentException("Formato SF2 inválido: cabeçalho RIFF sfbk esperado")
        }

        var soundFontName = filename.substringBeforeLast(".")
        var smplOffset = 0
        var smplLength = 0
        val sampleHeaders = mutableListOf<SF2Header>()
        val presets = mutableListOf<SF2Preset>()

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = readFourCC(buffer, offset)
            val chunkSize = buffer.getInt(offset + 4)
            val chunkEnd = min(offset + 8 + chunkSize, bytes.size)

            if (chunkId == "LIST") {
                val listType = readFourCC(buffer, offset + 8)
                var subOffset = offset + 12

                while (subOffset + 8 <= chunkEnd) {
                    val subId = readFourCC(buffer, subOffset)
                    val subSize = buffer.getInt(subOffset + 4)
                    val subDataEnd = min(subOffset + 8 + subSize, chunkEnd)

                    when (subId) {
                        "INAM" -> {
                            val nameLen = min(subSize, 256)
                            val nameBytes = ByteArray(nameLen)
                            System.arraycopy(bytes, subOffset + 8, nameBytes, 0, nameLen)
                            val str = String(nameBytes).trimEnd('\u0000', ' ')
                            if (str.isNotBlank()) soundFontName = str
                        }
                        "smpl" -> {
                            smplOffset = subOffset + 8
                            smplLength = subSize
                        }
                        "shdr" -> {
                            // Each sample header in SF2 is 46 bytes
                            var shdrPos = subOffset + 8
                            while (shdrPos + 46 <= subDataEnd) {
                                val sNameBytes = ByteArray(20)
                                System.arraycopy(bytes, shdrPos, sNameBytes, 0, 20)
                                val sName = String(sNameBytes).trimEnd('\u0000', ' ')

                                val sStart = buffer.getInt(shdrPos + 20)
                                val sEnd = buffer.getInt(shdrPos + 24)
                                val sLoopStart = buffer.getInt(shdrPos + 28)
                                val sLoopEnd = buffer.getInt(shdrPos + 32)
                                val sRate = buffer.getInt(shdrPos + 36)
                                val sKey = buffer.get(shdrPos + 40).toInt() and 0xFF
                                val sCorrection = buffer.get(shdrPos + 41).toInt()
                                val sType = buffer.getShort(shdrPos + 44).toInt() and 0xFFFF

                                if (sEnd > sStart && sRate > 0) {
                                    sampleHeaders.add(
                                        SF2Header(
                                            name = if (sName.isBlank()) "Sample_${sampleHeaders.size + 1}" else sName,
                                            start = sStart,
                                            end = sEnd,
                                            loopStart = sLoopStart,
                                            loopEnd = sLoopEnd,
                                            sampleRate = sRate,
                                            originalPitch = if (sKey in 1..127) sKey else 60,
                                            pitchCorrection = sCorrection,
                                            sampleType = sType
                                        )
                                    )
                                }
                                shdrPos += 46
                            }
                        }
                        "phdr" -> {
                            // Each preset header is 38 bytes
                            var phdrPos = subOffset + 8
                            while (phdrPos + 38 <= subDataEnd) {
                                val pNameBytes = ByteArray(20)
                                System.arraycopy(bytes, phdrPos, pNameBytes, 0, 20)
                                val pName = String(pNameBytes).trimEnd('\u0000', ' ')
                                val pPatch = buffer.getShort(phdrPos + 20).toInt() and 0xFFFF
                                val pBank = buffer.getShort(phdrPos + 22).toInt() and 0xFFFF

                                if (pName != "EOP" && pName.isNotBlank()) {
                                    presets.add(SF2Preset(pName, pPatch, pBank))
                                }
                                phdrPos += 38
                            }
                        }
                    }

                    subOffset = subDataEnd
                    if (subOffset % 2 != 0) subOffset++ // word alignment
                }
            }

            offset = chunkEnd
            if (offset % 2 != 0) offset++ // word alignment
        }

        // Extract PCM audio samples
        val extractedSamples = mutableListOf<AudioSampleData>()
        val totalSamplePoints = smplLength / 2
        val smplShortBuffer = if (smplLength > 0 && smplOffset + smplLength <= bytes.size) {
            val shortBuf = ShortArray(totalSamplePoints)
            val byteBuf = ByteBuffer.wrap(bytes, smplOffset, smplLength).order(ByteOrder.LITTLE_ENDIAN)
            byteBuf.asShortBuffer().get(shortBuf)
            shortBuf
        } else {
            ShortArray(0)
        }

        for (sh in sampleHeaders) {
            val startIdx = max(0, sh.start)
            val endIdx = min(smplShortBuffer.size, sh.end)
            val length = endIdx - startIdx
            if (length > 100) {
                val pcm = ShortArray(length)
                System.arraycopy(smplShortBuffer, startIdx, pcm, 0, length)

                val loopS = max(0, sh.loopStart - sh.start)
                val loopE = min(length - 1, max(loopS + 1, sh.loopEnd - sh.start))
                val isLooped = (sh.sampleType and 1) != 0 || (sh.loopEnd > sh.loopStart && sh.loopEnd <= sh.end)

                extractedSamples.add(
                    AudioSampleData(
                        id = "sf2_${filename}_${extractedSamples.size}_${sh.name.filter { it.isLetterOrDigit() }}",
                        name = sh.name,
                        sampleRate = if (sh.sampleRate in 8000..192000) sh.sampleRate else 44100,
                        rootKey = sh.originalPitch,
                        samples = pcm,
                        isStereo = false,
                        isLooped = isLooped,
                        loopStart = loopS,
                        loopEnd = loopE,
                        sourceFormat = "SF2"
                    )
                )
            }
        }

        return ParsedSoundFont(
            fontName = soundFontName,
            presets = if (presets.isEmpty()) {
                listOf(SF2Preset(soundFontName, 0, 0, sampleHeaders))
            } else presets,
            samples = extractedSamples
        )
    }

    private fun readFourCC(buffer: ByteBuffer, offset: Int): String {
        val b0 = buffer.get(offset).toInt().toChar()
        val b1 = buffer.get(offset + 1).toInt().toChar()
        val b2 = buffer.get(offset + 2).toInt().toChar()
        val b3 = buffer.get(offset + 3).toInt().toChar()
        return "$b0$b1$b2$b3"
    }
}
