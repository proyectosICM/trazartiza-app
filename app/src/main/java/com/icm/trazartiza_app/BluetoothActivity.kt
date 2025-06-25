package com.icm.trazartiza_app

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BluetoothActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var deviceListAdapter: BluetoothDeviceAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val foundDevices = mutableListOf<BluetoothDevice>()
    private lateinit var progressBar: ProgressBar

    // Launcher para pedir permisos Bluetooth o localización según la versión de Android
    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startBleScan()
        } else {
            Toast.makeText(this, "Permiso requerido para escanear BLE", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para habilitar el Bluetooth si está desactivado
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            checkBluetoothPermissions()
        } else {
            Toast.makeText(this, "Bluetooth debe estar activado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bluetooth)

        // Inicializa la barra de progreso y la oculta inicialmente
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtiene el adaptador Bluetooth del dispositivo
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Verifica si el dispositivo soporta Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Si el Bluetooth está apagado, solicita al usuario que lo active
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }

        // Configura el RecyclerView
        recyclerView = findViewById(R.id.bluetoothDevicesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Verifica permisos y, si están otorgados, inicia el escaneo BLE
        checkBluetoothPermissions()
    }

    // Verifica y solicita los permisos necesarios para escanear BLE
    private fun checkBluetoothPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissionLauncher.launch(permission)
        } else {
            startBleScan()
        }
    }

    // Inicia el escaneo de dispositivos BLE
    @Suppress("MissingPermission")
    private fun startBleScan() {
        foundDevices.clear()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        Toast.makeText(this, "Buscando dispositivos BLE...", Toast.LENGTH_SHORT).show()

        progressBar.visibility = View.VISIBLE

        bluetoothLeScanner.startScan(leScanCallback)

        // Detiene el escaneo automáticamente después de 10 segundos
        recyclerView.postDelayed({
            bluetoothLeScanner.stopScan(leScanCallback)
            Toast.makeText(this, "Escaneo finalizado", Toast.LENGTH_SHORT).show()

            progressBar.visibility = View.GONE
        }, 10000)
    }

    // Callback para manejar los resultados del escaneo BLE
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!foundDevices.contains(device)) {
                foundDevices.add(device)
                deviceListAdapter = BluetoothDeviceAdapter(foundDevices) { selectedDevice ->
                    connectToBLEDevice(selectedDevice)
                }
                recyclerView.adapter = deviceListAdapter
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(this@BluetoothActivity, "Error en escaneo BLE: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    // Establece conexión con el dispositivo seleccionado
    @Suppress("MissingPermission")
    private fun connectToBLEDevice(device: BluetoothDevice) {
        Toast.makeText(this, "Conectando a ${device.name ?: "Sin nombre"}...", Toast.LENGTH_SHORT).show()

        // Guarda el dispositivo conectado en un gestor global (BleConnectionManager)
        BleConnectionManager.connectedDevice = device

        // Define la acción a realizar al conectarse correctamente (abrir nueva pantalla)
        BleConnectionManager.onConnected = {
            runOnUiThread {
                val intent = Intent(this, BleDeviceActivity::class.java)
                startActivity(intent)
            }
        }

        /// Inicia la conexión GATT con el dispositivo
        BleConnectionManager.connectedGatt = device.connectGatt(
            this,
            false,
            BleConnectionManager.gattCallback
        )
    }

    override fun onResume() {
        super.onResume()
        // Al volver a la pantalla: limpia la lista y reinicia el escaneo
        foundDevices.clear()
        recyclerView.adapter = null
        checkBluetoothPermissions()
    }

    override fun onPause() {
        super.onPause()
        // Detiene el escaneo al salir de la pantalla
        if (::bluetoothLeScanner.isInitialized) {
            bluetoothLeScanner.stopScan(leScanCallback)
        }
        progressBar.visibility = View.GONE
    }
}
