package com.runtracker.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.runtracker.shared.data.model.SmartTrainerDevice
import com.runtracker.shared.data.model.SmartTrainerStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartTrainerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // FTMS (Fitness Machine Service) UUIDs
        val FTMS_SERVICE_UUID: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
        val INDOOR_BIKE_DATA_UUID: UUID = UUID.fromString("00002ad2-0000-1000-8000-00805f9b34fb")
        val FITNESS_MACHINE_CONTROL_POINT_UUID: UUID = UUID.fromString("00002ad9-0000-1000-8000-00805f9b34fb")
        val FITNESS_MACHINE_STATUS_UUID: UUID = UUID.fromString("00002ada-0000-1000-8000-00805f9b34fb")
        val FITNESS_MACHINE_FEATURE_UUID: UUID = UUID.fromString("00002acc-0000-1000-8000-00805f9b34fb")
        
        // Heart Rate Service UUIDs
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        
        // Client Characteristic Configuration Descriptor
        val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val statusMutex = Mutex()

    private val _discoveredDevices = MutableStateFlow<List<SmartTrainerDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<SmartTrainerDevice>> = _discoveredDevices.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _trainerStatus = MutableStateFlow(SmartTrainerStatus())
    val trainerStatus: StateFlow<SmartTrainerStatus> = _trainerStatus.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private var connectedDevice: SmartTrainerDevice? = null
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCOVERING_SERVICES,
        READY
    }
    
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!hasBluetoothPermissions() || !isBluetoothEnabled()) return
        if (_isScanning.value) return
        
        _discoveredDevices.value = emptyList()
        _isScanning.value = true
        
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(FTMS_SERVICE_UUID))
            .build()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        
        // Also scan without filter to catch devices that don't advertise FTMS
        bluetoothLeScanner?.startScan(scanCallbackNoFilter)
    }
    
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!hasBluetoothPermissions()) return
        
        bluetoothLeScanner?.stopScan(scanCallback)
        bluetoothLeScanner?.stopScan(scanCallbackNoFilter)
        _isScanning.value = false
    }
    
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }
        
        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { processScanResult(it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }
    
    @SuppressLint("MissingPermission")
    private val scanCallbackNoFilter = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Only process if device name suggests it's a trainer
            val name = result.device.name?.lowercase() ?: return
            if (name.contains("jetblack") || name.contains("trainer") || 
                name.contains("kickr") || name.contains("tacx") || 
                name.contains("wahoo") || name.contains("elite") ||
                name.contains("zwift") || name.contains("bike")) {
                processScanResult(result)
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val name = device.name ?: return
        val address = device.address
        
        // Check if already in list
        if (_discoveredDevices.value.any { it.address == address }) return
        
        // Determine brand from name
        val brand = when {
            name.lowercase().contains("jetblack") -> "Jetblack"
            name.lowercase().contains("kickr") -> "Wahoo"
            name.lowercase().contains("wahoo") -> "Wahoo"
            name.lowercase().contains("tacx") -> "Tacx"
            name.lowercase().contains("elite") -> "Elite"
            name.lowercase().contains("zwift") -> "Zwift"
            else -> "Unknown"
        }
        
        val trainerDevice = SmartTrainerDevice(
            id = address,
            name = name,
            brand = brand,
            model = null,
            address = address,
            supportsFTMS = result.scanRecord?.serviceUuids?.any { it.uuid == FTMS_SERVICE_UUID } == true
        )
        
        _discoveredDevices.value = _discoveredDevices.value + trainerDevice
    }
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: SmartTrainerDevice) {
        if (!hasBluetoothPermissions()) return
        
        stopScanning()
        _connectionState.value = ConnectionState.CONNECTING
        connectedDevice = device
        
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return
        bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)
    }
    
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectedDevice = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _trainerStatus.value = SmartTrainerStatus()
    }
    
    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.DISCOVERING_SERVICES
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _trainerStatus.value = SmartTrainerStatus()
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.CONNECTED
                enableNotifications(gatt)
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                INDOOR_BIKE_DATA_UUID -> parseIndoorBikeData(value)
                HEART_RATE_MEASUREMENT_UUID -> parseHeartRateData(value)
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: return
            when (characteristic.uuid) {
                INDOOR_BIKE_DATA_UUID -> parseIndoorBikeData(value)
                HEART_RATE_MEASUREMENT_UUID -> parseHeartRateData(value)
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt) {
        // Enable Indoor Bike Data notifications
        val ftmsService = gatt.getService(FTMS_SERVICE_UUID)
        ftmsService?.getCharacteristic(INDOOR_BIKE_DATA_UUID)?.let { characteristic ->
            gatt.setCharacteristicNotification(characteristic, true)
            characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let { descriptor ->
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
        
        // Enable Heart Rate notifications if available
        val hrService = gatt.getService(HEART_RATE_SERVICE_UUID)
        hrService?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)?.let { characteristic ->
            gatt.setCharacteristicNotification(characteristic, true)
            characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let { descriptor ->
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
        
        _connectionState.value = ConnectionState.READY
    }
    
    private fun parseIndoorBikeData(data: ByteArray) {
        if (data.size < 2) return
        
        val flags = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
        var offset = 2
        
        var speed = 0.0
        var cadence = 0
        var power = 0
        var heartRate: Int? = null
        
        // Parse based on flags
        // Bit 0: More Data (not used here)
        // Bit 1: Average Speed Present
        // Bit 2: Instantaneous Cadence Present
        // Bit 3: Average Cadence Present
        // Bit 4: Total Distance Present
        // Bit 5: Resistance Level Present
        // Bit 6: Instantaneous Power Present
        // Bit 7: Average Power Present
        // Bit 8: Expended Energy Present
        // Bit 9: Heart Rate Present
        
        // Instantaneous Speed (always present if bit 0 is 0)
        if ((flags and 0x01) == 0 && offset + 2 <= data.size) {
            speed = ((data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)) / 100.0
            offset += 2
        }
        
        // Average Speed
        if ((flags and 0x02) != 0 && offset + 2 <= data.size) {
            offset += 2 // Skip average speed
        }
        
        // Instantaneous Cadence
        if ((flags and 0x04) != 0 && offset + 2 <= data.size) {
            cadence = ((data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)) / 2
            offset += 2
        }
        
        // Average Cadence
        if ((flags and 0x08) != 0 && offset + 2 <= data.size) {
            offset += 2 // Skip average cadence
        }
        
        // Total Distance
        if ((flags and 0x10) != 0 && offset + 3 <= data.size) {
            offset += 3 // Skip total distance
        }
        
        // Resistance Level
        if ((flags and 0x20) != 0 && offset + 2 <= data.size) {
            offset += 2 // Skip resistance level
        }
        
        // Instantaneous Power
        if ((flags and 0x40) != 0 && offset + 2 <= data.size) {
            power = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
        }
        
        // Average Power
        if ((flags and 0x80) != 0 && offset + 2 <= data.size) {
            offset += 2 // Skip average power
        }
        
        // Expended Energy
        if ((flags and 0x100) != 0 && offset + 5 <= data.size) {
            offset += 5 // Skip expended energy
        }
        
        // Heart Rate
        if ((flags and 0x200) != 0 && offset + 1 <= data.size) {
            heartRate = data[offset].toInt() and 0xFF
        }
        
        serviceScope.launch {
            statusMutex.withLock {
                _trainerStatus.value = _trainerStatus.value.copy(
                    isConnected = true,
                    currentPowerWatts = power,
                    currentCadenceRpm = cadence,
                    currentSpeedKmh = speed,
                    currentHeartRate = heartRate ?: _trainerStatus.value.currentHeartRate
                )
            }
        }
    }

    private fun parseHeartRateData(data: ByteArray) {
        if (data.isEmpty()) return

        val flags = data[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0

        val heartRate = if (is16Bit && data.size >= 3) {
            (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        } else if (data.size >= 2) {
            data[1].toInt() and 0xFF
        } else {
            return
        }

        serviceScope.launch {
            statusMutex.withLock {
                _trainerStatus.value = _trainerStatus.value.copy(
                    currentHeartRate = heartRate
                )
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun setResistanceLevel(level: Int) {
        val gatt = bluetoothGatt ?: return
        val ftmsService = gatt.getService(FTMS_SERVICE_UUID) ?: return
        val controlPoint = ftmsService.getCharacteristic(FITNESS_MACHINE_CONTROL_POINT_UUID) ?: return
        
        // FTMS Control Point: Set Target Resistance Level (0x04)
        val command = byteArrayOf(0x04, (level and 0xFF).toByte())
        controlPoint.value = command
        gatt.writeCharacteristic(controlPoint)
        
        _trainerStatus.value = _trainerStatus.value.copy(currentResistanceLevel = level)
    }
    
    fun getConnectedDevice(): SmartTrainerDevice? = connectedDevice
}
