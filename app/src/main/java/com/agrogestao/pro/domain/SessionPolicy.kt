package com.agrogestao.pro.domain

fun canUseLocalProfile(existingEmail: String?, requestedEmail: String): Boolean {
    val current = existingEmail?.trim().orEmpty()
    return current.isBlank() || current.equals(requestedEmail.trim(), ignoreCase = true)
}

fun tokenExpiryEpochSeconds(nowEpochSeconds: Long, expiresInSeconds: Long): Long =
    nowEpochSeconds + expiresInSeconds.coerceAtLeast(0)

fun shouldRefreshToken(
    expiresAtEpochSeconds: Long,
    nowEpochSeconds: Long,
    safetyWindowSeconds: Long = 60
): Boolean = expiresAtEpochSeconds <= nowEpochSeconds + safetyWindowSeconds
