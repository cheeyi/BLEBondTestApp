package com.punchthrough.blebondtestapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.snapshots.SnapshotStateList

class BluetoothBroadcastReceiver(val logEntries: SnapshotStateList<LogEntry>) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = with(intent) {
        if (
            action == BluetoothDevice.ACTION_BOND_STATE_CHANGED &&
            hasExtra(BluetoothDevice.EXTRA_DEVICE)
        ) {
            val device = getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                ?: return@with
            val previousBondState = getIntExtra(
                BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1
            )
            val bondState = getIntExtra(
                BluetoothDevice.EXTRA_BOND_STATE, -1
            )

            val bondTransition =
                "${describeBluetoothBondStatus(previousBondState)} to ${describeBluetoothBondStatus(bondState)}"
            logEntries.log("${device.address} bond state changed | $bondTransition", LogLevel.WARN)
        } else if (action == BluetoothAdapter.ACTION_STATE_CHANGED &&
            hasExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE) &&
            hasExtra(BluetoothAdapter.EXTRA_STATE)
        ) {
            val previousState = getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1
            )
            val newState = getIntExtra(
                BluetoothAdapter.EXTRA_STATE, -1
            )

            val stateTransition =
                "${describeBluetoothStatus(previousState)} to ${describeBluetoothStatus(newState)}"
            logEntries.log("Android Bluetooth state changed | $stateTransition", LogLevel.WARN)

            if (newState == BluetoothAdapter.STATE_OFF) {
                logEntries.log("Bluetooth was disabled!", LogLevel.ERROR)
            }
        }
    }

    private fun describeBluetoothBondStatus(status: Int) = when (status) {
        BluetoothDevice.BOND_BONDED -> "BONDED"
        BluetoothDevice.BOND_BONDING -> "BONDING"
        BluetoothDevice.BOND_NONE -> "NOT BONDED"
        else -> "Unexpected Android BluetoothDevice bond state of $status!"
    }

    private fun describeBluetoothStatus(status: Int) = when (status) {
        BluetoothAdapter.STATE_OFF -> "OFF"
        BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
        BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
        BluetoothAdapter.STATE_ON -> "ON"
        else -> "Unexpected Android BluetoothAdapter state of $status!"
    }
}
