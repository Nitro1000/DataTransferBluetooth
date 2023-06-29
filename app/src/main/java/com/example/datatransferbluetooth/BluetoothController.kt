package com.example.datatransferbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.crypto.tink.Aead
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadFactory
import com.google.crypto.tink.aead.AeadKeyTemplates
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
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
    private var keysetHandle: KeysetHandle

    // Instancia de AEAD para cifrar y descifrar los datos
    private var aead: Aead


    init {
        updatePairedDevices()

        // Registra la configuración de Tink
        AeadConfig.register()
        keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM)
        aead = AeadFactory.getPrimitive(keysetHandle)


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
                            // Leer el tamaño del keysetString que pasó el cliente
                            val keysetStringSizeBuffer = ByteArray(4)
                            bluetoothClientSocket?.inputStream?.read(keysetStringSizeBuffer)
                            val keysetStringSize = ByteBuffer.wrap(keysetStringSizeBuffer).int
                            // Leer el keysetString que pasó el cliente
                            val keysetStringBuffer = ByteArray(keysetStringSize)
                            bluetoothClientSocket?.inputStream?.read(keysetStringBuffer)
                            val keysetString = String(keysetStringBuffer)

                            val inputStream = ByteArrayInputStream(keysetString.toByteArray(Charset.defaultCharset()))
                            val receivedKeysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withInputStream(inputStream))


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
                                // Descifrar los datos recibidos
                                val decryptedData = receivedKeysetHandle.getPrimitive(Aead::class.java).decrypt(fileBuffer, null)
                                // Construir y guardar el archivo con los datos recibidos
                                saveFile(context, decryptedData, fileName, bytes)
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
                val outputStream = ByteArrayOutputStream()
                CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(outputStream))
                val keysetString = String(outputStream.toByteArray(), Charset.defaultCharset())

                // Enviar el tamaño del keysetString
                write(ByteBuffer.allocate(4).putInt(keysetString.toByteArray().size).array())
                // Enviar el keysetString
                write(keysetString.toByteArray())

                // mostrar el nombre del archivo que se está enviando
                bluetoothDataListener?.onDataReceived("Enviando archivo: $fileName")

                // Enviar el tamaño nombre del archivo
                write(ByteBuffer.allocate(4).putInt(fileName.toByteArray().size).array())

                // Enviar el nombre del archivo
                write(fileName.toByteArray())

                // Enviar el tamaño del archivo
                write(ByteBuffer.allocate(4).putInt(data.size).array())
                // cifrar data antes de enviar
                val encryptedData = encryptData(data)
                write(encryptedData) // Enviar el archivo
            }
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al enviar los datos en sendData", e)
        }
    }

    private fun encryptData(data: ByteArray): ByteArray {
        val aead = keysetHandle.getPrimitive(Aead::class.java)
        val ciphertext = aead.encrypt(data, null)
        return ciphertext
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