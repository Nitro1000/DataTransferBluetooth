package com.example.datatransferbluetooth.presentation


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                    CustomButton(
                        text = "Iniciar Servidor",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        bluetoothController.startBluetoothServer()
                    }
                    CustomButton(
                        text = "Cerrar la conexión",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        bluetoothController.closeConnection()
                    }
                }
            }
        }
    )
}