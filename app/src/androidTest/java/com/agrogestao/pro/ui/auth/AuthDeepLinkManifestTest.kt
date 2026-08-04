package com.agrogestao.pro.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class AuthDeepLinkManifestTest {

    @Test
    fun confirmationLinkOpensMainActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("com.agrogestao.pro://auth/callback#type=signup")
        ).addCategory(Intent.CATEGORY_BROWSABLE)

        val resolved = context.packageManager.resolveActivity(intent, 0)

        assertNotNull(resolved)
        assertEquals(context.packageName, resolved?.activityInfo?.packageName)
    }
}
