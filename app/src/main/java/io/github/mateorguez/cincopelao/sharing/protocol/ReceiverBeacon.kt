package io.github.mateorguez.cincopelao.sharing.protocol

import java.security.SecureRandom
import java.util.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * Lo que el receptor publica por BLE (una característica GATT de lectura) para que el emisor
 * pueda unirse a su grupo Wi‑Fi Direct sin buscarlo ni pedir confirmación: nombre de red y
 * contraseña del grupo, además del nombre que eligió el usuario y el puerto del socket.
 */
@Serializable
data class ReceiverBeacon(
    val protocolVersion: Int = ShareProtocol.VERSION,
    val displayName: String,
    val networkName: String,
    val passphrase: String,
    val port: Int = ShareProtocol.PORT
) {
    fun encode(): ByteArray = FrameCodec.json.encodeToString(serializer(), this).toByteArray()

    companion object {
        /** Lo que cabe en una lectura GATT con la MTU máxima (512 bytes de valor). */
        const val MAX_BYTES = 512

        /** Decodifica y valida una baliza leída por BLE. */
        fun decode(bytes: ByteArray): ReceiverBeacon {
            if (bytes.size > MAX_BYTES) throw ShareProtocolException.TooLarge(bytes.size, MAX_BYTES)
            val beacon = try {
                FrameCodec.json.decodeFromString(serializer(), bytes.decodeToString())
            } catch (e: SerializationException) {
                throw ShareProtocolException.Malformed("baliza BLE ilegible")
            } catch (e: IllegalArgumentException) {
                throw ShareProtocolException.Malformed("baliza BLE ilegible")
            }
            if (beacon.protocolVersion != ShareProtocol.VERSION) {
                throw ShareProtocolException.IncompatibleVersion(beacon.protocolVersion)
            }
            if (!GroupCredentials.isValidNetworkName(beacon.networkName) ||
                !GroupCredentials.isValidPassphrase(beacon.passphrase) ||
                beacon.port !in 1..65535
            ) {
                throw ShareProtocolException.Malformed("datos del grupo Wi‑Fi Direct no válidos")
            }
            return beacon
        }
    }
}

/**
 * Nombre de red y contraseña del grupo Wi‑Fi Direct que crea el receptor.
 * Android exige que el nombre empiece por `DIRECT-xy` y una contraseña de 8 a 63 caracteres.
 */
data class GroupCredentials(val networkName: String, val passphrase: String) {
    companion object {
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        private const val PASSPHRASE_LENGTH = 16
        private val NETWORK_NAME = Regex("^DIRECT-[A-Za-z0-9]{2}.{0,23}$")

        fun generate(random: Random = SecureRandom()): GroupCredentials {
            fun randomString(length: Int) = buildString { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
            return GroupCredentials(
                networkName = "DIRECT-${randomString(2)}-CincoPelao",
                passphrase = randomString(PASSPHRASE_LENGTH)
            )
        }

        fun isValidNetworkName(name: String): Boolean =
            NETWORK_NAME.matches(name) && name.toByteArray().size <= 32

        fun isValidPassphrase(passphrase: String): Boolean =
            passphrase.length in 8..63 && passphrase.all { it.code in 0x20..0x7E }
    }
}
