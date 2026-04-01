package com.example.bluetoothchess.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothchess.R
import com.example.bluetoothchess.databinding.FragmentDeviceListBinding
import com.example.bluetoothchess.databinding.ItemDeviceBinding

class DeviceListFragment : Fragment() {

    private var _binding: FragmentDeviceListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()
    private val devices = mutableListOf<BluetoothDevice>()
    private lateinit var adapter: DeviceAdapter
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && !devices.contains(device) && device.name != null) {
                        devices.add(device)
                        adapter.notifyItemInserted(devices.size - 1)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btManager = requireContext().getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = btManager?.adapter

        adapter = DeviceAdapter { device ->
            bluetoothAdapter?.cancelDiscovery()
            viewModel.joinGame(device)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GameFragment())
                .commit()
        }

        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewDevices.adapter = adapter

        binding.btnStopScan.setOnClickListener {
            bluetoothAdapter?.cancelDiscovery()
            binding.progressScan.visibility = View.GONE
        }

        // Add paired devices first
        bluetoothAdapter?.bondedDevices?.let { paired ->
            devices.addAll(paired)
            adapter.notifyDataSetChanged()
        }

        startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        binding.progressScan.visibility = View.VISIBLE
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireContext().registerReceiver(receiver, filter)
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(receiver)
        bluetoothAdapter?.cancelDiscovery()
        _binding = null
    }

    inner class DeviceAdapter(private val onClick: (BluetoothDevice) -> Unit) : 
        RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("MissingPermission")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.binding.textDeviceName.text = device.name ?: "Unknown Device"
            holder.binding.textDeviceAddress.text = device.address
            holder.itemView.setOnClickListener { onClick(device) }
        }

        override fun getItemCount() = devices.size
    }
}
