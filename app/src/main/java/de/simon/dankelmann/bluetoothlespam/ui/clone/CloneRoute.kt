package de.simon.dankelmann.bluetoothlespam.ui.clone

import android.bluetooth.le.ScanResult
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Route entry point for the Clone screen: listens to the same raw BLE advertisement stream the
 * Spam Detector reads from ([IBluetoothLeScanCallback.onScanResult], fired for every nearby
 * advertisement, not just recognized spam packages), keeps a deduplicated-by-address list of
 * devices that are broadcasting an actual payload (service/manufacturer data -- e.g. the pairing
 * pop-up Bluetooth headphones send on a long button press), and lets the user save ("clone") one
 * so it can be replayed later exactly like any other custom advertisement set.
 */
@Composable
fun CloneRoute() {
    val context = LocalContext.current
    val viewModel: CloneViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val discovered = remember { mutableStateListOf<BluetoothLeScanResult>() }
    val clonedAddresses = remember { mutableStateListOf<String>() }

    val callback = remember {
        object : IBluetoothLeScanCallback {
            override fun onScanResult(scanResult: ScanResult) {
                val parsed = BluetoothLeScanResult.parseFromScanResult(context, scanResult)
                // Pure "is anyone there" adverts with no actual payload (most background chatter)
                // aren't useful to clone -- only keep devices that are broadcasting a service or
                // manufacturer-specific payload, which is what a pairing pop-up actually relies on.
                if (parsed.manufacturerSpecificData.isEmpty() && parsed.serviceData.isEmpty() && parsed.serviceUuids.isEmpty()) {
                    return
                }
                val existingIndex = discovered.indexOfFirst { it.address == parsed.address }
                if (existingIndex != -1) {
                    discovered[existingIndex] = parsed
                } else {
                    discovered.add(parsed)
                }
            }

            override fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown: Boolean) {}
            override fun onFlipperListUpdated() {}
            override fun onSpamResultPackageDetected(spamPackageScanResult: SpamPackageScanResult, alreadyKnown: Boolean) {}
            override fun onSpamResultPackageListUpdated() {}
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val scanService = (context.applicationContext as BleSpamApplication).scanService
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    scanService.addBluetoothLeScanServiceCallback(callback)
                    viewModel.isListening.value = scanService.isScanning()
                }
                Lifecycle.Event.ON_PAUSE -> scanService.removeBluetoothLeScanServiceCallback(callback)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isListening by viewModel.isListening.observeAsState(false)

    CloneScreen(
        isListening = isListening == true,
        discoveredDevices = discovered,
        clonedAddresses = clonedAddresses,
        onToggleListening = { onToggleListeningClicked(context, viewModel) },
        onCloneClicked = { scanResult ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    val title = scanResult.deviceName.ifEmpty { "Cloned ${scanResult.address}" }
                    val advertisementSet = DatabaseHelpers.cloneScanResultToAdvertisementSet(scanResult, title)
                    DatabaseHelpers.saveClonedAdvertisementSet(advertisementSet)
                }
                clonedAddresses.add(scanResult.address)
                Toast.makeText(
                    context,
                    "\"${scanResult.deviceName.ifEmpty { scanResult.address }}\" clonato — lo trovi in Cloned Devices",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )
}

private fun onToggleListeningClicked(context: Context, viewModel: CloneViewModel) {
    if (viewModel.isListening.value == true) {
        BluetoothLeScanForegroundService.stopService(context)
        viewModel.isListening.value = false
    } else {
        BluetoothLeScanForegroundService.startService(context)
        viewModel.isListening.value = true
    }
}
