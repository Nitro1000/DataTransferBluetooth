package com.example.datatransferbluetooth

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.datatransferbluetooth.Models.BluetoothDeviceModel
import com.example.datatransferbluetooth.presentation.BluetoothDeviceListScreen
import com.example.datatransferbluetooth.presentation.ClientScreen
import com.example.datatransferbluetooth.presentation.MainScreen
import com.example.datatransferbluetooth.presentation.ServerScreen
import com.example.datatransferbluetooth.presentation.SplashScreen

class MainActivity : ComponentActivity() {

    private val permissionRequestCode = 200
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN
    )

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(
                    this,
                    "Bluetooth activado",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Es necesario activar el Bluetooth",
                    Toast.LENGTH_SHORT
                ).show()
                // Si el usuario no activa el Bluetooth, vuelve a solicitar la activación
                checkBluetoothAndRequestEnable()
            }
        }

    private fun checkBluetoothAndRequestEnable() {
        if (bluetoothAdapter == null) {
            Toast.makeText(
                this,
                "Bluetooth no disponible",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                requestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent){
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED){
                when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)){
                    BluetoothAdapter.STATE_OFF -> {
                        checkBluetoothAndRequestEnable()
                    }

                }
            }
        }
    }

    private fun registerBluetoothStateReceiver(){
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aquí verificamos si tenemos los permisos.
        if (!hasPermissions(requiredPermissions)) {
            // Si no los tenemos, los solicitamos.
            ActivityCompat.requestPermissions(this, requiredPermissions, permissionRequestCode)
        }

        checkBluetoothAndRequestEnable()
        registerBluetoothStateReceiver()

        // Instanciar el controlador bluetooth
        val bluetoothController = BluetoothController(
            context = this
        )
        val connectionListener = object : BluetoothConnectionListener {
            override fun onConnectionSuccess() {
                // La conexión se estableció con éxito
                Toast.makeText(
                    this@MainActivity,
                    "Conexión establecida",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onConnectionError() {
                // Ocurrió un error al establecer la conexión
                Toast.makeText(
                    this@MainActivity,
                    "Error al establecer la conexión",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val dataListener = object : BluetoothDataListener {
            override fun onDataReceived(data: String) {
                runOnUiThread{
                    // Se recibió un mensaje de datos
                    Toast.makeText(
                        this@MainActivity,
                        "Mensaje recibido: $data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        bluetoothController.setDataListener(listener = dataListener)

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "welcome") {
                composable(route = "main") {
                    MainScreen(
                        navController = navController,
                        bluetoothController = bluetoothController
                    )
                }
                composable(route = "pairedDevices"){
                    BluetoothDeviceListScreen(
                        navController = navController,
                        bluetoothController = bluetoothController,
                        connectionListener = connectionListener
                    )
                }
                composable(route = "client/{deviceadress}/{devicename}") {backStackEntry ->
                    val deviceadress = backStackEntry.arguments?.getString("deviceadress")
                    val devicename = backStackEntry.arguments?.getString("devicename")
                    val device = deviceadress?.let {
                        BluetoothDeviceModel(
                            address = it,
                            name = devicename
                        )
                    }
                    if (device != null) {
                        ClientScreen(
                            navController = navController,
                            device = device,
                            bluetoothController = bluetoothController
                        )
                    }
                }
                composable(route = "server") {
                    ServerScreen(
                        navController = navController,
                        bluetoothController = bluetoothController
                    )
                }
                composable(route = "welcome") {
                    SplashScreen(
                        navController = navController
                    )
                }
            }
        }
    }

    // Método para verificar si los permisos están concedidos.
    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Método para manejar la respuesta del usuario a la solicitud de permisos.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
                AlertDialog.Builder(this)
                    .setTitle("Solicitud de permisos")
                    .setMessage("¿Acepta conceder los permisos requeridos para Bluetooth?")
                    .setPositiveButton("Aceptar") { _, _ -> requestPermissions(requiredPermissions, 0) }
                    .setNegativeButton("Cancelar") { _, _ -> finish() }
                    .create()
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
    }

}

