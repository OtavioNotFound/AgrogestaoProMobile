package com.agrogestao.pro.data.sync

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val cloudTimestampRegex = Regex(
    "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?(Z|[+-]\\d{2}:?\\d{2})$"
)

fun shouldApplyRemoteChange(localTimestamp: Long, remoteTimestamp: Long): Boolean =
    remoteTimestamp > 0 && remoteTimestamp >= localTimestamp

fun nextLocalTimestamp(previous: Long, now: Long = System.currentTimeMillis()): Long =
    maxOf(now, previous + 1)

fun formatCloudTimestamp(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }.format(Date(epochMillis))

fun parseCloudTimestamp(value: String): Long? {
    val match = cloudTimestampRegex.matchEntire(value) ?: return null
    val fraction = match.groupValues[2].padEnd(3, '0').take(3)
    val normalized = match.groupValues[1] + "." + fraction + match.groupValues[3]
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(normalized)?.time
    }.getOrNull()
}
