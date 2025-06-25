package com.icm.trazartiza_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adaptador para mostrar una lista de direcciones IP de dispositivos Wi-Fi en un RecyclerView
class WifiDeviceAdapter(
    private val devices: List<String>,          // Lista de direcciones IP detectadas
    private val onItemClick: (String) -> Unit  // Callback cuando se toca un ítem
) : RecyclerView.Adapter<WifiDeviceAdapter.DeviceViewHolder>() {

    // ViewHolder representa cada ítem de la lista y mantiene sus vistas
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ipText: TextView = itemView.findViewById(R.id.device_ip)
    }

    // Infla el layout del ítem (item_wifi_device.xml) y crea el ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_device, parent, false)
        return DeviceViewHolder(view)
    }

    // Asocia cada dirección IP con su ViewHolder
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val ip = devices[position]
        holder.ipText.text = ip
        holder.itemView.setOnClickListener {
            onItemClick(ip)
        }
    }

    // Devuelve el número total de dispositivos en la lista
    override fun getItemCount(): Int = devices.size
}
