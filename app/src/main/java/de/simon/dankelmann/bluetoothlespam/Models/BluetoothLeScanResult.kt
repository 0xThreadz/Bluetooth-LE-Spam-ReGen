package de.simon.dankelmann.bluetoothlespam.Models

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.ParcelUuid
import androidx.core.util.forEach
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import java.time.LocalDateTime

open class BluetoothLeScanResult {
    private val logTag = "BluetoothLeScanResult"
    var deviceName = ""
    var address = ""
    var scanRecord = byteArrayOf()
    var rssi = 0
    var firstSeen = LocalDateTime.now()
    var lastSeen = LocalDateTime.now()
    private val _serviceUuids = mutableListOf<ParcelUuid>()
    private val _manufacturerSpecificData = mutableMapOf<Int, ByteArray>()
    // Service Data (keyed by Service UUID) — e.g. Fast Pair's model id lives here, not in
    // manufacturerSpecificData. Needed alongside serviceUuids/manufacturerSpecificData to
    // faithfully clone an arbitrary advertisement (see CloneScreen / DatabaseHelpers.cloneScanResultToAdvertisementSet).
    private val _serviceData = mutableMapOf<ParcelUuid, ByteArray>()

    val serviceUuids: List<ParcelUuid>
        get() = _serviceUuids

    val manufacturerSpecificData: Map<Int, ByteArray>
        get() = _manufacturerSpecificData

    val serviceData: Map<ParcelUuid, ByteArray>
        get() = _serviceData

    fun parseFromBluetoothLeScanResult(bluetoothLeScanResult: BluetoothLeScanResult) {
        deviceName = bluetoothLeScanResult.deviceName
        address = bluetoothLeScanResult.address
        scanRecord = bluetoothLeScanResult.scanRecord
        rssi = bluetoothLeScanResult.rssi
        firstSeen = bluetoothLeScanResult.firstSeen
        lastSeen = bluetoothLeScanResult.lastSeen
        _serviceUuids.clear()
        _serviceUuids.addAll(bluetoothLeScanResult.serviceUuids)
        _manufacturerSpecificData.clear()
        _manufacturerSpecificData.putAll(bluetoothLeScanResult.manufacturerSpecificData)
        _serviceData.clear()
        _serviceData.putAll(bluetoothLeScanResult.serviceData)
    }

    companion object {
        private const val _logTag = "BluetoothLeScanResult"

        fun parseFromScanResult(context: Context, scanResult: ScanResult): BluetoothLeScanResult {
            val model = BluetoothLeScanResult()

            scanResult.scanRecord?.let { record ->
                model.scanRecord = record.bytes

                record.serviceUuids?.let { uuids ->
                    model._serviceUuids.addAll(uuids)
                }

                record.manufacturerSpecificData?.let { msd ->
                    msd.forEach { manufacturerId, data ->
                        model._manufacturerSpecificData[manufacturerId] = data
                    }
                }

                record.serviceData?.let { serviceData ->
                    serviceData.forEach { (uuid, data) ->
                        model._serviceData[uuid] = data
                    }
                }
            }

            model.address = scanResult.device.address

            if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, context)) {
                scanResult.device?.name?.let { name ->
                    model.deviceName = name
                }
            }

            model.rssi = scanResult.rssi
            
            return model
        }
    }
}