package com.example.cincopelao

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import javax.inject.Singleton

interface BleRepository {
    fun startAdvertising(SSID: String, passphrase: String, deviceId: String, mtu: Int = 11)
    fun stopAdvertising()
    fun startScanning(onDeviceFound: (ScanResult, String) -> Unit)
    fun stopScanning()
}

@Singleton
class BleRepositoryImpl @Inject constructor(
    private val context: Context
) : BleRepository {

    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null

    override fun startAdvertising(SSID: String, passphrase: String, deviceId: String, mtu: Int) {
        val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser ?: return

        val fullPayload = "$SSID.$passphrase.$deviceId".toByteArray(Charsets.UTF_8)

        val packets = mutableListOf<ByteArray>()
        var index = 0
        val maxChunkSize = mtu
        while (index < fullPayload.size) {
            val end = minOf(index + maxChunkSize, fullPayload.size)
            packets.add(fullPayload.copyOfRange(index, end))
            index = end
        }

        Log.d("BLE_ADVERT", "Payload total: ${fullPayload.size} bytes, fragmentos: ${packets.size}")

        var packetIndex = 0
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        fun advertiseNext() {
            if (packetIndex >= packets.size) return

            val packetPayload = byteArrayOf(packetIndex.toByte(), packets.size.toByte()) + packets[packetIndex]
            Log.d("BLE_ADVERT", "Enviando paquete $packetIndex/${packets.size} (${packets[packetIndex].size} bytes)")

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceData(
                    ParcelUuid(UUID.fromString("e4a2c8aa-62f1-4b9c-8d39-57c8d908b33f")),
                    packetPayload
                )
                .build()

            advertiseCallback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.d("BLE_ADVERT", "Paquete $packetIndex enviado correctamente")
                    packetIndex++
                    Handler(Looper.getMainLooper()).postDelayed({ advertiseNext() }, 100)
                }

                override fun onStartFailure(errorCode: Int) {
                    Log.e("BLE_ADVERT", "Error enviando paquete $packetIndex: $errorCode")
                }
            }

            advertiser.startAdvertising(settings, data, advertiseCallback)
        }

        advertiseNext()
    }

    override fun stopAdvertising() {
        val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
        advertiseCallback?.let { advertiser?.stopAdvertising(it) }
        advertiseCallback = null
    }

    private var scanningStarted = false
    private val receivedChunks = mutableMapOf<String, MutableMap<Int, ByteArray>>() // deviceAddress -> index -> chunk

    override fun startScanning(onDeviceFound: (ScanResult, String) -> Unit) {
        if (scanningStarted) return
        scanningStarted = true

        val scanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner ?: return
        val scanFilter = ScanFilter.Builder().build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord?.getServiceData(
                    ParcelUuid(UUID.fromString("e4a2c8aa-62f1-4b9c-8d39-57c8d908b33f"))
                )

                if (serviceData != null) {
                    Log.d("BLE_SCAN", "Paquete recibido de ${result.device.address}: ${serviceData.size} bytes -> ${serviceData.joinToString()}")
                    if (serviceData.size < 2) return

                    val index = serviceData[0].toInt()
                    val total = serviceData[1].toInt()
                    val chunkData = serviceData.copyOfRange(2, serviceData.size)

                    val deviceChunks = receivedChunks.getOrPut(result.device.address) { mutableMapOf() }
                    deviceChunks[index] = chunkData

                    if (deviceChunks.size == total) {
                        val fullData = (0 until total).map { deviceChunks[it]!! }.reduce { acc, bytes -> acc + bytes }
                        Log.d("BLE_SCAN", "Payload completo recibido de ${result.device.address}: ${String(fullData, Charsets.UTF_8)}")
                        onDeviceFound(result, String(fullData, Charsets.UTF_8))
                        receivedChunks.remove(result.device.address)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE_SCAN", "Scan failed: $errorCode")
            }
        }

        scanner.startScan(listOf(scanFilter), settings, scanCallback)
    }

    override fun stopScanning() {
        val scanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
        scanningStarted = false
        Toast.makeText(context, "Escaneo detenido", Toast.LENGTH_SHORT).show()
    }
}

fun truncateName(name: String, maxLength: Int = 8): String {
    if (name.length <= maxLength) return name
    // Tomamos solo los primeros maxLength caracteres
    val sub = name.take(maxLength)
    // Buscamos el último espacio dentro de esa subcadena
    val lastSpace = sub.lastIndexOf(' ')
    return if (lastSpace == -1) {
        // No hay espacio, cortamos al límite
        sub
    } else {
        // Cortamos antes de la palabra que se cortaría
        sub.take(lastSpace).trimEnd()
    }
}