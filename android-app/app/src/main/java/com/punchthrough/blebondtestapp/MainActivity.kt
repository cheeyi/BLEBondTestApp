package com.punchthrough.blebondtestapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.punchthrough.blebondtestapp.ui.theme.BLEBondTestAppTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val permissionsGranted = mutableStateOf(false)
    private val logEntries = mutableStateListOf<LogEntry>()
    private val bluetoothInteractor = BluetoothInteractor(this, logEntries)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val deniedPermissions = result.filter { !it.value }.map { it.key }
        permissionsGranted.value = result.all { it.value }

        logEntries.log("Permissions granted: ${permissionsGranted.value}", LogLevel.WARN)
        when {
            deniedPermissions.isNotEmpty() -> {
                // One or more required permissions is denied, unable to proceed
                logEntries.log("Denied permission list: $deniedPermissions", LogLevel.ERROR)
                logEntries.log("Please grant all required permissions", LogLevel.ERROR)
                Toast.makeText(
                    this,
                    "Please grant $deniedPermissions",
                    Toast.LENGTH_LONG
                )
                try {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    if (!isFinishing) {
                        Toast.makeText(
                            this,
                            "Failed to launch App Settings, please grant permissions manually",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(BluetoothBroadcastReceiver(logEntries), intentFilter)
        logEntries.log(
            "Testing on ${Build.MANUFACTURER} ${Build.MODEL} running Android ${Build.VERSION.RELEASE}.",
            LogLevel.DEBUG
        )
        if (bluetoothInteractor.bluetoothAdapter?.isEnabled == true) {
            logEntries.log("Bluetooth is enabled", LogLevel.DEBUG)
        } else {
            logEntries.log("Bluetooth is disabled or not available!", LogLevel.ERROR)
        }
        requestPermissionLauncher.requestBluetoothPermissions()

        setContent {
            BLEBondTestAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Text(
                            text = getString(R.string.app_description),
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = {
                                bluetoothInteractor.startTesting()
                            },
                            enabled = permissionsGranted.value &&
                                    bluetoothInteractor.currentTestStage.value ==
                                    BluetoothInteractor.TestStage.IDLE
                        ) {
                            Text(text = "Run Test")
                        }
                        LogTable(logEntries = logEntries)
                    }
                }
            }
        }
    }
}
