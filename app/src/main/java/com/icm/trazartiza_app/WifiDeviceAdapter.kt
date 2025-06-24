package com.icm.trazartiza_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WifiDeviceAdapter(
    private val devices: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<WifiDeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ipText: TextView = itemView.findViewById(R.id.device_ip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val ip = devices[position]
        holder.ipText.text = ip
        holder.itemView.setOnClickListener {
            onItemClick(ip)
        }
    }

    override fun getItemCount(): Int = devices.size
}
