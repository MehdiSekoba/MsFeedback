package ir.mehdisekoba.feedback.components

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.TimeZone


class DeviceInfoUtil(private val context: Context) {

    /**
     * Retrieves and formats various device information into a single string.
     *
     * This function gathers details about the device, including its brand, model,
     * SDK version, app version, language, time zone, memory status, device type,
     * and network type. It constructs a string representation of this information,
     * optionally adding a header for better readability when not called from a dialog.
     *
     * @param fromDialog A boolean flag indicating whether this function is being called
     *                   from a dialog or not. If `false`, a header ("SYSTEM-INFO") will
     *                   be prepended to the output string. If `true`, no header is added.
     * @return A string containing the formatted device information. The format includes
     *         key-value pairs for each piece of information.
     *
     * Example output (when fromDialog is false):
     *
     *  ==== SYSTEM-INFO ===
     *
     *  Brand: Google
     *  Device: Pixel 4
     *  SDK Version: 33
     *  App Version: 1.2.3
     *  Language: en-US
     *  TimeZone: America/Los_Angeles
     *  Total Memory: 8 GB
     *  Free Memory: 3 GB
     *  Device Type: Phone
     *  Data Type: WIFI
     *
     * Example output (when fromDialog is true):
     *
     *  Brand: Google
     *  Device: Pixel 4
     *  SDK Version: 33
     *  App Version: 1.2.3
     *  Language: en-US
     *  TimeZone: America/Los_Angeles
     *  Total Memory: 8 GB
     *  Free Memory: 3 GB
     *  Device Type: Phone
     *  Data Type: WIFI
     */
    fun getAllDeviceInfo(fromDialog: Boolean): String {
        val stringBuilder = StringBuilder()

        if (!fromDialog) {
            stringBuilder.append("\n ==== SYSTEM-INFO ===\n")
        }
        val supportedAbis = Build.SUPPORTED_ABIS
        val deviceAbi = supportedAbis.firstOrNull() ?:""
        stringBuilder.append("\n Device: ${getDeviceName()}")
        stringBuilder.append("\n SDK Version: ${getSdkVersion()}")
        stringBuilder.append("\n App Version: ${getAppVersion()}")
        stringBuilder.append("\n Language: ${getLanguage()}")
        stringBuilder.append("\n TimeZone: ${getTimeZone()}")
        stringBuilder.append("\n CPU: $deviceAbi")
        stringBuilder.append("\n Total Memory: ${getTotalMemory()}")
        stringBuilder.append("\n Free Memory: ${getFreeMemory()}")
        stringBuilder.append("\n Device Type: ${getDeviceType()}")
        stringBuilder.append("\n Data Type: ${getNetworkType()}")

        return stringBuilder.toString()
    }

    private fun getDeviceName(): String {
        return try {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            if (model.startsWith(manufacturer, ignoreCase = true)) {
                model.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            } else {
                "${manufacturer.replaceFirstChar { it.titlecase(Locale.getDefault()) }} $model"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }

    private fun getSdkVersion(): String {
        return try {
            "SDK ${Build.VERSION.SDK_INT}"
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }

    private fun getAppVersion(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
                packageInfo.versionName ?: "N/A"
            } else {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "N/A"
            }
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
            "N/A"
        }
    }

    private fun getLanguage(): String {
        return try {
            Locale.getDefault().displayLanguage
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }

    private fun getTimeZone(): String {
        return try {
            TimeZone.getDefault().id
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }

    private fun getTotalMemory(): String {
        return try {
            val path = Environment.getExternalStorageDirectory().path
            val stat = StatFs(path)
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            "%.2f GB".format(Locale.US, bytesTotal.toFloat() / (1024 * 1024 * 1024))
        } catch (_: Exception) {
            try {
                val path = Environment.getDataDirectory().path
                val stat = StatFs(path)
                val bytesTotal = stat.blockSizeLong * stat.blockCountLong
                "%.2f GB".format(Locale.US, bytesTotal.toFloat() / (1024 * 1024 * 1024))
            } catch (e: Exception) {
                e.printStackTrace()
                "N/A"
            }
        }
    }

    private fun getFreeMemory(): String {
        return try {
            val path = Environment.getExternalStorageDirectory().path
            val stat = StatFs(path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            "%.2f GB".format(Locale.US, bytesAvailable.toFloat() / (1024 * 1024 * 1024))
        } catch (_: Exception) {
            try {
                val path = Environment.getDataDirectory().path
                val stat = StatFs(path)
                val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
                "%.2f GB".format(Locale.US, bytesAvailable.toFloat() / (1024 * 1024 * 1024))
            } catch (e: Exception) {
                e.printStackTrace()
                "N/A"
            }
        }
    }

    private fun getDeviceType(): String {
        return try {
            if (context.resources.configuration.screenLayout and
                android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK >=
                android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
            ) {
                "Tablet"
            } else {
                "Phone"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }

    private fun getNetworkType(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Check for ACCESS_NETWORK_STATE permission
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_NETWORK_STATE) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return "Permission Denied (ACCESS_NETWORK_STATE)"
            }

            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            // Return "No Network" if no capabilities are available
            if (capabilities == null) {
                return "No Network"
            }

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    // Check for READ_PHONE_STATE permission for detailed cellular info
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        // Define 3G network types for cleaner when clause
                        val threeGTypes = setOf(
                            TelephonyManager.NETWORK_TYPE_GPRS,
                            TelephonyManager.NETWORK_TYPE_EDGE,
                            TelephonyManager.NETWORK_TYPE_CDMA,
                            TelephonyManager.NETWORK_TYPE_1xRTT,
                            TelephonyManager.NETWORK_TYPE_UMTS,
                            TelephonyManager.NETWORK_TYPE_EVDO_0,
                            TelephonyManager.NETWORK_TYPE_EVDO_A,
                            TelephonyManager.NETWORK_TYPE_HSDPA,
                            TelephonyManager.NETWORK_TYPE_HSUPA,
                            TelephonyManager.NETWORK_TYPE_HSPA,
                            TelephonyManager.NETWORK_TYPE_EVDO_B,
                            TelephonyManager.NETWORK_TYPE_EHRPD,
                            TelephonyManager.NETWORK_TYPE_HSPAP
                        )

                        when (telephonyManager.dataNetworkType) {
                            in threeGTypes -> "Mobile Data 3G"
                            TelephonyManager.NETWORK_TYPE_LTE -> "Mobile Data 4G"
                            TelephonyManager.NETWORK_TYPE_NR -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    "Mobile Data 5G"
                                } else {
                                    "Mobile Data"
                                }
                            }
                            else -> "Mobile Data"
                        }
                    } else {
                        "Mobile Data (Permission Denied: READ_PHONE_STATE)"
                    }
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }
}