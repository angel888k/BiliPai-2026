package com.android.purebilibili.core.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.store.ThemeModeRoleOverrides
import com.android.purebilibili.core.store.ThemeRoleOverrides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeRoleOverridePolicyTest {

    @Test
    fun enabledOverrides_replaceMaterialRolesAndChooseReadableButtonText() {
        val overrides = ThemeRoleOverrides(
            enabled = true,
            light = ThemeModeRoleOverrides("#F4F0E8", "#201A17", "#655D57", "#FFF000"),
            dark = ThemeRoleOverrides().dark
        )

        val result = applyThemeRoleOverrides(lightColorScheme(), overrides, darkTheme = false)

        assertEquals(Color(0xFFF4F0E8), result.background)
        assertEquals(Color(0xFF201A17), result.onBackground)
        assertEquals(Color(0xFF655D57), result.onSurfaceVariant)
        assertEquals(Color(0xFFFFF000), result.primary)
        assertEquals(Color.Black, result.onPrimary)
    }

    @Test
    fun contrastWarning_allowsGoodContrastAndFlagsWeakText() {
        assertFalse(
            hasThemeRoleContrastWarning(
                ThemeModeRoleOverrides("#FFFFFF", "#111111", "#555555", "#0061A4")
            )
        )
        assertTrue(
            hasThemeRoleContrastWarning(
                ThemeModeRoleOverrides("#FFFFFF", "#EEEEEE", "#F2F2F2", "#0061A4")
            )
        )
    }
}
