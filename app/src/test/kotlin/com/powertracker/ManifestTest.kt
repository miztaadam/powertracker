package com.powertracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Guards what the merged manifest promises: an offline app called Grit with the Grit launcher icon. */
@RunWith(RobolectricTestRunner::class)
class ManifestTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun noInternetPermission() {
        val permissions = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()

        // The list is really the manifest's: a permission the app does ask for is in it.
        assertTrue(permissions.contains(Manifest.permission.VIBRATE))
        assertFalse(permissions.contains(Manifest.permission.INTERNET))
    }

    @Test
    fun launcherLabelIsGrit() {
        assertEquals("Grit", context.applicationInfo.loadLabel(context.packageManager).toString())
        assertEquals("Grit", context.getString(R.string.app_name))
    }

    @Test
    fun launcherIconsAreTheAdaptiveIcon() {
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(R.mipmap.ic_launcher, info.icon)
        assertEquals(R.mipmap.ic_launcher_round, info.roundIconResource())
    }

    private fun android.content.pm.ApplicationInfo.roundIconResource(): Int =
        android.content.pm.ApplicationInfo::class.java.getField("roundIconRes").getInt(this)
}
