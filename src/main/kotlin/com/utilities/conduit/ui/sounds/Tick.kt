package com.utilities.conduit.ui.sounds

import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.getValue

object Tick {
    private val clip: Clip by lazy {
        val stream = Tick::class.java.getResourceAsStream(
            "/assets/sounds/tick.wav"
        ) ?: error("Could not find tick.wav")

        AudioSystem.getClip().also { clip ->
            stream.use {
                clip.open(AudioSystem.getAudioInputStream(
                    BufferedInputStream(it)))
            }
        }
    }

    fun play() {
        if (clip.isRunning) {
            clip.stop()
        }

        clip.framePosition = 0
        clip.start()
    }
}
