package com.garfiec.util.security

import at.favre.lib.crypto.bcrypt.BCrypt

fun hashPassword(password: String): String {
    return BCrypt.withDefaults().hashToString(12, password.toCharArray())
}

fun verifyPassword(plain: String, hashed: String): Boolean {
    return BCrypt.verifyer().verify(plain.toByteArray(), hashed.toByteArray()).verified
}
