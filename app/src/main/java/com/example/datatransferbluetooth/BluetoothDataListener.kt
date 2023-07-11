package com.example.datatransferbluetooth

interface BluetoothDataListener {
    fun onDataReceived(data: String)
}