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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.datatransferbluetooth.BluetoothConnectionListener
import com.example.datatransferbluetooth.BluetoothController

@Composable
fun BluetoothDeviceListScreen(
    navController: NavHostController,
    bluetoothController: BluetoothController,
    modifier: Modifier = Modifier,
    connectionListener: BluetoothConnectionListener
)
{
    val pairedDevices = bluetoothController.getPairedDevices()
    val messageState = remember {
        mutableStateOf("")
    }

    Scaffold(
        topBar = {
            CustomAppBar(navigationIcon = Icons.Filled.ArrowBack){
                navController.navigate(route = "main"){
                    popUpTo(route = "main")
                }
            }
        },
        content = {padding ->
            Surface(color= MaterialTheme.colors.background){
                Column(modifier = Modifier.padding(padding)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)){

                        items(pairedDevices) { device ->
                            Button(
                                onClick = {
                                    bluetoothController.connectToBluetoothDevice(
                                        device = device,
                                        connectionListener = connectionListener
                                    )
                                    navController.navigate(route = "client/${device.address}/${device.name}"){
                                        launchSingleTop=true
                                    }
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
}