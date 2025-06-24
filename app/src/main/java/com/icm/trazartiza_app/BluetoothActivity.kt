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

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startBleScan()
        } else {
            Toast.makeText(this, "Permiso requerido para escanear BLE", Toast.LENGTH_SHORT).show()
        }
    }

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }

        recyclerView = findViewById(R.id.bluetoothDevicesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        checkBluetoothPermissions()
    }

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

    @Suppress("MissingPermission")
    private fun startBleScan() {
        foundDevices.clear()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        Toast.makeText(this, "Buscando dispositivos BLE...", Toast.LENGTH_SHORT).show()
        bluetoothLeScanner.startScan(leScanCallback)

        recyclerView.postDelayed({
            bluetoothLeScanner.stopScan(leScanCallback)
            Toast.makeText(this, "Escaneo finalizado", Toast.LENGTH_SHORT).show()
        }, 10000)
    }

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

    @Suppress("MissingPermission")
    private fun connectToBLEDevice(device: BluetoothDevice) {
        Toast.makeText(this, "Conectando a ${device.name ?: "Sin nombre"}...", Toast.LENGTH_SHORT).show()

        BleConnectionManager.connectedDevice = device

        // ✅ Definir acción al descubrir servicios BLE
        BleConnectionManager.onConnected = {
            runOnUiThread {
                val intent = Intent(this, BleDeviceActivity::class.java)
                startActivity(intent)
            }
        }

        // ✅ Usar el callback centralizado
        BleConnectionManager.connectedGatt = device.connectGatt(
            this,
            false,
            BleConnectionManager.gattCallback
        )
    }
}
