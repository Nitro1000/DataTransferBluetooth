package com.example.datatransferbluetooth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.datatransferbluetooth.BluetoothController
import com.example.datatransferbluetooth.ui.theme.DataTransferBluetoothTheme

@Composable
fun MainScreen(navController: NavHostController, bluetoothController: BluetoothController) {
    DataTransferBluetoothTheme {
        Scaffold(
            topBar = {
                CustomAppBar(title = "Selecciona una opción")
            },
            content = { padding ->
                Surface(color = Color(0xFFBCD7E5)) {
                    Column(
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CustomButton("Enviar archivos") {
                            bluetoothController.updatePairedDevices()
                            navController.navigate(
                                route = "pairedDevices"
                            ){
                                launchSingleTop=true
                            }
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                        CustomButton("Recibir archivos") {
                            navController.navigate(
                                route = "server"
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        )
    }
}


