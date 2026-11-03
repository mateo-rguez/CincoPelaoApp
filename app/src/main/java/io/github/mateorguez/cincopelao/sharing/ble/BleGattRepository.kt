package io.github.mateorguez.cincopelao.sharing.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*

interface BleGattRepository {
    fun startServer(deviceName: String, passphrase: String, deviceId: String)
    fun stopServer()
    fun startScanning(
        onDataReceived: (deviceName: String, passphrase: String, deviceId: String, macBle: String) -> Unit,
        onDeviceSeen: (deviceId: String) -> Unit,
        onDeviceDisconnected: (deviceId: String) -> Unit
    )
    fun stop()
}

@Singleton
class BleGattRepositoryImpl @Inject constructor(
    private val context: Context
) : BleGattRepository {

    private var bluetoothGattServer: BluetoothGattServer? = null
    private val serviceUuid = UUID.fromString("e4a2c8aa-62f1-4b9c-8d39-57c8d908b33f")
    val charUuidDevice: UUID = UUID.fromString("0000aaaa-0000-1000-8000-00805f9b34fb")
    val charUuidPass: UUID = UUID.fromString("0000aaab-0000-1000-8000-00805f9b34fb")
    val charUuidId: UUID = UUID.fromString("0000aaac-0000-1000-8000-00805f9b34fb")

    override fun startServer(deviceName: String, passphrase: String, deviceId: String) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        val advertiser = adapter.bluetoothLeAdvertiser

        // Crear servicio
        val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // 🔹 Characteristic para SSID
        val deviceNameChar = BluetoothGattCharacteristic(
            charUuidDevice,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        deviceNameChar.value = deviceName.toByteArray(Charsets.UTF_8)

        // 🔹 Characteristic para Passphrase
        val passChar = BluetoothGattCharacteristic(
            charUuidPass,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        passChar.value = passphrase.toByteArray(Charsets.UTF_8)

        // 🔹 Characteristic para DeviceId
        val idChar = BluetoothGattCharacteristic(
            charUuidId,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        idChar.value = deviceId.toByteArray(Charsets.UTF_8)

        // Añadir al servicio
        service.addCharacteristic(deviceNameChar)
        service.addCharacteristic(passChar)
        service.addCharacteristic(idChar)

        bluetoothGattServer = manager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                super.onConnectionStateChange(device, status, newState)
                Log.d("BLE_GATT_SERVER", "Device ${device?.address} newState=$newState")
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    characteristic?.value
                )
            }
        })

        bluetoothGattServer?.addService(service)

        // Advertising pequeño solo con UUID
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .build()

        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d("BLE_GATT_SERVER", "Advertising started")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e("BLE_GATT_SERVER", "Advertising failed: $errorCode")
            }
        })
    }

    override fun stopServer() {
        bluetoothGattServer?.close()
    }

    private var bluetoothGatt: BluetoothGatt? = null

    override fun startScanning(
        onDataReceived: (deviceName: String, passphrase: String, deviceId: String, macBle: String) -> Unit,
        onDeviceSeen: (deviceId: String) -> Unit,
        onDeviceDisconnected: (deviceId: String) -> Unit
    ) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val scanner = manager.adapter.bluetoothLeScanner

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        val connectedDevices = mutableSetOf<String>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val macBle = result.device.address
                Log.d("BLE_SCAN", "Visto dispositivo: $macBle")
                onDeviceSeen(macBle)

                if (connectedDevices.contains(macBle)) {
                    Log.d("BLE_SCAN", "Ya conectado a $macBle, saltando")
                    return
                }
                connectedDevices.add(macBle)

                result.device.connectGatt(context, false, object : BluetoothGattCallback() {
                    var ssid: String? = null
                    var passphrase: String? = null
                    var deviceId: String? = null

                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        Log.d("BLE_GATT", "Device $macBle cambio estado a $newState, status=$status")
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.d("BLE_GATT", "Solicitando MTU grande para $macBle")
                            gatt.requestMtu(512)  // Solicita MTU grande
                            // NO llames a discoverServices aquí
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d("BLE_GATT", "Dispositivo $macBle desconectado")
                            connectedDevices.remove(macBle)
                            onDeviceDisconnected(macBle)
                            gatt.close()
                        }
                    }

                    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d("BLE_GATT", "MTU cambiado a $mtu para $macBle, descubriendo servicios")
                            gatt.discoverServices()  // Ahora sí descubrimos servicios
                        } else {
                            Log.e("BLE_GATT", "Fallo al cambiar MTU: $status, aun así descubrimos servicios")
                            gatt.discoverServices()
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        Log.d("BLE_GATT", "Servicios descubiertos de $macBle, status=$status")
                        val service = gatt.getService(serviceUuid)
                        if (service != null) {
                            service.getCharacteristic(charUuidDevice)?.let {
                                Log.d("BLE_GATT", "Leyendo SSID de $macBle")
                                gatt.readCharacteristic(it)
                            }
                        }
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            when (characteristic.uuid) {
                                charUuidDevice -> {
                                    ssid = String(characteristic.value, Charsets.UTF_8)
                                    Log.d("BLE_GATT", "SSID leído de $macBle: $ssid")
                                    val next = gatt.getService(serviceUuid)?.getCharacteristic(charUuidPass)
                                    if (next != null) {
                                        Log.d("BLE_GATT", "Leyendo passphrase de $macBle")
                                        gatt.readCharacteristic(next)
                                    }
                                }
                                charUuidPass -> {
                                    passphrase = String(characteristic.value, Charsets.UTF_8)
                                    Log.d("BLE_GATT", "Passphrase leído de $macBle: $passphrase")
                                    val next = gatt.getService(serviceUuid)?.getCharacteristic(charUuidId)
                                    if (next != null) {
                                        Log.d("BLE_GATT", "Leyendo deviceId de $macBle")
                                        gatt.readCharacteristic(next)
                                    }
                                }
                                charUuidId -> {
                                    deviceId = String(characteristic.value, Charsets.UTF_8)
                                    Log.d("BLE_GATT", "DeviceId leído de $macBle: $deviceId")
                                    if (ssid != null && passphrase != null && deviceId != null) {
                                        Log.d("BLE_GATT", "Datos completos recibidos de $macBle")
                                        onDataReceived(ssid!!, passphrase!!, deviceId!!, macBle)
                                    }
                                    Log.d("BLE_GATT", "Desconectando $macBle")
                                    gatt.disconnect()
                                }
                            }
                        } else {
                            Log.e("BLE_GATT", "Error leyendo characteristic de $macBle, status=$status")
                        }
                    }
                })
            }
        }

        scanner.startScan(listOf(filter), settings, callback)
        Log.d("BLE_SCAN", "Escaneo iniciado")
    }

    override fun stop() {
        bluetoothGatt?.close()
    }
}
