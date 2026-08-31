package com.utilities.conduit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

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
                        ))
                }
            }
        }

        fun play() {
            if (clip.isRunning) {
                clip.stop()
            }

            if (!isSilent) {
                Whoosh.clip.framePosition = 0
                Whoosh.clip.start()
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
                        ))
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
                        ))
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
                        ))
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
}
