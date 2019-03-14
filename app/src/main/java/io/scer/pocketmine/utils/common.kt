package io.scer.pocketmine.utils

import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun URL.saveTo(file: File) {
    this.openStream().use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
}