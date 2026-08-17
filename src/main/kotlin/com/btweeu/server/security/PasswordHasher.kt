package com.btweeu.server.security

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {

    fun hash(plainPassword: String): String =
        BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray())

    fun verify(plainPassword: String, hashed: String): Boolean =
        BCrypt.verifyer().verify(plainPassword.toCharArray(), hashed).verified
}
