package com.example.datatransferbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.crypto.tink.Aead
import com.google.crypto.tink.AeadFactory
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.GeneralSecurityException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.min

interface BluetoothConnectionListener {
    fun onConnectionSuccess()
    fun onConnectionError()
}

// interfaz de escucha de datos Bluetooth
interface BluetoothDataListener {
    fun onDataReceived(data: String)
}

@SuppressLint("MissingPermission")
class BluetoothController(
    val context: Context
)
{
    // obtener una instancia del sistema de servicios de Bluetooth
    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    // obtener una instancia del adaptador Bluetooth
    val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var pairedDevices: List<BluetoothDeviceModel> = emptyList()

    // socket del servidor Bluetooth
    private var bluetoothServerSocket: BluetoothServerSocket? = null

    // socket del cliente Bluetooth
    private var bluetoothClientSocket: BluetoothSocket? = null

    // propiedad de escucha de datos Bluetooth
    private var bluetoothDataListener: BluetoothDataListener? = null

    // Conjunto de claves para cifrar y descifrar los datos
//    private var keysetHandle: KeysetHandle

    // Instancia de AEAD para cifrar y descifrar los datos
    private var aead: Aead


    init {
        updatePairedDevices()

        // Registra la configuración de Tink
        TinkConfig.register()

        // Genera o carga el conjunto de claves en función de si ya existe o no
        val keysetManager = AndroidKeysetManager.Builder()
            .withSharedPref(context, "keyset_prefs", "keyset")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://keyset_id")
            .build()

        if (!keysetManager.isKeysetLoaded) {
            keysetManager.generateNewMasterKey()
        }

        try {
            keysetManager.getPrimitive("android-keystore://keyset_id", Aead::class.java)?.let { primitive ->
                aead = primitive
            } ?: throw GeneralSecurityException("Error loading keyset")
        } catch (e: GeneralSecurityException) {
            // Manejar el error en caso de que ocurra
            aead = AeadFactory.getPrimitive(AeadKeyTemplates.AES256_GCM)
            Log.e("Bluetooth", "Error al cargar el conjunto de claves: ${e.message}")
        }
    }

    // método para actualizar la lista de dispositivos Bluetooth emparejados.
    private fun updatePairedDevices() {
        pairedDevices = bluetoothAdapter?.bondedDevices?.map {
            BluetoothDeviceModel(
                name = it.name,
                address = it.address
            )
        } ?: emptyList()
    }

    fun getPairedDevices(): List<BluetoothDeviceModel>{
        return pairedDevices
    }

    // método para iniciar el servidor Bluetooth.
    fun startBluetoothServer() {
        bluetoothServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
            "BluetoothServer",
            UUID.fromString(SERVICE_UUID)
        )

        var shouldLoop = true
        while(shouldLoop){
            try {
                bluetoothServerSocket?.accept()?.let { socket ->
                    bluetoothServerSocket?.close()
                    bluetoothServerSocket = null
                    bluetoothClientSocket = socket
                    shouldLoop = false
                    Toast.makeText(context, "Conexión con cliente establecida", Toast.LENGTH_SHORT).show()
                }

                // Hilo para escuchar los mensajes entrantes del cliente
                Thread {
                    val bufferSize = 4096 // Tamaño del buffer en bytes
                    while (true) {
                        try {
                            // Leer el tamaño nombre del archivo
                            val fileNameSizeBuffer = ByteArray(4)
                            bluetoothClientSocket?.inputStream?.read(fileNameSizeBuffer)
                            val fileNameSize = ByteBuffer.wrap(fileNameSizeBuffer).int
                            // Leer el nombre del archivo
                            val fileNameBuffer = ByteArray(fileNameSize)
                            bluetoothClientSocket?.inputStream?.read(fileNameBuffer)
                            val fileName = String(fileNameBuffer)

                            // Leer el tamaño del archivo
                            val fileSizeBuffer = ByteArray(4)
                            bluetoothClientSocket?.inputStream?.read(fileSizeBuffer)
                            val fileSize = ByteBuffer.wrap(fileSizeBuffer).int
                            var bytes: Int
                            var remainingBytes = fileSize
                            val fileBuffer = ByteArray(size = bufferSize)
                            while (remainingBytes > 0){
                                bytes = bluetoothClientSocket?.inputStream?.read(fileBuffer,0,min(remainingBytes,bufferSize)) ?: -1
                                    if (bytes == -1) {
                                    break
                                }
                                remainingBytes -= bytes
                                // Construir y guardar el archivo con los datos recibidos
                                saveFile(context, fileBuffer, fileName, bytes)
//                                saveFile(context, decryptedData, fileName, decryptedData.size)
                            }
                        } catch (e: IOException) {
                            // Error al leer los datos
                            Log.e("Bluetooth", "Error al leer los datos en startBluetoothServer", e)
                            break
                        }
                    }
                }.start()
            }catch (e: IOException){
                shouldLoop = false
            }
        }
    }

    // método para conectar con un dispositivo Bluetooth.
    fun connectToBluetoothDevice(
        device: BluetoothDeviceModel,
        connectionListener: BluetoothConnectionListener
    ) {
        bluetoothClientSocket = bluetoothAdapter
            ?.getRemoteDevice(device.address)
            ?.createRfcommSocketToServiceRecord(
            UUID.fromString(SERVICE_UUID)
        )
        bluetoothClientSocket?.let { socket ->
            try {
                socket.connect()
                connectionListener.onConnectionSuccess()
            } catch (e: IOException) {
                socket.close()
                bluetoothClientSocket = null
                connectionListener.onConnectionError()
            }
        }
    }

    // método para detener la conexión del servidor Bluetooth.
    fun closeConnection() {
        bluetoothServerSocket?.close()
        bluetoothServerSocket = null
        bluetoothClientSocket?.close()
        bluetoothClientSocket = null
    }

    // método para configurar la escucha de datos
    fun setDataListener(listener: BluetoothDataListener){
        bluetoothDataListener = listener
    }

    // método para enviar datos a través de Bluetooth desde el cliente al servidor.
    fun sendData(data: ByteArray, fileName: String) {
        try {
            bluetoothClientSocket?.outputStream?.apply {
                // mostrar el nombre del archivo que se está enviando
                bluetoothDataListener?.onDataReceived("Enviando archivo: $fileName")

                // Enviar el tamaño nombre del archivo
                write(ByteBuffer.allocate(4).putInt(fileName.toByteArray().size).array())

                // Enviar el nombre del archivo
                write(fileName.toByteArray())

                // Enviar el tamaño del archivo
                write(ByteBuffer.allocate(4).putInt(data.size).array())
                write(data) // Enviar el archivo
            }
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al enviar los datos en sendData", e)
        }
    }
    // Construir y guardar el archivo con los datos recibidos
    private fun saveFile(context: Context, data: ByteArray, fileName: String, bytes: Int){
        // Obtener el directorio de almacenamiento externo de la aplicación
        val externalFilesDir = context.getExternalFilesDir(null)

        // Crear un directorio llamado 'archivos_ejemplos' si no existe
        val fileDir = File(externalFilesDir, "archivos_ejemplos")
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }

        // Crear un objeto File con la ruta del directorio y el nombre del archivo
        val file = File(fileDir, fileName)

        // Utilizar un FileOutputStream para escribir los datos en el archivo
        val fileOutputStream = FileOutputStream(file, true)

        // Escribir los datos en el archivo
        try {
            fileOutputStream.write(data, 0, bytes)
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al escribir los datos en saveFile", e)
        } finally {
            fileOutputStream.close()
        }
    }

    companion object{
        const val SERVICE_UUID = "caf59de6-8089-4fd5-9836-768a63fd2281"
    }
}