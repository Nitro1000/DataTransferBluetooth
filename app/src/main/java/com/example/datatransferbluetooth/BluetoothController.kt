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

    var alicePrivateKey: PrivateKey

    var bobPublicKey: PublicKey

    var sharedSecretAlice: ByteArray? = null

    var sharedSecretBob: ByteArray? = null

    var bobPrivateKey: PrivateKey

    var alicePublicKey: PublicKey

    var bobKeyAgreement: KeyAgreement

    var aliceKeyAgreement: KeyAgreement

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

        // cliente realiza el acuerdo de claves
        bobKeyAgreement = KeyAgreement.getInstance("ECDH")
        bobKeyAgreement.init(bobPrivateKey)

        // servidor realiza el acuerdo de claves
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
                        // Recibir la clave publica del cliente (bob)
                        // 1. Leer el tamaño de la clave publica
                        val bobPublicKeySizeBuffer = ByteArray(4)
                        bluetoothClientSocket?.inputStream?.read(bobPublicKeySizeBuffer)
                        // 2. Leer la clave publica
                        val bobPublicKeyBuffer = ByteArray(ByteBuffer.wrap(bobPublicKeySizeBuffer).int)
                        bluetoothClientSocket?.inputStream?.read(bobPublicKeyBuffer)
                        // mostrar la clave publica del cliente
//                        Log.d("Bob's public key servidor", bobPublicKeyBuffer.contentToString())
                        // pasar la clave publica del cliente bobPublicKeyBuffer a un objeto java.security.Key
                        val keyFactory = KeyFactory.getInstance("EC")
                        val bobPublicKeySpec = X509EncodedKeySpec(bobPublicKeyBuffer)
                        bobPublicKey = keyFactory.generatePublic(bobPublicKeySpec)
                        // Alice performs the key agreement
                        aliceKeyAgreement.doPhase(bobPublicKey, true)
                        // Generar la clave secreta compartida
                        sharedSecretAlice= aliceKeyAgreement.generateSecret()
                        // Mostrar la clave secreta compartida
//                        Log.d("Alice's shared secret", sharedSecretAlice?.contentToString() ?: "null")

                        // Enviar la clave pública de Alice al Cliente (Bob)
                        // 1. Enviar el tamaño de la clave publica
                        val alicePublicKeySizeBuffer = ByteBuffer.allocate(4).putInt(alicePublicKey.encoded.size).array()
                        bluetoothClientSocket?.outputStream?.write(alicePublicKeySizeBuffer)
                        // 2. Enviar la clave publica
                        bluetoothClientSocket?.outputStream?.write(alicePublicKey.encoded)
                        // Mostrar la clave publica de Alice
//                        Log.d("Alice's public key servidor", alicePublicKey.encoded.contentToString())

                        // 1. Divide by two the shared secret to have a 128 bit key
                        val aliceSharedSecret128 = sharedSecretAlice?.sliceArray(0 until 16)
                        // mostrar la clave secreta compartida
                        Log.d("Alice's shared secret 128", aliceSharedSecret128?.contentToString() ?: "null")
                        aead = AesGcmJce(aliceSharedSecret128)
                        // mostrar aead
                        Log.d("aead servidor", aead.toString())

                    }
                    shouldLoop = false
                    Toast.makeText(context, "Conexión con cliente establecida", Toast.LENGTH_SHORT).show()
                }

                /*Probando codigo del profesor*/
