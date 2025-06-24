package com.icm.trazartiza_app

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile

object BleConnectionManager {
    var connectedGatt: BluetoothGatt? = null
    var connectedDevice: BluetoothDevice? = null

    var onValueRead: ((String) -> Unit)? = null
    var onConnected: (() -> Unit)? = null

    val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onConnected?.invoke()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value?.toString(Charsets.UTF_8) ?: "Nulo"
                println("📘 Valor leído manualmente: $value")
                onValueRead?.invoke(value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value?.toString(Charsets.UTF_8) ?: "Nulo"
            println("📡 Notificación recibida: $value")
            onValueRead?.invoke(value)
        }
    }
}
