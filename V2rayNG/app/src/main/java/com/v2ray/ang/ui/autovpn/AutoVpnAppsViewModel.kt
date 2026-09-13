package com.v2ray.ang.ui.autovpn

import android.app.Application
import android.content.Context
import android.content.Intent
import com.v2ray.ang.AppConfig
import com.v2ray.ang.autoVpn.AppVpnPolicy
import com.v2ray.ang.autoVpn.AutoVpnPolicyRepository
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator

/** State and operations of the per-app policy list. */
class AutoVpnAppsViewModel(application: Application) : BaseViewModel(application) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _policies = MutableStateFlow<Map<String, AppVpnPolicy>>(emptyMap())
    val policies: StateFlow<Map<String, AppVpnPolicy>> = _policies.asStateFlow()

    private val _filter = MutableStateFlow<AppVpnPolicy?>(null)
    val filter: StateFlow<AppVpnPolicy?> = _filter.asStateFlow()

    private var allApps: List<AppInfo>? = null
    private var query = ""
    private var isLoadingApps = false

    fun load(context: Context) {
        AutoVpnPolicyRepository.refresh()
        _policies.value = AutoVpnPolicyRepository.all()
        if (allApps != null || isLoadingApps) return

        val applicationContext = context.applicationContext
        isLoadingApps = true
        launchLoading {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val launchable = launchablePackages(applicationContext)
                    val configured = _policies.value.keys
                    sortApps(
                        AppManagerUtil.loadNetworkAppList(applicationContext).filter { info ->
                            info.packageName != applicationContext.packageName &&
                                (info.packageName in launchable || info.packageName in configured)
                        }
                    )
                }
                allApps = loaded
                applyFilters()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "AutoVpn: failed to load the app list", e)
            } finally {
                isLoadingApps = false
            }
        }
    }

    fun search(value: String) {
        query = value
        applyFilters()
    }

    fun setFilter(policy: AppVpnPolicy?) {
        _filter.value = policy
        applyFilters()
    }

    fun policyOf(packageName: String): AppVpnPolicy =
        _policies.value[packageName] ?: AppVpnPolicy.DEFAULT

    fun setPolicy(packageName: String, policy: AppVpnPolicy) {
        AutoVpnPolicyRepository.set(packageName, policy)
        _policies.value = AutoVpnPolicyRepository.all()
        // The engine keeps the table in memory inside the daemon, so it has to be told.
        MessageHelper.sendMsg2Service(app, AppConfig.MSG_AUTO_VPN_RELOAD, "")
        applyFilters()
    }

    /**
     * Only an app with a launcher entry can become the app the user works with; resource
     * overlays and background components would otherwise flood the list. Packages that already
     * carry a policy stay visible so it can always be changed back.
     */
    private fun launchablePackages(context: Context): Set<String> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(launcher, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    private fun applyFilters() {
        val source = allApps ?: return
        val selected = _filter.value
        val text = query
        _apps.value = source.filter { info ->
            val matchesPolicy = selected == null || policyOf(info.packageName) == selected
            val matchesQuery = text.isEmpty() ||
                info.appName.contains(text, ignoreCase = true) ||
                info.packageName.contains(text, ignoreCase = true)
            matchesPolicy && matchesQuery
        }
    }

    private fun sortApps(apps: List<AppInfo>): List<AppInfo> {
        val collator = Collator.getInstance()
        val configured = _policies.value
        return apps.sortedWith { first, second ->
            val firstConfigured = first.packageName in configured
            val secondConfigured = second.packageName in configured
            when {
                firstConfigured && !secondConfigured -> -1
                !firstConfigured && secondConfigured -> 1
                first.isSystemApp && !second.isSystemApp -> 1
                !first.isSystemApp && second.isSystemApp -> -1
                else -> collator.compare(first.appName, second.appName)
            }
        }
    }
}
