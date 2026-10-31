package de.simon.dankelmann.bluetoothlespam.ui.clone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance

@Composable
fun CloneScreen(
    isListening: Boolean,
    discoveredDevices: List<BluetoothLeScanResult>,
    clonedAddresses: List<String>,
    onToggleListening: () -> Unit,
    onCloneClicked: (BluetoothLeScanResult) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + SpecterTopAppBarClearance,
            bottom = 16.dp + FloatingNavBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeaderCard(isListening = isListening, onToggleListening = onToggleListening)
        }

        item {
            Text(
                text = "Devices nearby (${discoveredDevices.size})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (discoveredDevices.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isListening) {
                            "In ascolto… tieni premuto il pulsante sulle cuffie (o avvicina il dispositivo) per far comparire il suo pacchetto qui."
                        } else {
                            "Premi play e avvicina il dispositivo da clonare (es. tieni premuto il pulsante delle cuffie per far apparire il popup di pairing)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        } else {
            items(discoveredDevices.sortedByDescending { it.rssi }, key = { it.address }) { device ->
                DiscoveredDeviceRow(
                    device = device,
                    alreadyCloned = device.address in clonedAddresses,
                    onCloneClicked = { onCloneClicked(device) },
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(isListening: Boolean, onToggleListening: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Clone Device", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Ascolta gli annunci BLE nelle vicinanze e clona quello di un dispositivo per riprodurlo in seguito.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FilledIconButton(onClick = onToggleListening) {
                Icon(
                    imageVector = if (isListening) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                )
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(
    device: BluetoothLeScanResult,
    alreadyCloned: Boolean,
    onCloneClicked: () -> Unit,
) {
    val payloadSummary = buildString {
        if (device.manufacturerSpecificData.isNotEmpty()) {
            val entry = device.manufacturerSpecificData.entries.first()
            append("Mfg 0x${entry.key.toString(16).uppercase()} · ${entry.value.size}B")
        }
        if (device.serviceData.isNotEmpty()) {
            if (isNotEmpty()) append(" · ")
            val entry = device.serviceData.entries.first()
            append("Svc ${entry.key} · ${entry.value.size}B")
        } else if (device.serviceUuids.isNotEmpty()) {
            if (isNotEmpty()) append(" · ")
            append("Svc ${device.serviceUuids.first()}")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName.ifEmpty { device.address },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(text = device.address, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${device.rssi} dBm" + if (payloadSummary.isNotEmpty()) " · $payloadSummary" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (alreadyCloned) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Already cloned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                FilledTonalIconButton(onClick = onCloneClicked) {
                    Icon(
                        painter = painterResource(R.drawable.repeat),
                        contentDescription = "Clone this device",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
