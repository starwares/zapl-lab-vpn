package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.colorCard
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed

/**
 * Connection summary shown above the profile list: service status (tap to test the connection),
 * the start/stop button and the selected profile.
 */
@Composable
internal fun MainConnectionCard(
    status: MainStatus,
    statusText: String,
    isRunning: Boolean,
    selectedServer: ServerRowUiModel?,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = colorCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectionStatusRow(
                statusText = statusText,
                statusColor = statusColor(status),
                onTestConnection = { onAction(MainAction.TestCurrentServer) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PowerButton(
                isRunning = isRunning,
                onToggle = { onAction(MainAction.ToggleService) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SelectedServerSummary(selectedServer = selectedServer)
        }
    }
}

@Composable
private fun ConnectionStatusRow(
    statusText: String,
    statusColor: Color,
    onTestConnection: () -> Unit
) {
    // One focus target: the dot is decorative and the text is merged into the clickable row.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.action_check_connection),
                onClick = onTestConnection
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PowerButton(
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "PowerButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
        label = "PowerButtonContent"
    )
    Surface(
        onClick = onToggle,
        modifier = Modifier.size(76.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_power_24dp),
                contentDescription = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun SelectedServerSummary(selectedServer: ServerRowUiModel?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = selectedServer?.remarks ?: stringResource(R.string.main_no_server_selected),
            style = MaterialTheme.typography.titleMedium,
            color = if (selectedServer != null) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (selectedServer != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedServer.typeDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (selectedServer.testDelayMillis != 0L) {
                    Text(
                        text = " · " + stringResource(R.string.server_test_delay_value, selectedServer.testDelayMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedServer.testDelayMillis < 0L) colorPingRed else colorPing,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: MainStatus): Color = when (status) {
    MainStatus.Connected -> colorPing
    MainStatus.Disconnected -> MaterialTheme.colorScheme.outline
    MainStatus.Testing, is MainStatus.TestProgress -> MaterialTheme.colorScheme.secondary
    is MainStatus.ConnectionTest -> if (status.result.delayMillis >= 0) colorPing else colorPingRed
}
