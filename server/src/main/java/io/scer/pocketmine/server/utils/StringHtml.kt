package io.scer.pocketmine.server.utils

import android.text.Html
import android.os.Build
import android.text.Spanned

fun splitKeepDelimiter(string: String, regex: Regex): MutableList<String> {
    val result = mutableListOf<String>()
    var start = 0
    regex.findAll(string).forEach {
        if (string.substring(start, it.range.first()).isNotEmpty()) {
            result.add(string.substring(start, it.range.first()))
        }
        result.add(it.value)
        start = it.range.last() + 1
    }
    if (start != string.length) result.add(string.substring(start))
    return result
}

fun String.toHTML(): String {
    val csi = "\u001B["
    val color = "${csi}38;5;"

    print(this)

    var tokens = 0
    val builder = StringBuilder()
    splitKeepDelimiter(this, Regex("\\u001B\\[[;\\d]*m")).forEach { token ->
        when (token) {
            "${csi}1m" -> {
                builder.append("<span style=\"font-weight:bold;\">")
                ++tokens
            }
            "${csi}3m" -> {
                builder.append("<span style=\"font-style:italic;\">")
                ++tokens
            }
            "${csi}4m" -> {
                builder.append("<span style=\"text-decoration:underline;\">")
                ++tokens
            }
            "${csi}9m" -> {
                builder.append("<span style=\"text-decoration:line-through;\">")
                ++tokens
            }
            "${csi}m" -> {
                builder.append("</span>".repeat(tokens))
                tokens = 0
            }

            "${color}16m" -> {
                builder.append("<span style=\"color:#000000;\">")
                ++tokens
            }
            "${color}19m" -> {
                builder.append("<span style=\"color:#00a00a;\">")
                ++tokens
            }
            "${color}34m" -> {
                builder.append("<span style=\"color:#0a00a0\">")
                ++tokens
            }
            "${color}37m" -> {
                builder.append("<span style=\"color:#0aa0aa;\">")
                ++tokens
            }
            "${color}124m" -> {
                builder.append("<span style=\"color:#a00a00;\">")
                ++tokens
            }
            "${color}127m" -> {
                builder.append("<span style=\"color:#a0aa0a;\">")
                ++tokens
            }
            "${color}214m" -> {
                builder.append("<span style=\"color:#fa0fa0;\">")
                ++tokens
            }
            "${color}145m" -> {
                builder.append("<span style=\"color:#aaaaaa;\">")
                ++tokens
            }
            "${color}59m" -> {
                builder.append("<span style=\"color:#555555;\">")
                ++tokens
            }
            "${color}63m" -> {
                builder.append("<span style=\"color:#55f55f;\">")
                ++tokens
            }
            "${color}83m" -> {
                builder.append("<span style=\"color:#55ff55;\">")
                ++tokens
            }
            "${color}87m" -> {
                builder.append("<span style=\"color:blue;\">")
                ++tokens
            }
            "${color}203m" -> {
                builder.append("<span style=\"color:blue;\">")
                ++tokens
            }
            "${color}207m" -> {
                builder.append("<span style=\"color:#f5ff5f;\">")
                ++tokens
            }
            "${color}227m" -> {
                builder.append("<span style=\"color:#F57F17;\">")
                ++tokens
            }
            "${color}231m" -> {
                builder.append("<span style=\"color:#000000;\">")
                ++tokens
            }
            else -> builder.append(token)
        }
    }
    builder.append("</span>".repeat(tokens))
    return builder.toString().replace("\n", "<br/>")
}

@Suppress("DEPRECATION")
fun String.fromHtml(): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    else
        Html.fromHtml(this)
}