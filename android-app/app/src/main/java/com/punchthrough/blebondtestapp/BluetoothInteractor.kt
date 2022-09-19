package com.punchthrough.blebondtestapp

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import timber.log.Timber

@SuppressLint("MissingPermission")
class BluetoothInteractor(val context: Context, val logEntries: SnapshotStateList<LogEntry>) {
    enum class TestStage {
        IDLE,
        SCANNING,
        CONNECTING,
        INITIATE_BOND,
        INTENTIONAL_DISCONNECT,
        FIRST_RECONNECT_TO_BONDED_PERIPHERAL,
        FIRST_RECONNECT_SUCCEEDED,
        INTENTIONAL_DISCONNECT_AND_UNBOND
    }

    val currentTestStage = mutableStateOf(TestStage.IDLE)
    val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager
        bluetoothManager?.adapter
    }

    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner
    private var connectedGatt: BluetoothGatt? = null
    private var bondedMacAddress: String? = null
    private val peripheralBondState: Int
        get() = connectedGatt?.device?.bondState ?: Int.MIN_VALUE

    fun startTesting() {
        if (!ensureTestStageIs(TestStage.IDLE)) {
            return
        }
        transitionTestStageTo(TestStage.SCANNING)
        scanner?.startScan(
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(
                        ParcelUuid.fromString(Util.uuidFromShortCode16("180D").toString())
                    )
                    .build(),
                ScanFilter.Builder()
                    .setDeviceName("Android13FTW")
                    .build()
            ),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            scanCallback
        )
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (currentTestStage.value != TestStage.SCANNING) return
            scanner?.stopScan(this)
            transitionTestStageTo(TestStage.CONNECTING)
            logEntries.log("Found peripheral, connecting to ${result.device.name}", LogLevel.DEBUG)
            connectedGatt = result.device.connectGatt(
                context.applicationContext,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.w("onConnectionStateChange $status $newState")
            when (currentTestStage.value) {
                TestStage.CONNECTING, TestStage.FIRST_RECONNECT_TO_BONDED_PERIPHERAL -> {
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                        connectedGatt = gatt
                        logEntries.log("Connected and discovering services", LogLevel.DEBUG)
                        Handler(Looper.getMainLooper()).post {
                            gatt.discoverServices()
                        }
                    }
                }
                TestStage.INTENTIONAL_DISCONNECT -> {
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                        connectedGatt?.close()
                        connectedGatt = null
                        logEntries.log("Disconnected from peripheral as expected", LogLevel.ERROR)
                        transitionTestStageTo(TestStage.FIRST_RECONNECT_TO_BONDED_PERIPHERAL)
                        val peripheralToReconnect = bluetoothAdapter?.bondedDevices?.firstOrNull {
                            it.address == bondedMacAddress
                        } ?: run {
                            logEntries.log("Unable to find bonded device!", LogLevel.ERROR)
                            resetState()
                            return
                        }
                        logEntries.log(
                            "Reconnecting to bonded peripheral $bondedMacAddress"
                            , LogLevel.DEBUG
                        )
                        peripheralToReconnect.connectGatt(context.applicationContext, false, this)
                    }
                }
                else -> {
                    logEntries.log(
                        "Unexpected onConnectionStateChange status $status, newState: $newState" +
                                ", current test stage: ${currentTestStage.value}",
                        LogLevel.ERROR
                    )
                    resetState()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (currentTestStage.value == TestStage.CONNECTING) {
                logEntries.log(
                    "Services discovered, attempting to read Body Sensor Location characteristic" +
                            ", which should trigger a bonding security request"
                    , LogLevel.DEBUG
                )
                transitionTestStageTo(TestStage.INITIATE_BOND)
            } else if (currentTestStage.value == TestStage.FIRST_RECONNECT_TO_BONDED_PERIPHERAL) {
                logEntries.log(
                    "Services discovered for reconnected bonded peripheral"
                    , LogLevel.DEBUG
                )
                transitionTestStageTo(TestStage.FIRST_RECONNECT_SUCCEEDED)
            }
            connectedGatt?.getService(Util.uuidFromShortCode16("180D"))
                ?.getCharacteristic(Util.uuidFromShortCode16("2A38"))
                ?.apply {
                    connectedGatt?.readCharacteristic(this)
                }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val bondState = describeBluetoothBondStatus(peripheralBondState)
            logEntries.log(
                "onCharacteristicRead status $status, bond state: $bondState, " +
                        "value: ${characteristic.value.toHexString()}",
                LogLevel.DEBUG
            )
            if (status == BluetoothGatt.GATT_SUCCESS && peripheralBondState == BluetoothDevice.BOND_BONDED) {
                logEntries.log("Bonded and read was successful", LogLevel.WARN)
                bondedMacAddress = connectedGatt?.device?.address
                when (currentTestStage.value) {
                    TestStage.INITIATE_BOND -> {
                        transitionTestStageTo(TestStage.INTENTIONAL_DISCONNECT)
                        logEntries.log(
                            "Waiting for firmware-initiated disconnect, " +
                                    "please press Button 1 on the nRF52840",
                            LogLevel.WARN
                        )
                    }
                    TestStage.FIRST_RECONNECT_SUCCEEDED -> {
                        transitionTestStageTo(TestStage.INTENTIONAL_DISCONNECT_AND_UNBOND)
                        connectedGatt?.close()
                        connectedGatt?.device?.let { device ->
                            try {
                                val method = device::class.java.getMethod("removeBond").invoke(device)
                                logEntries.log("removeBond called via Reflection", LogLevel.DEBUG)
                            } catch (e: Throwable) {
                                Timber.e(e, "Failed to remove bond")
                            }
                        }
                        logEntries.log(
                            "This iteration of test is complete, please run another iteration",
                            LogLevel.WARN
                        )
                        transitionTestStageTo(TestStage.IDLE)
                    }
                    else -> {
                        // Trigger an error state to reset everything
                        ensureTestStageIs(TestStage.INITIATE_BOND)
                    }
                }

            }
        }
    }

    /**
     * Returns false if the current test stage is not equal to [testStage].
     */
    private fun ensureTestStageIs(testStage: TestStage): Boolean {
        if (currentTestStage.value != testStage) {
            logEntries.log("Unexpected test stage $testStage", LogLevel.ERROR)
            logEntries.log("Please restart the app", LogLevel.DEBUG)
            resetState()
        }
        return currentTestStage.value == testStage
    }

    private fun transitionTestStageTo(testStage: TestStage) {
        logEntries.log("Transitioning test stage to $testStage", LogLevel.DEBUG)
        currentTestStage.value = testStage
    }

    private fun resetState() {
        transitionTestStageTo(TestStage.IDLE)
        connectedGatt = null
        bondedMacAddress = null
    }
}
