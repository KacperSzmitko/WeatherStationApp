package com.example.soi

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BluetoothLeService() : Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null

    fun initialize(): Boolean {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        this.bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            return false
        }
        return true
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }
}