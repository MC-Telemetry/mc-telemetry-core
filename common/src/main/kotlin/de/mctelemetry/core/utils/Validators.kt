package de.mctelemetry.core.utils

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.mctelemetry.core.commands.CommandExceptions
import net.minecraft.network.chat.Component

object Validators {

    fun parseOTelName(reader: StringReader, stopAtInvalid: Boolean = false): String {
        val startCursor = reader.cursor
        if (!reader.canRead() || reader.peek() == ' ') throw CommandExceptions.metricNameEmpty(reader)
        val firstChar = reader.read()
        if (firstChar !in 'a'..'z') throw CommandExceptions.metricNameBadStart(reader)
        var lastChar = firstChar
        var previousWasSeparator = false
        while (reader.canRead()) {
            val char = reader.read()
            when (char) {
                in 'a'..'z', in '0'..'9' -> {
                    previousWasSeparator = false
                }
                '_', '.' -> {
                    if (previousWasSeparator) {
                        throw CommandExceptions.metricNameDoubleDelimiter(reader)
                    }
                    previousWasSeparator = true
                }
                else -> {
                    if(stopAtInvalid){
                        reader.cursor--
                        break
                    }
                    throw CommandExceptions.metricNameInvalidChar(char, reader.cursor - startCursor, reader)
                }
            }
            lastChar = char
        }
        if (!lastChar.isLetterOrDigit()) throw CommandExceptions.metricNameBadEnd(reader)
        return reader.string.substring(startCursor, reader.cursor)
    }

    fun parseOTelName(name: String, stopAtInvalid: Boolean = false): String {
        return parseOTelName(StringReader(name), stopAtInvalid)
    }

    fun validateOTelName(name: String, stopAtInvalid: Boolean = false, forceBoxedMessage: Boolean = false): Component? {
        try {
            parseOTelName(name, stopAtInvalid)
        } catch (e: CommandSyntaxException) {
            val message = e.rawMessage
            if (!forceBoxedMessage && message is Component) {
                return message
            }
            return Component.literal(e.message!!)
        }
        return null
    }
}
