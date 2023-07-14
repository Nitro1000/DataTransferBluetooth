package com.example.datatransferbluetooth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
                        Button(onClick = {
                            bluetoothController.updatePairedDevices()
                            navController.navigate(
                                route = "pairedDevices"
                            ){
                                launchSingleTop=true
                            }
                        },
                            // Define the desired width of the Button, space and rounded corners
                            modifier = Modifier.padding(horizontal = 10.dp).width(280.dp),
                            shape = MaterialTheme.shapes.large.copy(CornerSize(50)),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF1A06F9),
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
                        Spacer(modifier = Modifier.height(16.dp)) // To give some space between the buttons
                        Button(
                            onClick = {
                                navController.navigate(
                                    route = "server"
                                ){
                                    launchSingleTop=true
                                }
                            },
                            modifier = Modifier.padding(horizontal = 10.dp).width(280.dp),
                            shape = MaterialTheme.shapes.large.copy(CornerSize(50)),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF1A06F9),
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


