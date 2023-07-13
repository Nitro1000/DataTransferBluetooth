package com.example.datatransferbluetooth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.datatransferbluetooth.BluetoothConnectionListener
import com.example.datatransferbluetooth.BluetoothController
import com.example.datatransferbluetooth.Models.BluetoothDeviceModel

@Composable
fun BluetoothDeviceListScreen(
    navController: NavHostController,
    bluetoothController: BluetoothController,
    modifier: Modifier = Modifier,
    connectionListener: BluetoothConnectionListener
) {
    val pairedDevices = bluetoothController.getPairedDevices()
    var selectedDevice: BluetoothDeviceModel? by remember { mutableStateOf(null) }
    var connectionSuccessful by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CustomAppBar(
                title = "Selecciona un dispositivo",
                navigationIcon = Icons.Filled.ArrowBack
            ) {
                navController.navigate(route = "main") {
                    popUpTo(route = "main")
                }
            }
        },
        content = { padding ->
            Surface(color = MaterialTheme.colors.background, modifier = modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(padding)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pairedDevices) { device ->
                            Button(
                                onClick = {
                                    selectedDevice = device
                                    bluetoothController.connectToBluetoothDevice(
                                        device = device,
                                        connectionListener = object : BluetoothConnectionListener {
                                            override fun onConnectionSuccess() {
                                                connectionSuccessful = true
                                                connectionListener.onConnectionSuccess()
                                            }

                                            override fun onConnectionError() {
                                                connectionSuccessful = false
                                                connectionListener.onConnectionError()
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = device.name ?: "Desconocido",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(connectionSuccessful) {
        if (connectionSuccessful && selectedDevice != null) {
            navController.navigate(route = "client/${selectedDevice!!.address}/${selectedDevice!!.name}") {
                launchSingleTop = true
            }
        }
    }
}
