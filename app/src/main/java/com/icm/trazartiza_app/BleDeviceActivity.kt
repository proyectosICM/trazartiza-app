package com.icm.trazartiza_app

import android.bluetooth.*
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class BleDeviceActivity : AppCompatActivity() {

    private var gatt: BluetoothGatt? = null
    private var device: BluetoothDevice? = null
    private lateinit var valueTextView: TextView

    private val targetServiceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val targetCharacteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ble_device)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        valueTextView = findViewById(R.id.device_value)

        gatt = BleConnectionManager.connectedGatt
        device = BleConnectionManager.connectedDevice

        if (gatt == null || device == null) {
            Toast.makeText(this, "No hay dispositivo conectado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Toast.makeText(this, "Conectado a ${device?.name}", Toast.LENGTH_SHORT).show()

        // ✅ Callback para mostrar valor leído
        BleConnectionManager.onValueRead = { value ->
            runOnUiThread {
                valueTextView.text = "Valor recibido: $value"
            }
        }

        // 📡 Habilitar notificaciones
        enableNotification()
    }

    private fun enableNotification() {
        val service = gatt?.getService(targetServiceUUID)
        if (service == null) {
            Toast.makeText(this, "Servicio no encontrado", Toast.LENGTH_SHORT).show()
            println("❌ Servicio $targetServiceUUID no encontrado")
            return
        }

        val characteristic = service.getCharacteristic(targetCharacteristicUUID)
        if (characteristic == null) {
            Toast.makeText(this, "Característica no encontrada", Toast.LENGTH_SHORT).show()
            println("❌ Característica $targetCharacteristicUUID no encontrada")
            return
        }

        // Verificar que tiene la propiedad NOTIFY
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            Toast.makeText(this, "Característica no tiene NOTIFY", Toast.LENGTH_SHORT).show()
            println("⚠️ Característica no tiene propiedad NOTIFY")
            return
        }

        // Habilitar notificaciones locales
        val notificationSet = gatt?.setCharacteristicNotification(characteristic, true) ?: false
        if (!notificationSet) {
            Toast.makeText(this, "No se pudo activar notificaciones", Toast.LENGTH_SHORT).show()
            return
        }

        // Escribir descriptor para activar notificaciones remotas
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val result = gatt?.writeDescriptor(descriptor) ?: false

        println("✅ Notificaciones habilitadas: $result")
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("¿Cerrar conexión?")
            .setMessage("¿Estás seguro de que quieres retroceder?\nEsto cerrará la conexión con el dispositivo BLE.")
            .setPositiveButton("Sí") { _, _ ->
                gatt?.disconnect()
                gatt?.close()
                BleConnectionManager.connectedGatt = null
                BleConnectionManager.connectedDevice = null
                BleConnectionManager.onValueRead = null
                super.onBackPressed()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        BleConnectionManager.onValueRead = null
    }
}
