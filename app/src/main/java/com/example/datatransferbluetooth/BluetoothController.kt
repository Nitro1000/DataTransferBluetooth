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

    // par de claves cliente
    val aliceKpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC")

    // par de claves servidor
    val bobKpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC")

    val aliceKeyPair: KeyPair

    val bobKeyPair: KeyPair

    val alicePrivateKey: PrivateKey

    val bobPublicKey: PublicKey

    var sharedSecretAlice: ByteArray? = null

    var sharedSecretBob: ByteArray? = null

    val bobPrivateKey: PrivateKey

    val alicePublicKey: PublicKey

    val bobKeyAgreement: KeyAgreement

    val aliceKeyAgreement: KeyAgreement

    var keyset128:  List<Byte> = emptyList()

    var aead: AesGcmJce? = null

    init {
        aliceKpg.initialize(256) // Key size in bits
        aliceKeyPair = aliceKpg.generateKeyPair()

        bobKpg.initialize(256) // Key size in bits
        bobKeyPair = bobKpg.generateKeyPair()

        // Alice's private key and Bob's public key
        alicePrivateKey = aliceKeyPair.private
        bobPublicKey = bobKeyPair.public

        // Bob performs the key agreement
        bobPrivateKey= bobKeyPair.private
        alicePublicKey= aliceKeyPair.public

        bobKeyAgreement = KeyAgreement.getInstance("ECDH")
        bobKeyAgreement.init(bobPrivateKey)

        aliceKeyAgreement = KeyAgreement.getInstance("ECDH")
        aliceKeyAgreement.init(alicePrivateKey)

        AeadConfig.register()

        updatePairedDevices()
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
                    bluetoothClientSocket?.outputStream?.apply {
                        // leer el tamaño de la clave pública del cliente
                        val bobPublicKeySizeBuffer = ByteArray(4)
                        bluetoothClientSocket?.inputStream?.read(bobPublicKeySizeBuffer)
                        // obtener la clave pública del cliente
                        val bobPublicKeyBuffer = ByteArray(ByteBuffer.wrap(bobPublicKeySizeBuffer).int)
                        bluetoothClientSocket?.inputStream?.read(bobPublicKeyBuffer)
                        // convertir la clave pública del cliente en un objeto PublicKey
                        val bobPublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bobPublicKeyBuffer))
                        // generar la clave compartida
                        aliceKeyAgreement.doPhase(bobPublicKey, true)
                        sharedSecretAlice = aliceKeyAgreement.generateSecret()
                        // enviar el tamañp de la clave pública del servidor al cliente
                        write(ByteBuffer.allocate(4).putInt(alicePublicKey.encoded.size).array())
                        // enviar la clave pública del servidor al cliente
                        write(alicePublicKey.encoded)

                    }
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
//                            val fileBuffer = ByteArray(size = bufferSize)
                            val dataBuffer = ByteArray(min(remainingBytes, bufferSize))
                            while (remainingBytes > 0){
//                                bytes = bluetoothClientSocket?.inputStream?.read(fileBuffer,0,min(remainingBytes,bufferSize)) ?: -1
                                bytes = bluetoothClientSocket?.inputStream?.read(dataBuffer) ?: -1
                                if (bytes == -1) {
                                    break
                                }
                                remainingBytes -= bytes

                                // Desencriptar el archivo
                                val keyset128 = sharedSecretAlice?.take(16)
                                aead = AesGcmJce(keyset128?.toByteArray())
                                val decrypted = aead!!.decrypt(dataBuffer,null)
                                // Guardar el archivo
                                if (decrypted != null) {
                                    saveFile(context, decrypted, fileName, decrypted?.size ?: 0)
                                }else{
                                    Log.e("Bluetooth", "Error al desencriptar el archivo")
                                }

                                // Construir y guardar el archivo con los datos recibidos
//                                saveFile(context, fileBuffer, fileName, bytes)
//                                saveFile(context, ciphertext, fileName, ciphertext.size)
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
                // enviar el tamaño de la clave pública del servidor
                socket.outputStream?.write(ByteBuffer.allocate(4).putInt(bobPublicKey.encoded.size).array())
                // enviar la clave pública del servidor
                socket.outputStream?.write(bobPublicKey.encoded)
                // leer el tamaño de la clave pública del cliente
                val alicePublicKeySizeBuffer = ByteArray(4)
                socket.inputStream?.read(alicePublicKeySizeBuffer)
                // obtener la clave pública del cliente
                val alicePublicKeyBuffer = ByteArray(ByteBuffer.wrap(alicePublicKeySizeBuffer).int)
                socket.inputStream?.read(alicePublicKeyBuffer)
                // convertir la clave pública del cliente en un objeto PublicKey
                val alicePublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(alicePublicKeyBuffer))
                // generar la clave compartida
                bobKeyAgreement.doPhase(alicePublicKey, true)
                sharedSecretBob = bobKeyAgreement.generateSecret()

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
                bluetoothDataListener?.onDataReceived("Enviando archivo: $fileName")

                // Enviar el tamaño nombre del archivo
                write(ByteBuffer.allocate(4).putInt(fileName.toByteArray().size).array())

                // Enviar el nombre del archivo
                write(fileName.toByteArray())

                // cifrar los datos con la clave compartida
                val keyset128 = sharedSecretBob?.take(16)
                aead = AesGcmJce(keyset128?.toByteArray())
                val ciphertext = aead!!.encrypt(data, null)

                // Enviar el tamaño del archivo
                write(ByteBuffer.allocate(4).putInt(ciphertext.size).array())
                // Enviar el archivo
                write(ciphertext)


//                // Enviar el tamaño del archivo
//                write(ByteBuffer.allocate(4).putInt(data.size).array())
//                // Enviar el archivo
//                write(data)
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