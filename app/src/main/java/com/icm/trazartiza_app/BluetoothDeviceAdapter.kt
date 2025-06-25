package com.icm.trazartiza_app

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onItemClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a los TextViews del layout item_bluetooth_device.xml
        private val deviceName: TextView = itemView.findViewById(R.id.device_name)
        private val deviceAddress: TextView = itemView.findViewById(R.id.device_address)

        // Asocia los datos del dispositivo con los elementos de la vista
        fun bind(device: BluetoothDevice) {
            deviceName.text = device.name ?: "Sin nombre"
            deviceAddress.text = device.address

            // Llama a la función cuando se hace clic en el ítem
            itemView.setOnClickListener {
                onItemClick(device)
            }
        }
    }

    // Infla el layout XML para cada ítem de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return DeviceViewHolder(view)
    }

    // Asocia cada dispositivo con su ViewHolder
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    // Devuelve el número total de elementos en la lista
    override fun getItemCount(): Int = devices.size
}
