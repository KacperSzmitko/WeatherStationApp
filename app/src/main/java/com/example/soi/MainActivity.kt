package com.example.soi

import android.bluetooth.*
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import com.example.soi.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID


class MainActivity : UseBluetooth() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var device: BluetoothDevice
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var dataAdapter: DataAdapter
    private var data : ArrayList<DataClass> = ArrayList()
    private var tmp_subscribed = false
    private var press_subscribed = false
    private lateinit var service: BluetoothGattService

    companion object {
        val service_uuid : UUID = UUID.fromString("2c2a0001-7212-443e-8b37-c0ab864d7c26")
        val tmp_uuid : UUID = UUID.fromString("2c2a0002-7212-443e-8b37-c0ab864d7c26")
        val pressure_uuid : UUID = UUID.fromString("2c2a0003-7212-443e-8b37-c0ab864d7c26")
        val cccd_uuid : UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding = ActivityMainBinding.inflate(layoutInflater)
        this.btStatus = binding.btStatus
        val view = binding.root
        setContentView(view)
        this.bluetoothManager = getSystemService(BluetoothManager::class.java)
        this.bluetoothAdapter = bluetoothManager.adapter
        this.binding.toPairView.setOnClickListener{startActivity(Intent(this, com.example.soi.PairActivity::class.java))}
        this.manageBluetooth()
        val d = intent?.extras
        if (d != null && d.containsKey("device")){
            device = d.getParcelable<BluetoothDevice>("device")!!
            with(device) {
                Log.w("ScanResultAdapter", "Connecting to $address")
                connectGatt(this@MainActivity, false, callback)
            }
            data.add(DataClass("Temperatura", 0f))
            data.add(DataClass("Ciśnienie", 0f))
        }
        dataAdapter = DataAdapter(this, data)
        binding.list.adapter = dataAdapter
    }

    override fun onDestroy() {
        bluetoothGatt.close()
        super.onDestroy()
    }


    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(this@MainActivity, "Pomyślnie połączono z GATT", Toast.LENGTH_LONG).show()}
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt.discoverServices()
                    }
            } else { Handler(Looper.getMainLooper()).post{
                    Toast.makeText(this@MainActivity, "GATT success ale nie connected", Toast.LENGTH_LONG).show()} }
            } else { Handler(Looper.getMainLooper()).post{
                Toast.makeText(this@MainActivity, "Nie udało się nawiązać połączenia z GATT", Toast.LENGTH_LONG).show()
            } }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                printGattTable()
                service = getService(service_uuid)
                val tmpChar = service.getCharacteristic(tmp_uuid)
                enableNotifications(tmpChar)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            with(characteristic!!) {
                if (uuid == tmp_uuid){
                    Handler(Looper.getMainLooper()).post {
                        data[0].value = parseValue(value).toFloat() / 10
                        dataAdapter.notifyDataSetChanged()
                    }
                }
                else{
                    Handler(Looper.getMainLooper()).post {
                        data[1].value = parseValue(value).toFloat() / 10
                        dataAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                if (!tmp_subscribed){
                    tmp_subscribed = true
                    val pressChar = service.getCharacteristic(pressure_uuid)
                    enableNotifications(pressChar)
                }
                else if(!press_subscribed){
                    press_subscribed = true
                }
            }
        }

    }

    fun parseValue(v: ByteArray): Int{
        val b = ByteBuffer.wrap(v)
        b.order(ByteOrder.LITTLE_ENDIAN)
        return b.getInt(0)
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        }
    }

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val payload = when {
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(cccd_uuid)?.let { cccDescriptor ->
            if (bluetoothGatt.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        characteristic.getDescriptor(cccd_uuid)?.let { cccDescriptor ->
            if (bluetoothGatt.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }


}