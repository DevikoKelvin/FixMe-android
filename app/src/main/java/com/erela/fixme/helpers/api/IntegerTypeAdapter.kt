package com.erela.fixme.helpers.api

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class IntegerTypeAdapter : TypeAdapter<Int>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Int?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Int? {
        val peek = `in`.peek()
        return if (peek == JsonToken.NULL) {
            `in`.nextNull()
            null
        } else if (peek == JsonToken.STRING) {
            val value = `in`.nextString()
            if (value.isEmpty()) {
                0
            } else {
                try {
                    Integer.valueOf(value)
                } catch (e: NumberFormatException) {
                    0
                }
            }
        } else {
            `in`.nextInt()
        }
    }
}
