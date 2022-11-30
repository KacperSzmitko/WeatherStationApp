package com.example.soi
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.soi.databinding.ActivityPairBinding

class PairActivity: UseBluetooth()  {
    private lateinit var binding: ActivityPairBinding
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var leDeviceListAdapter: ArrayAdapter<BluetoothDevice>
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private var devices : ArrayList<BluetoothDevice> = ArrayList()
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 3000

    private fun scanLeDevice() {
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.device !in devices) {
                devices.add(result.device)
                leDeviceListAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding = ActivityPairBinding.inflate(layoutInflater)
        this.btStatus = binding.btStatus
        val view = binding.root
        setContentView(view)
        this.bluetoothManager = getSystemService(BluetoothManager::class.java)
        this.bluetoothAdapter = bluetoothManager.adapter
        this.bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        this.manageBluetooth()
        this.scanLeDevice()
        this.binding.toMainView.setOnClickListener{startActivity(Intent(this, MainActivity::class.java))}
        this.binding.refresh.setOnClickListener{scanLeDevice()}
        this.leDeviceListAdapter = object: ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, devices){
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val item = getItem(position)
                view.findViewById<TextView>(android.R.id.text1).text = item?.name ?: item!!.address
                return view
            }
        }
        this.binding.selectDeviceList.adapter = leDeviceListAdapter
        this.binding.selectDeviceList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = devices[position]
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("device", device)
            bluetoothLeScanner.stopScan(leScanCallback)
            scanning = false
            startActivity(intent)
       }
    }

}