package com.example.datatransferbluetooth.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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

                Surface(color = MaterialTheme.colors.background) {
                    Column(modifier = Modifier.padding(padding)) {
                        Button(onClick = {
                            navController.navigate(
                                route = "pairedDevices"
                                ){
                                launchSingleTop=true
                            }
                        },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp,
                                disabledElevation = 0.dp
                            )
                        ) {
                            Text(text = "Enviar archivos")
                        }
                        Button(
                            onClick = {
                                navController.navigate(
                                    route = "server"
                                ){
                                    launchSingleTop=true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp,
                                disabledElevation = 0.dp
                            )
                        ){
                            Text(text = "Recibir archivos")
                        }
                    }

                }
            }
        )
    }
}

