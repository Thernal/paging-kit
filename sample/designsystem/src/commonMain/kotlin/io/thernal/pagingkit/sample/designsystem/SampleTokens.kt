package io.thernal.pagingkit.sample.designsystem

import androidx.compose.runtime.Immutable

/**
 * The tokens the sample's design system adds to Material 3, installed with [SampleTheme]. Colours,
 * shapes and type are Material's; paging-kit only asks for motion.
 */
@Immutable
data class SampleTokens(
    val motion: SampleMotion = SampleMotion(),
)
