package com.postindustria.ssai.loader.common

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.xml.bind.DatatypeConverter

class Utils {
    companion object {
        @Throws(NoSuchAlgorithmException::class)
        fun getMD5(data: String): String {
            val messageDigest = MessageDigest.getInstance("MD5")
            messageDigest.update(data.toByteArray())
            val digest = messageDigest.digest()
            return DatatypeConverter.printHexBinary(digest).toLowerCase()
        }
    }
}