package com.icm.trazartiza_app

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile

// Objeto singleton que maneja la conexión BLE con un dispositivo
object BleConnectionManager {
    // Referencia al GATT conectado (canal de comunicación BLE)
    var connectedGatt: BluetoothGatt? = null

    // Dispositivo actualmente conectado
    var connectedDevice: BluetoothDevice? = null

    // Callback que se ejecuta cuando se recibe un valor leído o notificado
    var onValueRead: ((String) -> Unit)? = null

    // Callback que se ejecuta cuando se descubre correctamente el servicio BLE
    var onConnected: (() -> Unit)? = null

    // Callback que maneja los eventos de la conexión BLE
    val gattCallback = object : BluetoothGattCallback() {
        // Se llama cuando cambia el estado de la conexión
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Si se conectó correctamente, intenta descubrir los servicios del dispositivo
                gatt.discoverServices()
            }
        }

        // Se llama cuando se descubren los servicios GATT del dispositivo
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onConnected?.invoke()
            }
        }

        // Se llama cuando se realiza una lectura manual de una característica
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value?.toString(Charsets.UTF_8) ?: "Nulo"
                println("📘 Valor leído manualmente: $value")
                onValueRead?.invoke(value) // Llama al callback con el valor leído
            }
        }

        // Se llama automáticamente cuando una característica con NOTIFY cambia
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value?.toString(Charsets.UTF_8) ?: "Nulo"
            println("📡 Notificación recibida: $value")
            onValueRead?.invoke(value) // Llama al callback con el valor notificado
        }
    }
}
