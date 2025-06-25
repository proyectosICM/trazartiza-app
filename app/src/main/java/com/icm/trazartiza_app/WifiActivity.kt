package com.icm.trazartiza_app

import NetworkScanner
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WifiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val foundDevices = mutableListOf<String>()
    private lateinit var adapter: WifiDeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wifi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configuración del RecyclerView y su adaptador
        recyclerView = findViewById(R.id.deviceRecyclerView)
        adapter = WifiDeviceAdapter(foundDevices) { ip ->
            Toast.makeText(this, "Dispositivo encontrado: $ip", Toast.LENGTH_SHORT).show()
            // TODO: Aquí más adelante realizar la conexiòn al ESP
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Inicializa el escáner de red local
        val scanner = NetworkScanner(this) { ip ->
            runOnUiThread {
                if (!foundDevices.contains(ip)) {
                    foundDevices.add(ip)
                    adapter.notifyItemInserted(foundDevices.size - 1)
                }
            }
        }
        scanner.startScan()
    }
}
