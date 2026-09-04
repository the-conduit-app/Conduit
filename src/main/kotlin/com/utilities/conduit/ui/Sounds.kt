package com.utilities.conduit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

object Sounds {
    var isSilent by mutableStateOf(false)

    object Tick {
        private val clip: Clip by lazy {
            val stream = Tick::class.java.getResourceAsStream(
                "/assets/sounds/tick.wav"
            ) ?: error("Could not find tick.wav")

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
            if (clip.isRunning) {
                clip.stop()
            }
            if (!isSilent) {
                clip.framePosition = 0
                clip.start()
            }
        }
    }

    object Whoosh {
        private val clip: Clip by lazy {
            val stream = Whoosh::class.java.getResourceAsStream(
                "/assets/sounds/whoosh.wav"
            ) ?: error("Could not find whoosh.wav")

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
            if (clip.isRunning) {
                clip.stop()
            }

            if (!isSilent) {
                clip.framePosition = 0
                clip.start()
            }
        }
    }

    object Hsoohw {
        private val clip: Clip by lazy {
            val stream = Hsoohw::class.java.getResourceAsStream(
                "/assets/sounds/hsoohw.wav"
            ) ?: error("Could not find hsoohw.wav")

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
            if (clip.isRunning) {
                clip.stop()
            }

            if (!isSilent) {
                clip.framePosition = 0
                clip.start()
            }
        }
    }

    object EnterChime {
        private val clip: Clip by lazy {
            val stream = EnterChime::class.java.getResourceAsStream(
                "/assets/sounds/enterChime.wav"
            ) ?: error("Could not find enterChime.wav")

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
            if (clip.isRunning) {
                clip.stop()
            }

            if (!isSilent) {
                clip.framePosition = 0
                clip.start()
            }
        }
    }

    object Ting {
        private val clip: Clip by lazy {
            val stream = Ting::class.java.getResourceAsStream(
                "/assets/sounds/ting.wav"
            ) ?: error("Could not find ting.wav")

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
            if (clip.isRunning) {
                clip.stop()
            }

            if (!isSilent) {
                clip.framePosition = 0
                clip.start()
            }
        }
    }

    // For Splash Screen
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
            if (clip.isRunning) {
                clip.stop()
            }
            if (!isSilent) {
                clip.framePosition = 0
                clip.start()
            }
        }
        fun stop() { if (clip.isRunning) { clip.stop() } }

        fun fadeOut(durationMs: Long = 1000) {
            if (!clip.isRunning) return

            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val initialGain = gainControl.value
            val minGain = gainControl.minimum

            Thread {
                try {
                    val steps = 20
                    val stepDelay = durationMs / steps

                    for (i in 1..steps) {
                        val fraction = i.toFloat() / steps
                        gainControl.value = initialGain + (minGain - initialGain) * fraction

                        Thread.sleep(stepDelay)
                    }
                } finally {
                    clip.stop()
                    clip.framePosition = 0
                    gainControl.value = initialGain
                }
            }.start()
        }
    }
}
