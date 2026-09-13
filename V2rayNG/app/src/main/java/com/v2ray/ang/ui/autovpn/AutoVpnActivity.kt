package com.v2ray.ang.ui.autovpn

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.autoVpn.AppVpnPolicy
import com.v2ray.ang.autoVpn.AutoVpnPolicyEngine
import com.v2ray.ang.service.AutoVpnMonitorService
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.PreferenceGroupHeader
import com.v2ray.ang.ui.compose.SettingsListItem
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.SettingsSwitchItem

/** Settings screen of the automatic, active-app driven VPN control. */
class AutoVpnActivity : BaseComponentActivity() {

    private val viewModel: AutoVpnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        // Usage access is granted on a system screen, so the state can only be re-read here.
        // Once it is granted, the monitor that refused to start earlier can come up.
        viewModel.refresh()
        AutoVpnMonitorService.syncWithSettings(this)
    }

    @Composable
    override fun ScreenContent() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        AutoVpnScreen(
            state = state,
            onBackClick = { finish() },
            onEnabledChange = viewModel::setEnabled,
            onKeepWhileScreenOffChange = viewModel::setKeepWhileScreenOff,
            onIdleTimeoutChange = { viewModel.setIdleTimeout(it) },
            onGrantAccessClick = { openUsageAccessSettings() },
            onPolicyClick = { policy -> startActivity(AutoVpnAppsActivity.intent(this, policy)) },
        )
    }

    private fun openUsageAccessSettings() {
        // Some OEM builds ship without the usage-access screen; fall back to app settings so the
        // user is not left on a dead button.
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            return
        }
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", packageName, null))
        )
    }
}

@Composable
fun AutoVpnScreen(
    state: AutoVpnViewModel.UiState,
    onBackClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onKeepWhileScreenOffChange: (Boolean) -> Unit,
    onIdleTimeoutChange: (Long) -> Unit,
    onGrantAccessClick: () -> Unit,
    onPolicyClick: (AppVpnPolicy) -> Unit,
) {
    val scrollState = rememberScrollState()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = stringResource(R.string.auto_vpn_title), onBackClick = onBackClick) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            if (!state.hasUsageAccess) {
                UsageAccessCard(onGrantAccessClick)
                AppDivider()
            }
            if (!state.supportedRunMode) {
                Text(
                    text = stringResource(R.string.auto_vpn_requires_vpn_mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            SettingsSwitchItem(
                title = stringResource(R.string.auto_vpn_switch),
                summary = stringResource(R.string.auto_vpn_switch_summary),
                checked = state.enabled,
                onCheckedChange = onEnabledChange,
                enabled = state.hasUsageAccess
            )

            PreferenceGroupHeader(title = stringResource(R.string.auto_vpn_apps_title))
            AppVpnPolicy.entries.forEach { policy ->
                // The default policy is never stored, so a count would always read 0.
                val subtitle = if (policy == AppVpnPolicy.DEFAULT) {
                    stringResource(R.string.auto_vpn_policy_direct_summary)
                } else {
                    stringResource(R.string.auto_vpn_apps_count, state.counts[policy] ?: 0)
                }
                SettingsMenuItem(
                    title = stringResource(policy.labelRes),
                    subtitle = subtitle,
                    onClick = { onPolicyClick(policy) }
                )
            }

            PreferenceGroupHeader(title = stringResource(R.string.auto_vpn_behavior))
            SettingsListItem(
                title = stringResource(R.string.auto_vpn_idle_timeout),
                entries = AutoVpnViewModel.IDLE_TIMEOUT_OPTIONS_MS.map { timeoutLabel(it) },
                values = AutoVpnViewModel.IDLE_TIMEOUT_OPTIONS_MS.map { it.toString() },
                selectedValue = state.idleTimeoutMs.toString(),
                onSelected = { onIdleTimeoutChange(it.toLongOrNull() ?: AutoVpnPolicyEngine.DEFAULT_IDLE_TIMEOUT_MS) }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.auto_vpn_keep_screen_off),
                summary = stringResource(R.string.auto_vpn_keep_screen_off_summary),
                checked = state.keepWhileScreenOff,
                onCheckedChange = onKeepWhileScreenOffChange
            )

            PreferenceGroupHeader(title = stringResource(R.string.auto_vpn_status))
            StatusBlock(state)
            NavigationBarsSpacer()
        }
    }
}

@Composable
private fun timeoutLabel(valueMs: Long): String =
    if (valueMs == 0L) stringResource(R.string.auto_vpn_immediately)
    else stringResource(R.string.auto_vpn_seconds, (valueMs / 1000).toInt())

@Composable
private fun UsageAccessCard(onGrantAccessClick: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.auto_vpn_permission_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.auto_vpn_permission_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onGrantAccessClick, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.auto_vpn_permission_action))
        }
    }
}

@Composable
private fun StatusBlock(state: AutoVpnViewModel.UiState) {
    val unknown = stringResource(R.string.auto_vpn_status_none)
    val vpnState = when (state.status.vpnState) {
        AutoVpnPolicyEngine.VpnState.ON -> stringResource(R.string.auto_vpn_state_on)
        AutoVpnPolicyEngine.VpnState.STARTING -> stringResource(R.string.auto_vpn_state_starting)
        AutoVpnPolicyEngine.VpnState.STOPPING -> stringResource(R.string.auto_vpn_state_stopping)
        AutoVpnPolicyEngine.VpnState.ERROR -> stringResource(R.string.auto_vpn_state_error)
        AutoVpnPolicyEngine.VpnState.OFF -> stringResource(R.string.auto_vpn_state_off)
    }
    val appLabel = state.statusAppLabel ?: state.status.primaryPackage ?: unknown
    val mode = state.status.policy?.let { stringResource(it.labelRes) } ?: unknown
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.auto_vpn_status_vpn, vpnState),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.auto_vpn_status_app, appLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.auto_vpn_status_mode, mode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
