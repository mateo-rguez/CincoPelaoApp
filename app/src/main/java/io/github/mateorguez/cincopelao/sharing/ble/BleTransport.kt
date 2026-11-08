package io.github.mateorguez.cincopelao.sharing.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.sharing.protocol.BleAdvert
import io.github.mateorguez.cincopelao.sharing.protocol.ReceiverBeacon
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeout

/** Lo que pasa en el anuncio del receptor (para el registro técnico). */
sealed interface AdvertiseEvent {
    data class Started(val txPower: String, val mode: String) : AdvertiseEvent
    data class ClientConnected(val address: String) : AdvertiseEvent
    data class BeaconRead(val address: String, val offset: Int, val bytes: Int) : AdvertiseEvent
    data class ClientDisconnected(val address: String) : AdvertiseEvent
}

/** Un receptor visto en el escaneo. */
data class ScanSighting(
    val address: String,
    val rssi: Int,
    val txPower: Int?,
    val protocolVersion: Int,
    val displayName: String,
    val timestampMillis: Long
)

/** Resultado de leer la baliza GATT, con las cifras de la conexión. */
data class BeaconRead(val beacon: ReceiverBeacon, val mtu: Int, val elapsedMillis: Long)

class BleException(message: String) : Exception(message)

/**
 * Bluetooth LE de la compartición. Cada operación vive lo que la corrutina que la usa: al
 * cancelarla se para el anuncio o el escaneo y se cierran los GATT. Antes el escaneo no se
 * paraba nunca y `stopServer` no detenía el anuncio.
 *
 * Los permisos se comprueban en la pantalla antes de llamar aquí.
 */
