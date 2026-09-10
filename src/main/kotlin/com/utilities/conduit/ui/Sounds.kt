package com.utilities.conduit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utilities.conduit.ui.Sounds.Space.clip
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.jvm.java

private fun createSound(resourceName: String): Sound {
    return Sound(loadClip(resourceName))
}

private fun loadClip(resourceName: String): Clip? {
    val stream = Sounds::class.java.getResourceAsStream("/assets/sounds/$resourceName") ?: return null

    return AudioSystem.getClip().also { clip ->
        stream.use {
            clip.open(AudioSystem.getAudioInputStream(BufferedInputStream(it)))
        }
    }
}

private class Sound(private val clip: Clip?) {
    fun play() {
        if (clip == null) return

        if (clip.isRunning) {
            clip.stop()
        }
        clip.framePosition = 0
        clip.start()
    }

    fun loop() {
        if (clip == null) return

        if (clip.isRunning) {
            clip.stop()
        }
        clip.framePosition = 0
        clip.loop(Clip.LOOP_CONTINUOUSLY)
        clip.start()
    }

    fun stop() {
        clip?.stop()
    }
}

object Sounds {
    var isSilent by mutableStateOf(false)

    // ChatsList Menu active item change
    object Tick {
        private val sound by lazy { createSound("tick.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // Water drop on treeview node click
    object Drop {
        private val sound by lazy { createSound("waterdrop.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // Switch branches in ChatView
    object Swish {
        private val sound by lazy { createSound("swish.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // Teleport to another branch
    object Teleport {
        private val sound by lazy { createSound("teleport.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // onSend dispatches prompt to expert
    object Whoosh {
        private val sound by lazy { createSound("whoosh.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // ESC to cancel and clear InputArea
    object Hsoohw {
        private val sound by lazy { createSound("Hsoohw.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // Enter app when init is done (from splash screen)
    object EnterChime {
        private val sound by lazy { createSound("enterChime.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // When app becomes ready (systeme expert loaded) in splash screen
    object Ready {
        private val sound by lazy { createSound("ready.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // When response from expert is complete
    object Ting {
        private val sound by lazy { createSound("ting.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // When clicking on Conduit portal before app is ready (splash screen)
    object Knock {
        private val sound by lazy { createSound("door_knock.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // When changing packs
    object SwishSwash {
        private val sound by lazy { createSound("swish_swash.wav") }
        fun play() { if (!isSilent) sound.play() }
    }

    // When initializing the system expert
    object Crunch {
        private val sound by lazy { createSound("crunch.wav") }

        fun loop() {
            if (isSilent) return
            sound.loop()
        }
        fun play() {
            if (isSilent) return
            sound.play()
        }
        fun stop() {
            if (isSilent) return
            sound.stop()
        }
    }

    // For Splash Screen (TODO - awaiting refactor to reuse above code)
    object Space {
        private val clip: Clip by lazy {
            val stream = Tick::class.java.getResourceAsStream(
                "/assets/sounds/deep_space.wav"
            ) ?: error("Could not find deep_space.wav")

            AudioSystem.getClip().also { clip ->
                stream.use {
                    clip.open(
                        AudioSystem.getAudioInputStream(
                            BufferedInputStream(it)
                        )
                    )
                }
            }
        }

        fun play() {
            if (isSilent) return

            if (clip.isRunning) {
                clip.stop()
            }
            clip.framePosition = 0
            clip.loop(Clip.LOOP_CONTINUOUSLY)
        }

        fun stop() {
            if (clip.isRunning) { clip.stop() }
        }

        fun fadeOut(durationMs: Long = 1000) {
            if (!clip.isRunning) return

            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val initialGain = gainControl.value
            val minGain = gainControl.minimum

            Thread {
                try {
                    val steps = 1000
                    val stepDelay = durationMs / steps

                    for (i in 1..steps) {
                        val fraction = i.toFloat() / steps
                        gainControl.value = initialGain + (minGain - initialGain) * fraction

                        Thread.sleep(stepDelay)
                    }
                } finally {
                    clip.stop()
                    clip.framePosition = 0
                    gainControl.value = gainControl.maximum
                }
            }.apply {
                isDaemon = true
                start()
            }
        }
    }
}
