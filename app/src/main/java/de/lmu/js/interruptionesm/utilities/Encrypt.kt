package de.lmu.js.interruptionesm.utilities

import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets


class Encrypt {

    companion object {
        fun encryptKey (str: String): String {
            return Hashing.sha256()
                .hashString(str, StandardCharsets.UTF_8)
                .toString()
        }

    }

}