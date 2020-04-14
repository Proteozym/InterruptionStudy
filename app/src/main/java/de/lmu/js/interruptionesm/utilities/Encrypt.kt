package de.lmu.js.interruptionesm.utilities

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class Encrypt {

    companion object {
        fun encryptKey (str: String): String {
            val plaintext: ByteArray =  str.toByteArray()
            val keygen = KeyGenerator.getInstance("AES")
            keygen.init(256)
            val key: SecretKey = keygen.generateKey()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val ciphertext: ByteArray = cipher.doFinal(plaintext)
            val iv: ByteArray = cipher.iv
            return iv.toString()
        }

    }

}