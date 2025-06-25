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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class BleDeviceActivity : AppCompatActivity() {
    // Referencias al GATT y dispositivo conectado
    private var gatt: BluetoothGatt? = null
    private var device: BluetoothDevice? = null
    private lateinit var valueTextView: TextView

    // UUID del servicio y característica que se quiere usar
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

        // Inicializa la vista donde se muestra el valor recibido
        valueTextView = findViewById(R.id.device_value)

        // Obtiene las referencias del dispositivo y conexión activa desde el ConnectionManager
        gatt = BleConnectionManager.connectedGatt
        device = BleConnectionManager.connectedDevice

        // Si no hay conexión activa, muestra mensaje y cierra la actividad
        if (gatt == null || device == null) {
            Toast.makeText(this, "No hay dispositivo conectado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Toast.makeText(this, "Conectado a ${device?.name}", Toast.LENGTH_SHORT).show()

        // Callback para mostrar en pantalla el valor recibido del BLE
        BleConnectionManager.onValueRead = { value ->
            runOnUiThread {
                valueTextView.text = "Valor recibido: $value"
            }
        }

        // Habilita la recepción de notificaciones del BLE
        enableNotification()

        // Referencias a los campos de texto y botón
        val inputMin = findViewById<TextInputEditText>(R.id.input_min)
        val inputMax = findViewById<TextInputEditText>(R.id.input_max)
        val btnEnviar = findViewById<MaterialButton>(R.id.btn_enviar)

        // Acción del botón "Enviar": valida e intenta mandar los valores por BLE
        btnEnviar.setOnClickListener {
            val minText = inputMin.text.toString().trim()
            val maxText = inputMax.text.toString().trim()

            if (minText.isEmpty() || maxText.isEmpty()) {
                Toast.makeText(this, "Ingresa ambos valores", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val message = "$minText,$maxText"
            sendMessageOverBle(message)

            inputMin.setText("")
            inputMax.setText("")
        }
    }

    // Habilita notificaciones BLE para la característica definida
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

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            Toast.makeText(this, "Característica no tiene NOTIFY", Toast.LENGTH_SHORT).show()
            println("⚠️ Característica no tiene propiedad NOTIFY")
            return
        }

        val notificationSet = gatt?.setCharacteristicNotification(characteristic, true) ?: false
        if (!notificationSet) {
            Toast.makeText(this, "No se pudo activar notificaciones", Toast.LENGTH_SHORT).show()
            return
        }

        // Habilita notificaciones a través del descriptor estándar
        val descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        )
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val result = gatt?.writeDescriptor(descriptor) ?: false

        println("✅ Notificaciones habilitadas: $result")
    }

    // Envía un mensaje en forma de texto por BLE a la característica indicada
    private fun sendMessageOverBle(message: String) {
        val service = gatt?.getService(targetServiceUUID)
        val characteristic = service?.getCharacteristic(targetCharacteristicUUID)

        if (characteristic == null) {
            Toast.makeText(this, "No se pudo obtener la característica", Toast.LENGTH_SHORT).show()
            return
        }

        // Verifica que la característica sea escribible
        val props = characteristic.properties
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE == 0 &&
            props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == 0
        ) {
            Toast.makeText(this, "Característica no es escribible", Toast.LENGTH_SHORT).show()
            return
        }

        // Escribe el mensaje como bytes UTF-8
        characteristic.value = message.toByteArray(Charsets.UTF_8)
        val success = gatt?.writeCharacteristic(characteristic) ?: false

        if (!success) {
            Toast.makeText(this, "Error al escribir en la característica", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Mensaje enviado: $message", Toast.LENGTH_SHORT).show()
        }
    }

    // Al presionar atrás, pregunta al usuario si desea desconectar el BLE
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

    // Limpia el callback al destruir la actividad
    override fun onDestroy() {
        super.onDestroy()
        BleConnectionManager.onValueRead = null
    }
}