/*



                // Alice performs the key agreement
                aliceKeyAgreement.doPhase(bobPublicKey, true)

                // Generate shared secret
                sharedSecretAlice= aliceKeyAgreement.generateSecret()

                // Bob performs the key agreement
                bobKeyAgreement.doPhase(alicePublicKey, true)

                // Generate shared secret
                sharedSecretBob= bobKeyAgreement.generateSecret()

                // Mostrar ambas claves compartidas
                Log.d("Alice's shared secret", sharedSecretAlice?.contentToString() ?: "null")
                Log.d("Bob's shared secret", sharedSecretBob?.contentToString() ?: "null")

                // 1. Divide by two the shared secret to have a 128 bit key
                val aliceSharedSecret128 = sharedSecretAlice?.sliceArray(0 until 16)
                aead = AesGcmJce(aliceSharedSecret128)

*/


                /*Termina el codigo del profesor*/


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
//                                bytes = bluetoothClientSocket?.inputStream?.read(dataBuffer) ?: -1
                                if (bytes == -1) {
                                    break
                                }
                                remainingBytes -= bytes
                                // Desencriptar los datos recibidos
                                val plaintext = aead?.decrypt(fileBuffer, null)

                                // Construir y guardar el archivo con los datos recibidos
                                if (plaintext != null) {
                                    saveFile(context, plaintext, fileName, bytes)
                                }
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

                // Enviar la clave pública de Bob al servidor (alice)
                // 1. Sacar el tamaño de la clave pública de Bob
                val bobPublicKeySizeBuffer = ByteBuffer.allocate(4).putInt(bobPublicKey?.encoded?.size ?: 0).array()
                // 2. Sacar la clave pública de Bob
                val bobPublicKeyBuffer = bobPublicKey?.encoded ?: ByteArray(0)
                // 3. Enviar el tamaño de la clave pública de Bob
                socket.outputStream.write(bobPublicKeySizeBuffer)
                // 4. Enviar la clave pública de Bob
                socket.outputStream.write(bobPublicKeyBuffer)

                // mostrar la clave publica de bob
//                Log.d("Bob's public key cliente", bobPublicKeyBuffer.contentToString())

                //mostrar el clave publica de bob  bobPublicKey
//                Log.d("Bob's public key cliente bobPublicKey", bobPublicKey?.encoded?.contentToString() ?: "null")

                // Recibir la clave pública de Alice
                // 1. Sacar el tamaño de la clave pública de Alice
                val alicePublicKeySizeBuffer = ByteArray(4)
                socket.inputStream.read(alicePublicKeySizeBuffer)
                val alicePublicKeySize = ByteBuffer.wrap(alicePublicKeySizeBuffer).int
                // 2. Sacar la clave pública de Alice
                val alicePublicKeyBuffer = ByteArray(alicePublicKeySize)
                socket.inputStream.read(alicePublicKeyBuffer)
                // mostrar la clave publica del servidor
//                Log.d("Alice's public key cliente", alicePublicKeyBuffer.contentToString())
                // 3. Generar la clave pública de Alice
                alicePublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(alicePublicKeyBuffer))
                // Bob performs the key agreement
                bobKeyAgreement.doPhase(alicePublicKey, true)
                // Generate shared secret
                sharedSecretBob= bobKeyAgreement.generateSecret()
                // Mostrar ambas claves compartidas
//                Log.d("Bob's shared secret", sharedSecretBob?.contentToString() ?: "null")

                // 1. Divide by two the shared secret to have a 128 bit key
                val bobSharedSecret128 = sharedSecretBob?.sliceArray(0 until 16)
                // mostrar la clave compartida de bob
                Log.d("Bob's shared secret 128", bobSharedSecret128?.contentToString() ?: "null")
                aead = AesGcmJce(bobSharedSecret128)
                // mostrar aead
                Log.d("aead cliente", aead.toString())

                // cliente [36, -108, 92, -80, -19, -36, -109, 101, 96, 16, -34, 3, 115, 41, 78, -46, -69, 62, 109, -23, 101, -68, -51, 60, -111, -52, 31, 6, -123, -56, -28, -126]
                // servidor [36, -108, 92, -80, -19, -36, -109, 101, 96, 16, -34, 3, 115, 41, 78, -46, -69, 62, 109, -23, 101, -68, -51, 60, -111, -52, 31, 6, -123, -56, -28, -126]



                /**
                 * val bobPublicKeyBytes = bobPublicKey?.encoded
                socket.outputStream.write(ByteBuffer.allocate(4).putInt(bobPublicKeyBytes?.size ?: 0).array())
                socket.outputStream.write(bobPublicKeyBytes ?: byteArrayOf())
                 */




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

                val ciphertext = aead?.encrypt(data, null)
                // Enviar el tamaño del archivo cifrado
                write(ByteBuffer.allocate(4).putInt(ciphertext?.size ?: 0).array())
                // Enviar el archivo cifrado
                write(ciphertext ?: byteArrayOf())
                // mostrar el archivo cifrado
                Log.d("ciphertext cliente", ciphertext?.contentToString() ?: "null")

                // Enviar el tamaño del archivo
//                write(ByteBuffer.allocate(4).putInt(data.size).array())
                // Enviar el archivo
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