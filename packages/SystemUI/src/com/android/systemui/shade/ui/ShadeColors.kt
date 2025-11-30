/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.shade.ui

import android.content.Context
import android.graphics.Color
import android.os.UserHandle
import android.provider.Settings
import com.android.internal.graphics.ColorUtils
import com.android.systemui.res.R

object ShadeColors {
    /**
     * Calculate notification shade panel color.
     *
     * @param context Context to resolve colors.
     * @param blurSupported Whether blur is enabled (can be off due to battery saver)
     * @param withScrim Whether to composite a scrim when blur is enabled (used by legacy shade).
     * @return color for the shade panel.
     */
    @JvmStatic
    fun shadePanel(context: Context, blurSupported: Boolean, withScrim: Boolean): Int {
        return if (blurSupported) {
            if (withScrim) {
                ColorUtils.compositeColors(
                    shadePanelStandard(context),
                    shadePanelScrimBehind(context),
                )
            } else {
                shadePanelStandard(context)
            }
        } else {
            shadePanelFallback(context)
        }
    }

    @JvmStatic
    fun notificationScrim(context: Context, blurSupported: Boolean): Int {
        return if (blurSupported) {
            notificationScrimStandard(context)
        } else {
            notificationScrimFallback(context)
        }
    }

    @JvmStatic
    fun shadePanelScrimBehind(context: Context): Int {
        return context.resources.getColor(
            com.android.internal.R.color.shade_panel_scrim,
            context.theme,
        )
    }

    /**
     * Background color of the rounded cornered rect behind Notifications in Single- or SplitShade.
     */
    @JvmStatic
    fun classicShadeNotificationScrimBg(
        context: Context,
        blurSupported: Boolean,
        composited: Boolean,
    ): Int {
        val surfaceEffect0Color =
            context.resources.getColor(
                com.android.internal.R.color.customColorSurfaceEffect0,
                context.theme,
            )
        if (!composited) {
            // SplitShade (avoid adding the shade panel with scrim)
            return surfaceEffect0Color
        }
        // SingleShade
        val shadePanelWithScrim = shadePanel(context, blurSupported, withScrim = true)
        return ColorUtils.compositeColors(surfaceEffect0Color, shadePanelWithScrim)
    }

    @JvmStatic
    private fun shadePanelStandard(context: Context): Int {
        val layerAbove =
            context.resources.getColor(com.android.internal.R.color.shade_panel_fg, context.theme)
        val layerBelow =
            context.resources.getColor(com.android.internal.R.color.shade_panel_bg, context.theme)
        return ColorUtils.compositeColors(layerAbove, layerBelow)
    }

    private fun isSingleQsToneEnabled(context: Context): Boolean {
        return Settings.System.getIntForUser(
            context.contentResolver,
            Settings.System.SINGLE_QS_TONE_ENABLED,
            0,
            UserHandle.USER_CURRENT,
        ) == 1
    }

    @JvmStatic
    private fun shadePanelFallback(context: Context): Int {
        return if (isSingleQsToneEnabled(context)) {
            context.getColor(R.color.single_tone_shade_panel_fallback)
        } else {
            context.getColor(R.color.shade_panel_fallback)
        }
    }

    @JvmStatic
    private fun notificationScrimStandard(context: Context): Int {
        if (isSingleQsToneEnabled(context)) {
            return Color.TRANSPARENT
        }
        return ColorUtils.setAlphaComponent(
            context.getColor(R.color.notification_scrim_base),
            (0.5f * 255).toInt(),
        )
    }

    @JvmStatic
    private fun notificationScrimFallback(context: Context): Int {
        return if (isSingleQsToneEnabled(context)) {
            context.getColor(R.color.single_tone_shade_panel_fallback)
        } else {
            context.getColor(R.color.notification_scrim_fallback)
        }
    }

    @JvmStatic
    fun opaqueNotificationBackground(context: Context): Int {
        val container = context.getColor(com.android.internal.R.color.materialColorSurfaceContainer)
        // Pure Black uses a black container, which disappears against a black shade.
        return if (container == Color.BLACK) {
            context.getColor(com.android.internal.R.color.materialColorSurfaceContainerHigh)
        } else {
            container
        }
    }
}
