package com.example.datatransferbluetooth.presentation


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.datatransferbluetooth.BluetoothController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    bluetoothController: BluetoothController
) {
    Scaffold(
        topBar = {
            CustomAppBar(navigationIcon = Icons.Filled.ArrowBack){
                navController.navigate(route = "main"){
                    popUpTo(route = "main")
                }
            }
        },
        content = { padding ->
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(padding)) {
                    androidx.compose.material.Button(
                        onClick = {
                            bluetoothController.startBluetoothServer()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.material.MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = androidx.compose.material.MaterialTheme.colors.secondary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 8.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        androidx.compose.material.Text(text = "Iniciar Servidor")
                    }
                    androidx.compose.material.Button(
                        onClick = {
                            bluetoothController.closeConnection()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.material.MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = androidx.compose.material.MaterialTheme.colors.secondary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 8.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        androidx.compose.material.Text(text = "Cerrar la conexión")
                    }
                }
            }
        }
    )
}