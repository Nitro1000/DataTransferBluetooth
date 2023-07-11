package com.example.datatransferbluetooth.presentation

import android.content.Context
import android.net.Uri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.datatransferbluetooth.BluetoothController
import com.example.datatransferbluetooth.BluetoothDeviceModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    navController: NavHostController,
    device: BluetoothDeviceModel,
    modifier: Modifier = Modifier,
    bluetoothController: BluetoothController
) {
    val context = bluetoothController.context

    val selectedFileUri = remember { mutableStateOf("") }
    val selectedFileName = remember { mutableStateOf("") }

    // Selector de archivos
    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { result ->
        val uri = result ?: return@rememberLauncherForActivityResult
        selectedFileUri.value = uri.toString()

        uri.let { uri ->
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    selectedFileName.value = displayName
                } else {
                    Toast.makeText(context, "No se pudo obtener el nombre del archivo", Toast.LENGTH_SHORT).show()
                    null
                }
            }
        }

    }

    Scaffold(
        topBar = {
            CustomAppBar(
                title = "Selecciona un archivo",
                navigationIcon = Icons.Filled.ArrowBack) {
                navController.navigate(route = "pairedDevices") {
                    popUpTo(route = "pairedDevices")
                }
            }
        },
        content = { padding ->
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(padding)) {
                    Text(
                        text = "Conectado a: ${device.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    Text(
                        text = selectedFileName.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    Button(
                        onClick = { filePickerLauncher.launch("application/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = "Seleccionar archivo")
                    }
                    if (selectedFileUri.value.isNotEmpty()) {
                        Button(
                            onClick = {
                                val uri = selectedFileUri.value
                                if (uri.isNotEmpty()) {
                                    val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                                    val bytes = inputStream?.readBytes()
                                    inputStream?.close()
                                    bytes?.let { byteArray ->
                                        bluetoothController.sendData(byteArray, selectedFileName.value)
                                    }
                                } else {
                                    showErrorMessage("No se ha seleccionado ningún archivo", context)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(text = "Enviar archivo")
                        }
                    }

                }
            }
        }
    )
}

fun showErrorMessage(message: String, context: Context) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
