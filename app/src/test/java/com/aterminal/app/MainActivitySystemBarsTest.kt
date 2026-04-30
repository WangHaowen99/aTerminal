package com.aterminal.app

import android.graphics.Color
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM],
    application = AterminalApplication::class,
)
class MainActivitySystemBarsTest {
    @Test
    fun usesTransparentSystemBarsForEdgeToEdgeLayout() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
        assertEquals(Color.TRANSPARENT, activity.window.navigationBarColor)
        assertEquals(Color.TRANSPARENT, activity.window.navigationBarDividerColor)
        assertFalse(activity.window.isNavigationBarContrastEnforced)
    }
}
