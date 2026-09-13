package com.v2ray.ang.ui.autovpn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.v2ray.ang.R
import com.v2ray.ang.autoVpn.AppVpnPolicy
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.ItemDivider
import com.v2ray.ang.ui.compose.NavigationBarsBottomPadding
import com.v2ray.ang.ui.compose.SelectListDialog
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.AppIconFetcher

/** Per-app policy list with search and the three policy filters. */
class AutoVpnAppsActivity : BaseComponentActivity() {

    private val viewModel: AutoVpnAppsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(EXTRA_POLICY)
            ?.let { name -> AppVpnPolicy.entries.firstOrNull { it.name == name } }
            ?.let { viewModel.setFilter(it) }
        viewModel.load(this)
    }

    @Composable
    override fun ScreenContent() {
        val apps by viewModel.apps.collectAsStateWithLifecycle()
        val policies by viewModel.policies.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

        AutoVpnAppsScreen(
            apps = apps,
            policyOf = { policies[it] ?: AppVpnPolicy.DEFAULT },
            filter = filter,
            isLoading = isLoading,
            onBackClick = { finish() },
            onSearch = viewModel::search,
            onFilterChange = viewModel::setFilter,
            onPolicySelected = viewModel::setPolicy,
        )
    }

    companion object {
        private const val EXTRA_POLICY = "auto_vpn_policy"

        fun intent(context: Context, policy: AppVpnPolicy?): Intent =
            Intent(context, AutoVpnAppsActivity::class.java)
                .putExtra(EXTRA_POLICY, policy?.name)
    }
}

@Composable
fun AutoVpnAppsScreen(
    apps: List<AppInfo>,
    policyOf: (String) -> AppVpnPolicy,
    filter: AppVpnPolicy?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onSearch: (String) -> Unit,
    onFilterChange: (AppVpnPolicy?) -> Unit,
    onPolicySelected: (String, AppVpnPolicy) -> Unit,
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val chipScrollState = rememberScrollState()

    LaunchedEffect(Unit) { onSearch(searchQuery) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.auto_vpn_apps_title),
                onBackClick = onBackClick,
                isLoading = isLoading,
                isSearchActive = showSearch,
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    onSearch(query)
                },
                onSearchClose = {
                    searchQuery = ""
                    onSearch("")
                    showSearch = false
                },
                searchPlaceholder = stringResource(R.string.menu_item_search),
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painterResource(R.drawable.ic_search_24dp),
                                contentDescription = stringResource(R.string.acc_search)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text(stringResource(R.string.auto_vpn_filter_all)) }
                )
                AppVpnPolicy.entries.forEach { policy ->
                    val label = if (policy == AppVpnPolicy.VPN_FORBIDDEN) {
                        stringResource(R.string.auto_vpn_filter_forbidden)
                    } else {
                        stringResource(policy.labelRes)
                    }
                    FilterChip(
                        selected = filter == policy,
                        onClick = { onFilterChange(policy) },
                        label = { Text(label) }
                    )
                }
            }

            if (apps.isEmpty() && !isLoading) {
                Text(
                    text = stringResource(R.string.auto_vpn_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState),
                contentPadding = NavigationBarsBottomPadding()
            ) {
                items(items = apps, key = { it.packageName }) { app ->
                    AutoVpnAppRow(
                        app = app,
                        policy = policyOf(app.packageName),
                        onClick = { selectedPackage = app.packageName }
                    )
                    ItemDivider()
                }
            }
        }
    }

    val editing = selectedPackage
    if (editing != null) {
        val currentPolicy = policyOf(editing)
        SelectListDialog(
            title = stringResource(R.string.auto_vpn_policy_question),
            options = AppVpnPolicy.entries.toList(),
            optionText = { stringResource(it.labelRes) },
            selectedOption = currentPolicy,
            showRadio = true,
            onSelected = { policy ->
                onPolicySelected(editing, policy)
                selectedPackage = null
            },
            onDismiss = { selectedPackage = null }
        )
    }
}

@Composable
private fun AutoVpnAppRow(app: AppInfo, policy: AppVpnPolicy, onClick: () -> Unit) {
    val context = LocalContext.current
    val policyLabel = stringResource(policy.labelRes)
    val rowDescription = "${app.appName}, $policyLabel"
    val model = remember(app.packageName) {
        ImageRequest.Builder(context)
            .data("appicon:${app.packageName}")
            .fetcherFactory(AppIconFetcher.Factory(context))
            .build()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = rowDescription
                onClick { onClick(); true }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = policyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
