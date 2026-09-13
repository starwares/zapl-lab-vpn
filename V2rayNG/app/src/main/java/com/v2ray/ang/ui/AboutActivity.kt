package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.VersionInfoBlock
import com.v2ray.ang.util.Utils

class AboutActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        AboutScreen(
            onBackClick = { finish() },
            onTranslatorsClick = {
                startActivity(Intent(this, TranslatorsActivity::class.java))
            }
        )
    }
}

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onTranslatorsClick: () -> Unit
) {
    val context = LocalContext.current
    var openDocument by remember { mutableStateOf<LegalDocument?>(null) }

    val libVersion = CoreNativeManager.getLibVersion()
    val versionText = "v${BuildConfig.VERSION_NAME} ($libVersion)"
    // The commit ties this binary to its published source revision.
    val appIdText = "${BuildConfig.APPLICATION_ID} · ${BuildConfig.SOURCE_COMMIT}"

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_about),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_source_code_24dp),
                title = stringResource(R.string.title_source_code),
                onClick = { Utils.openUri(context, AppConfig.APP_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_about_24dp),
                title = stringResource(R.string.title_upstream_project),
                subtitle = stringResource(R.string.summary_upstream_project),
                onClick = { Utils.openUri(context, AppConfig.UPSTREAM_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.license_24px),
                title = stringResource(R.string.title_license),
                subtitle = stringResource(R.string.summary_license),
                onClick = { openDocument = LegalDocument.License }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.license_24px),
                title = stringResource(R.string.title_legal_notices),
                onClick = { openDocument = LegalDocument.Notices }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.license_24px),
                title = stringResource(R.string.title_oss_license),
                onClick = { openDocument = LegalDocument.OpenSourceLicenses }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_translate_24dp),
                title = stringResource(R.string.title_translators),
                onClick = onTranslatorsClick
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_feedback_24dp),
                title = stringResource(R.string.title_pref_feedback),
                onClick = { Utils.openUri(context, AppConfig.APP_ISSUES_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_privacy_24dp),
                title = stringResource(R.string.title_privacy_policy),
                onClick = { Utils.openUri(context, AppConfig.APP_PRIVACY_POLICY) }
            )
            VersionInfoBlock(
                versionText = versionText,
                appIdText = appIdText
            )
            NavigationBarsSpacer()
        }
    }

    openDocument?.let { document ->
        AlertDialog(
            onDismissRequest = { openDocument = null },
            title = { Text(stringResource(document.titleRes)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            loadUrl(document.assetUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { openDocument = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}

/**
 * Legal texts bundled in the APK. The `legal/` pages are generated at build time from the
 * repository's LICENSE, NOTICE and THIRD_PARTY_NOTICES.md, so the binary carries the same notices
 * as its source revision.
 */
private enum class LegalDocument(@StringRes val titleRes: Int, val assetUrl: String) {
    License(R.string.title_license, "file:///android_asset/legal/license.html"),
    Notices(R.string.title_legal_notices, "file:///android_asset/legal/notices.html"),
    OpenSourceLicenses(R.string.title_oss_license, "file:///android_asset/open_source_licenses.html")
}
