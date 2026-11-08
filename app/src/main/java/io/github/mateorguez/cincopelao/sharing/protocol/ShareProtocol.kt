package io.github.mateorguez.cincopelao.sharing.protocol

/** Constantes del protocolo de compartición. Si cambia el formato de los mensajes, sube [VERSION]. */
object ShareProtocol {
    /** Versión del protocolo. Dos móviles con versiones distintas no se entienden y lo dicen. */
    const val VERSION = 1

    /** "5PLO" en ASCII: los cuatro primeros bytes de cada trama. */
    const val MAGIC = 0x35504C4F

    /** Puerto TCP en el que escucha el receptor (dueño del grupo Wi‑Fi Direct). */
    const val PORT = 8988

    /** Límite para los mensajes de control (Hello, Request, Ack, Error). */
    const val MAX_CONTROL_BYTES = 64 * 1024

    /** Límite para el paquete de datos (JSON + imagen del horario). */
    const val MAX_PAYLOAD_BYTES = 32 * 1024 * 1024

    /** Tamaño de los bloques en los que se escribe y lee el cuerpo (para informar del progreso). */
    const val CHUNK_BYTES = 16 * 1024
}

/** Errores del protocolo: el otro extremo ha mandado algo que no se puede aceptar. */
sealed class ShareProtocolException(message: String) : Exception(message) {
    class NotCincoPelao : ShareProtocolException("El otro dispositivo no habla el protocolo de Cinco Pelao")
    class IncompatibleVersion(val peerVersion: Int) :
        ShareProtocolException("Versión de protocolo incompatible (la otra app usa la v$peerVersion y esta la v${ShareProtocol.VERSION}). Actualizad las dos apps.")
    class UnknownType(val type: Int) : ShareProtocolException("Tipo de mensaje desconocido ($type)")
    class TooLarge(val size: Int, val limit: Int) :
        ShareProtocolException("Mensaje demasiado grande: $size bytes (máximo $limit)")
    class Corrupted(val expectedCrc: Long, val actualCrc: Long) :
        ShareProtocolException("Datos dañados por el camino (CRC32 esperado %08X, calculado %08X)".format(expectedCrc, actualCrc))
    class Truncated : ShareProtocolException("La conexión se cortó a mitad de un mensaje")
    class Malformed(detail: String) : ShareProtocolException("Mensaje mal formado: $detail")
    class Unexpected(expected: String, got: String) : ShareProtocolException("Se esperaba $expected y llegó $got")
    class Rejected(reason: String) : ShareProtocolException(reason)
}
