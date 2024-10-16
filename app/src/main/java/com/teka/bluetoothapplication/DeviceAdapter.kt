package com.teka.bluetoothapplication

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.teka.bluetoothapplication.bluetooth_module.BtDeviceModel

class DeviceAdapter(
    private val context: Context,
    private val devices: MutableList<BtDeviceModel> = mutableListOf(),
    private val listener: DeviceListener
) : RecyclerView.Adapter<DeviceAdapter.DeviceHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    fun addDevice(device: BtDeviceModel) {
        devices.add(device)
        notifyItemInserted(devices.size - 1)
    }
    fun addDeviceList(deviceList: List<BtDeviceModel>) {
        devices.addAll(deviceList)
        notifyDataSetChanged()
    }

    // Add a device at a specific index or replace an existing one by its address
    @SuppressLint("MissingPermission")
    fun addDevice(index: Int, device: BtDeviceModel) {
        for (i in devices.indices) {
            val d = devices[i]
            if (d.address == device.address) {
                if (!d.name.isNullOrEmpty()) {
                    return  // If the existing device has a non-empty name, return
                }
                devices.removeAt(i)
                notifyItemRemoved(i)
                break
            }
        }
        devices.add(index, device)
        notifyItemInserted(index)
    }

    // Clear the device list
    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder {
        val view = inflater.inflate(R.layout.row_device, parent, false)
        return DeviceHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceHolder, position: Int) {
        val device = devices[position]
        holder.name.text = device.name ?: "Unknown Device"
        holder.address.text = device.address

        holder.itemView.setOnClickListener {
            listener.onDeviceClicked(device)
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    // ViewHolder to represent each device item
    inner class DeviceHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
        val address: TextView = itemView.findViewById(R.id.address)
    }

    // Interface for device click listener
    interface DeviceListener {
        fun onDeviceClicked(device: BtDeviceModel)
    }
}
