package com.punchthrough.blebondtestapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
            PackageManager.PERMISSION_GRANTED
}

fun Context.hasRequiredBluetoothPermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

fun Activity.requestBluetoothPermissions() {
    val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    }
    ActivityCompat.requestPermissions(
        this,
        requiredPermissions,
        0
    )
}

fun ActivityResultLauncher<Array<String>>.requestBluetoothPermissions() {
    val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    }
    launch(requiredPermissions)
}

private val timeZone = ZoneId.of(TimeZone.getDefault().id)
@Throws(DateTimeException::class)
fun DateTimeFormatter.dateFormatted(millisTimestamp: Long): String = format(
    Instant.ofEpochMilli(millisTimestamp).atZone(timeZone)
)

fun describeBluetoothBondStatus(status: Int) = when (status) {
    BluetoothDevice.BOND_BONDED -> "BONDED"
    BluetoothDevice.BOND_BONDING -> "BONDING"
    BluetoothDevice.BOND_NONE -> "NOT BONDED"
    else -> "Unexpected Android BluetoothDevice bond state of $status!"
}

object Util {
    fun uuidFromShortCode16(shortCode16: String): UUID? {
        if (shortCode16.length != 4) return null
        return UUID.fromString("0000${shortCode16.uppercase()}-0000-1000-8000-00805F9B34FB")
    }
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
