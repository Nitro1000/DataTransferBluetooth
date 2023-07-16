package com.example.datatransferbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.subtle.AesGcmJce
import com.example.datatransferbluetooth.Models.BluetoothDeviceModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import javax.crypto.KeyAgreement
import kotlin.math.min


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
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var pairedDevices: List<BluetoothDeviceModel> = emptyList()

    // socket del servidor Bluetooth
    private var bluetoothServerSocket: BluetoothServerSocket? = null

    // socket del cliente Bluetooth
    private var bluetoothClientSocket: BluetoothSocket? = null

    // propiedad de escucha de datos Bluetooth
    private var bluetoothDataListener: BluetoothDataListener? = null

    // par de claves cliente
    private val serverKpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC")

    // par de claves servidor
    private val clientKpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC")

    private val serverKeyPair: KeyPair

    private val clientKeyPair: KeyPair

    private var serverPrivateKey: PrivateKey

    private var clientPublicKey: PublicKey

    private var sharedSecretserver: ByteArray? = null

    private var sharedSecretclient: ByteArray? = null

    private var clientPrivateKey: PrivateKey

    private var serverPublicKey: PublicKey

    private var aead: AesGcmJce? = null

    init {
        serverKpg.initialize(256)
        serverKeyPair = serverKpg.generateKeyPair()

        clientKpg.initialize(256)
        clientKeyPair = clientKpg.generateKeyPair()

        serverPrivateKey = serverKeyPair.private
        clientPublicKey = clientKeyPair.public

        clientPrivateKey= clientKeyPair.private
        serverPublicKey= serverKeyPair.public

        AeadConfig.register()

        updatePairedDevices()
    }

    // método para actualizar la lista de dispositivos Bluetooth emparejados.
    fun updatePairedDevices() {
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
                    bluetoothClientSocket?.outputStream?.apply {
                        val clientPublicKeySizeBuffer = ByteArray(4)
                        bluetoothClientSocket?.inputStream?.read(clientPublicKeySizeBuffer)

                        val clientPublicKeyBuffer = ByteArray(ByteBuffer.wrap(clientPublicKeySizeBuffer).int)
                        bluetoothClientSocket?.inputStream?.read(clientPublicKeyBuffer)

                        val keyFactory = KeyFactory.getInstance("EC")
                        val clientPublicKeySpec = X509EncodedKeySpec(clientPublicKeyBuffer)
                        clientPublicKey = keyFactory.generatePublic(clientPublicKeySpec)

                        val serverKeyAgreement: KeyAgreement = KeyAgreement.getInstance("ECDH")
                        serverKeyAgreement.init(serverPrivateKey)
                        serverKeyAgreement.doPhase(clientPublicKey, true)

                        sharedSecretserver= serverKeyAgreement.generateSecret()

                        val serverPublicKeySizeBuffer = ByteBuffer.allocate(4).putInt(serverPublicKey.encoded.size).array()
                        bluetoothClientSocket?.outputStream?.write(serverPublicKeySizeBuffer)

                        bluetoothClientSocket?.outputStream?.write(serverPublicKey.encoded)

                        val serverSharedSecret128 = sharedSecretserver?.sliceArray(0 until 16)

                        aead = AesGcmJce(serverSharedSecret128)


                    }
                    shouldLoop = false
                    Toast.makeText(context, "Conexión con cliente establecida", Toast.LENGTH_SHORT).show()
                }

                // Hilo para escuchar los mensajes entrantes del cliente
                Thread {
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
                            val allFileData: ArrayList<Byte> = ArrayList()
                            val fileBuffer = ByteArray(size = BUFFER_SIZE)
                            while (remainingBytes > 0){
                                bytes = bluetoothClientSocket?.inputStream?.read(fileBuffer,0,min(remainingBytes,BUFFER_SIZE)) ?: -1

                                if (bytes == -1) {
                                    break
                                }
                                remainingBytes -= bytes
                                // Guardar los datos recibidos
                                for (i in 0 until bytes) {
                                    allFileData.add(fileBuffer[i])
                                }
                            }
                            // Cuando ya no queda nada por leer, es momento de descifrar los datos.
                            val fileDataAsArray = allFileData.toByteArray()
                            val plaintext = aead?.decrypt(fileDataAsArray, null)

                            if (plaintext != null) {
                                saveFile(context, plaintext, fileName)
                            } else {
                                Log.e("Bluetooth", "Error al desencriptar los datos en startBluetoothServer")
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


                val clientPublicKeySizeBuffer = ByteBuffer.allocate(4).putInt(clientPublicKey.encoded?.size ?: 0).array()

                val clientPublicKeyBuffer = clientPublicKey.encoded ?: ByteArray(0)

                socket.outputStream.write(clientPublicKeySizeBuffer)

                socket.outputStream.write(clientPublicKeyBuffer)


                val serverPublicKeySizeBuffer = ByteArray(4)
                socket.inputStream.read(serverPublicKeySizeBuffer)

                val serverPublicKeySize = ByteBuffer.wrap(serverPublicKeySizeBuffer).int
                val serverPublicKeyBuffer = ByteArray(serverPublicKeySize)
                socket.inputStream.read(serverPublicKeyBuffer)

                val keyFactory = KeyFactory.getInstance("EC")
                val serverPublicKeySpec = X509EncodedKeySpec(serverPublicKeyBuffer)
                serverPublicKey = keyFactory.generatePublic(serverPublicKeySpec)

                val clientKeyAgreement: KeyAgreement = KeyAgreement.getInstance("ECDH")
                clientKeyAgreement.init(clientPrivateKey)
                clientKeyAgreement.doPhase(serverPublicKey, true)

                sharedSecretclient = clientKeyAgreement.generateSecret()
                val clientSharedSecret128 = sharedSecretclient?.sliceArray(0 until 16)

                aead = AesGcmJce(clientSharedSecret128)

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

                // mostrar data
                Log.d("data cliente", data.contentToString())

                val ciphertext = aead?.encrypt(data, null)
                // Enviar el tamaño del archivo cifrado
                write(ByteBuffer.allocate(4).putInt(ciphertext?.size ?: 0).array())
                // Enviar el archivo cifrado
                write(ciphertext ?: byteArrayOf())

            }
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al enviar los datos en sendData", e)
        }
    }


    // Construir y guardar el archivo con los datos recibidos
    private fun saveFile(context: Context, data: ByteArray, fileName: String){
        // Obtener el directorio de almacenamiento externo de la aplicación
        val externalFilesDir = context.getExternalFilesDir(null)

        // Crear un directorio llamado 'archivos_ejemplos' si no existe
        val fileDir = File(externalFilesDir, "Archivos_Recibidos")
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }

        // Crear un objeto File con la ruta del directorio y el nombre del archivo
        val file = File(fileDir, fileName)

        // Escribir los datos en el archivo
        FileOutputStream(file, true).use { fileOutputStream ->
            fileOutputStream.write(data)
        }
    }

    companion object{
        const val SERVICE_UUID = "caf59de6-8089-4fd5-9836-768a63fd2281"
        const val BUFFER_SIZE = 4096
    }
}