@SuppressLint("MissingPermission")
@Singleton
class BleTransport @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager get() = context.getSystemService(BluetoothManager::class.java)

    /**
     * Receptor: publica la baliza por GATT y se anuncia hasta que se cancele el Flow.
     */
    fun advertise(displayName: String, beacon: ReceiverBeacon): Flow<AdvertiseEvent> = callbackFlow {
        val btManager = manager ?: throw BleException("Este móvil no tiene Bluetooth")
        val advertiser = btManager.adapter?.bluetoothLeAdvertiser
            ?: throw BleException("Este móvil no puede anunciarse por Bluetooth LE")
        val value = beacon.encode()

        var server: BluetoothGattServer? = null
        val serverCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                trySend(
                    if (newState == BluetoothProfile.STATE_CONNECTED) AdvertiseEvent.ClientConnected(device.address)
                    else AdvertiseEvent.ClientDisconnected(device.address)
                )
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                // Lecturas largas: si la MTU es pequeña, Android pide el resto con offset
                val ok = characteristic.uuid == BleAdvert.BEACON_CHARACTERISTIC_UUID && offset <= value.size
                val chunk = if (ok) value.copyOfRange(offset, value.size) else null
                server?.sendResponse(
                    device, requestId,
                    if (ok) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_INVALID_OFFSET,
                    offset, chunk
                )
                if (ok) trySend(AdvertiseEvent.BeaconRead(device.address, offset, chunk?.size ?: 0))
            }
        }
        val gattServer = btManager.openGattServer(context, serverCallback)
            ?: throw BleException("No se pudo abrir el servidor GATT")
        server = gattServer
        val service = BluetoothGattService(BleAdvert.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).apply {
            addCharacteristic(
                BluetoothGattCharacteristic(
                    BleAdvert.BEACON_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ
                )
            )
        }
        gattServer.addService(service)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleAdvert.SERVICE_UUID))
            .setIncludeDeviceName(false) // el nombre del móvil no se publica: solo el que elige el usuario
            .build()
        val scanResponse = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(BleAdvert.SERVICE_UUID), BleAdvert.encodeServiceData(displayName))
            .build()
        val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                val power = when (settingsInEffect.txPowerLevel) {
                    AdvertiseSettings.ADVERTISE_TX_POWER_HIGH -> "HIGH"
                    AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM -> "MEDIUM"
                    AdvertiseSettings.ADVERTISE_TX_POWER_LOW -> "LOW"
                    else -> "ULTRA_LOW"
                }
                trySend(AdvertiseEvent.Started(txPower = power, mode = "LOW_LATENCY"))
            }

            override fun onStartFailure(errorCode: Int) {
                close(BleException("No se pudo anunciar por Bluetooth (${advertiseError(errorCode)})"))
            }
        }
        advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback)

        awaitClose {
            runCatching { advertiser.stopAdvertising(advertiseCallback) }
            runCatching { gattServer.clearServices() }
            runCatching { gattServer.close() }
        }
    }

    /**
     * Emisor: busca receptores de Cinco Pelao hasta que se cancele el Flow.
     */
    fun scan(): Flow<ScanSighting> = callbackFlow {
        val scanner = manager?.adapter?.bluetoothLeScanner
            ?: throw BleException("El Bluetooth está apagado o no hay Bluetooth LE")
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord
                val (version, name) = BleAdvert.decodeServiceData(
                    record?.getServiceData(ParcelUuid(BleAdvert.SERVICE_UUID))
                ) ?: return // sin respuesta al escaneo todavía: llegará en el siguiente resultado
                trySend(
                    ScanSighting(
                        address = result.device.address,
                        rssi = result.rssi,
                        txPower = result.txPower.takeIf { it != ScanResult.TX_POWER_NOT_PRESENT },
                        protocolVersion = version,
                        displayName = name,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                close(BleException("El escaneo Bluetooth ha fallado (código $errorCode)"))
            }
        }
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(BleAdvert.SERVICE_UUID)).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        scanner.startScan(listOf(filter), settings, callback)

        awaitClose { runCatching { scanner.stopScan(callback) } }
    }

    /**
     * Emisor: se conecta al receptor [address], pide una MTU grande, lee la baliza y se desconecta.
     */
    suspend fun readBeacon(address: String, timeoutMillis: Long = 15_000): BeaconRead {
        val adapter = manager?.adapter ?: throw BleException("Este móvil no tiene Bluetooth")
        val device = adapter.getRemoteDevice(address)
        val start = System.currentTimeMillis()
        val result = CompletableDeferred<BeaconRead>()
        var mtu = 23

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when {
                    newState == BluetoothProfile.STATE_CONNECTED -> gatt.requestMtu(517)
                    newState == BluetoothProfile.STATE_DISCONNECTED ->
                        result.completeExceptionally(BleException("El receptor se ha desconectado (GATT $status)"))
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, newMtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) mtu = newMtu
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val characteristic = gatt.getService(BleAdvert.SERVICE_UUID)
                    ?.getCharacteristic(BleAdvert.BEACON_CHARACTERISTIC_UUID)
                if (characteristic == null || !gatt.readCharacteristic(characteristic)) {
                    result.completeExceptionally(BleException("Ese dispositivo no ofrece el servicio de Cinco Pelao"))
                }
            }

            // Android 13+
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) = deliver(value, status)

            // Android 12 y anteriores
            @Deprecated("Sustituido en Android 13")
            @Suppress("DEPRECATION")
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) =
                deliver(characteristic.value ?: ByteArray(0), status)

            private fun deliver(value: ByteArray, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    result.completeExceptionally(BleException("No se pudo leer la baliza (GATT $status)"))
                    return
                }
                runCatching { ReceiverBeacon.decode(value) }
                    .onSuccess { result.complete(BeaconRead(it, mtu, System.currentTimeMillis() - start)) }
                    .onFailure { result.completeExceptionally(it) }
            }
        }

        val gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            ?: throw BleException("No se pudo abrir la conexión GATT")
        try {
            return withTimeout(timeoutMillis) { result.await() }
        } finally {
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }
    }

    private fun advertiseError(code: Int): String = when (code) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "datos demasiado grandes"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "demasiados anuncios activos"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ya estaba anunciándose"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "error interno"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "no soportado por este móvil"
        else -> "código $code"
    }
}
