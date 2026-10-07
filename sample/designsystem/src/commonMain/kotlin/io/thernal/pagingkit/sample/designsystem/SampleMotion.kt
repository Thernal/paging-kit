package io.thernal.pagingkit.sample.designsystem

import androidx.compose.runtime.Immutable

/** Durations, in milliseconds: a quick fade and a larger change of content. */
@Immutable
data class SampleMotion(
    val fastMillis: Int = FAST_MILLIS,
    val mediumMillis: Int = MEDIUM_MILLIS,
)

private const val FAST_MILLIS = 150
private const val MEDIUM_MILLIS = 300
