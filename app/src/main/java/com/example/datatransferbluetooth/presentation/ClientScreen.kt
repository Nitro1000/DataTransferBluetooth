package com.example.datatransferbluetooth.presentation

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.datatransferbluetooth.BluetoothController
import com.example.datatransferbluetooth.BluetoothDeviceModel
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    navController: NavHostController,
    device: BluetoothDeviceModel,
    modifier: Modifier = Modifier,
    bluetoothController: BluetoothController
) {

    val messageState = remember { mutableStateOf("") }

    val context = bluetoothController.context

    val uriFile = remember { mutableStateOf("") }

    // selector de archivos
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { result ->
        val uri = result ?: return@rememberLauncherForActivityResult
        uriFile.value = uri.toString()

        val fileName = uri.let { uri ->
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    displayName
                } else {
                    null
                }
            }
        }
        messageState.value = fileName ?: ""

        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        bytes?.let { byteArray ->
            if (fileName != null) {
                bluetoothController.sendData(byteArray, fileName)
            }else {
                Toast.makeText(context, "No se pudo enviar el archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CustomAppBar(navigationIcon = Icons.Filled.ArrowBack) {
                navController.navigate(route = "pairedDevices") {
                    popUpTo(route = "pairedDevices")
                }
            }
        },
        content = { padding ->
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(padding)) {

                    // Nombre del dispositivo al que se conecta
                    Text(
                        text = "Conectado a: ${device.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    // Campo de texto y botón para enviar datos al servidor
                    TextField(
                        value = messageState.value,
                        onValueChange = { value -> messageState.value = value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    // Botón para seleccionar un archivo
                    Button(
                        onClick = {
                            // contentActivityResultLauncher.launch("application/*")
                            launcher.launch("application/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = "Seleccionar archivo")
                    }

                    // Uri del archivo seleccionado
                    Text(
                        text = uriFile.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    )
}