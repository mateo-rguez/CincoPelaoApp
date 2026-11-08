package io.github.mateorguez.cincopelao.sharing

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.sharing.protocol.ShareMessage
import io.github.mateorguez.cincopelao.sharing.session.SetupIssue
import io.github.mateorguez.cincopelao.sharing.session.SharePermissions
import io.github.mateorguez.cincopelao.sharing.session.ShareRole
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Estado del móvil para compartir: permisos, radios y quién es. */
@Singleton
class ShareEnvironment @Inject constructor(@ApplicationContext private val context: Context) {

    val sdkInt: Int get() = Build.VERSION.SDK_INT

    fun requiredPermissions(role: ShareRole): List<String> = SharePermissions.required(sdkInt, role)

    fun missingPermissions(role: ShareRole): List<String> = requiredPermissions(role).filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    /** Lo que falta para poder compartir, en el orden en que conviene resolverlo. */
    fun setupIssues(role: ShareRole): List<SetupIssue> = buildList {
        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) || !pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            add(SetupIssue.UNSUPPORTED)
            return@buildList
        }
        if (missingPermissions(role).isNotEmpty()) add(SetupIssue.PERMISSIONS)
        if (context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled != true) add(SetupIssue.BLUETOOTH_OFF)
        if (context.getSystemService(WifiManager::class.java)?.isWifiEnabled != true) add(SetupIssue.WIFI_OFF)
        if (SharePermissions.needsLocationServices(sdkInt) &&
            context.getSystemService(LocationManager::class.java)?.isLocationEnabled != true
        ) {
            add(SetupIssue.LOCATION_OFF)
        }
    }

    /** Presentación de este móvil en el protocolo. El nonce es nuevo en cada sesión. */
    fun hello(): ShareMessage.Hello = ShareMessage.Hello(
        deviceName = deviceName(),
        model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        androidVersion = "${Build.VERSION.RELEASE} (API $sdkInt)",
        appVersion = appVersion(),
        nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    )

    private fun deviceName(): String =
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() }
            ?: Build.MODEL

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"
}
