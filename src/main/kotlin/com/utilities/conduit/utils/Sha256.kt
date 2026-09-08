package com.utilities.conduit.utils

import java.io.File
import java.security.MessageDigest

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")

    file.inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024)

        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest()
        .joinToString("") { "%02x".format(it) }
}
