package com.runtracker.app.health

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeartRateZoneManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _connectionState = MutableStateFlow(HRConnectionState.DISCONNECTED)
    val connectionState: StateFlow<HRConnectionState> = _connectionState.asStateFlow()

    private val _currentHeartRate = MutableStateFlow<Int?>(null)
    val currentHeartRate: StateFlow<Int?> = _currentHeartRate.asStateFlow()

    private val _currentZone = MutableStateFlow<HeartRateZone?>(null)
    val currentZone: StateFlow<HeartRateZone?> = _currentZone.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var userMaxHeartRate: Int = 190 // Default, should be calculated from age

    fun setUserAge(age: Int) {
        userMaxHeartRate = 220 - age
    }

    fun calculateZone(heartRate: Int): HeartRateZone {
        val percentage = (heartRate.toFloat() / userMaxHeartRate) * 100
        return when {
            percentage < 60 -> HeartRateZone.ZONE_1_RECOVERY
            percentage < 70 -> HeartRateZone.ZONE_2_AEROBIC
            percentage < 80 -> HeartRateZone.ZONE_3_TEMPO
            percentage < 90 -> HeartRateZone.ZONE_4_THRESHOLD
            else -> HeartRateZone.ZONE_5_MAX
        }
    }

    fun getZoneRange(zone: HeartRateZone): Pair<Int, Int> {
        return when (zone) {
            HeartRateZone.ZONE_1_RECOVERY -> Pair((userMaxHeartRate * 0.5).toInt(), (userMaxHeartRate * 0.6).toInt())
            HeartRateZone.ZONE_2_AEROBIC -> Pair((userMaxHeartRate * 0.6).toInt(), (userMaxHeartRate * 0.7).toInt())
            HeartRateZone.ZONE_3_TEMPO -> Pair((userMaxHeartRate * 0.7).toInt(), (userMaxHeartRate * 0.8).toInt())
            HeartRateZone.ZONE_4_THRESHOLD -> Pair((userMaxHeartRate * 0.8).toInt(), (userMaxHeartRate * 0.9).toInt())
            HeartRateZone.ZONE_5_MAX -> Pair((userMaxHeartRate * 0.9).toInt(), userMaxHeartRate)
        }
    }

    fun startScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            _connectionState.value = HRConnectionState.BLUETOOTH_OFF
            return
        }

        _connectionState.value = HRConnectionState.SCANNING
        _discoveredDevices.value = emptyList()

        val scanner = bluetoothAdapter.bluetoothLeScanner
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        } catch (e: SecurityException) {
            _connectionState.value = HRConnectionState.PERMISSION_DENIED
        }
    }

    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            // Ignore
        }
        if (_connectionState.value == HRConnectionState.SCANNING) {
            _connectionState.value = HRConnectionState.DISCONNECTED
        }
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = HRConnectionState.CONNECTING

        try {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            _connectionState.value = HRConnectionState.PERMISSION_DENIED
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            // Ignore
        }
        bluetoothGatt = null
        _connectionState.value = HRConnectionState.DISCONNECTED
        _currentHeartRate.value = null
        _currentZone.value = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val currentList = _discoveredDevices.value.toMutableList()
                if (!currentList.any { it.address == device.address }) {
                    currentList.add(device)
                    _discoveredDevices.value = currentList
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = HRConnectionState.SCAN_FAILED
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = HRConnectionState.CONNECTED
                    try {
                        gatt?.discoverServices()
                    } catch (e: SecurityException) {
                        // Ignore
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = HRConnectionState.DISCONNECTED
                    _currentHeartRate.value = null
                    _currentZone.value = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val hrService = gatt?.getService(HEART_RATE_SERVICE_UUID)
                val hrCharacteristic = hrService?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)

                hrCharacteristic?.let { characteristic ->
                    try {
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                    } catch (e: SecurityException) {
                        // Ignore
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let {
                if (it.uuid == HEART_RATE_MEASUREMENT_UUID) {
                    val flag = it.properties
                    val format = if (flag and 0x01 != 0) {
                        BluetoothGattCharacteristic.FORMAT_UINT16
                    } else {
                        BluetoothGattCharacteristic.FORMAT_UINT8
                    }
                    val heartRate = it.getIntValue(format, 1) ?: return

                    _currentHeartRate.value = heartRate
                    _currentZone.value = calculateZone(heartRate)
                }
            }
        }
    }
}

enum class HRConnectionState {
    DISCONNECTED,
    BLUETOOTH_OFF,
    PERMISSION_DENIED,
    SCANNING,
    SCAN_FAILED,
    CONNECTING,
    CONNECTED
}

enum class HeartRateZone(
    val displayName: String,
    val description: String,
    val colorHex: Long
) {
    ZONE_1_RECOVERY("Zone 1 - Recovery", "Light activity, warm-up", 0xFF90CAF9),
    ZONE_2_AEROBIC("Zone 2 - Aerobic", "Fat burning, endurance", 0xFF81C784),
    ZONE_3_TEMPO("Zone 3 - Tempo", "Aerobic endurance", 0xFFFFEB3B),
    ZONE_4_THRESHOLD("Zone 4 - Threshold", "Lactate threshold", 0xFFFF9800),
    ZONE_5_MAX("Zone 5 - Maximum", "Maximum effort", 0xFFF44336)
}
