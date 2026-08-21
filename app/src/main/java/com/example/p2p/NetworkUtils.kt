package com.example.p2p

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

enum class NetworkType(val displayName: String) {
    WIFI("Wi-Fi Network"),
    HOTSPOT("Personal Hotspot / AP"),
    ETHERNET("Ethernet LAN"),
    CELLULAR("Cellular Hotspot"),
    LOCAL_LOOPBACK("Offline / Loopback")
}

data class NetworkInfoState(
    val ipAddress: String = "127.0.0.1",
    val networkType: NetworkType = NetworkType.LOCAL_LOOPBACK,
    val interfaceName: String = "lo",
    val isHotspotActive: Boolean = false,
    val wifiSsid: String? = null,
    val subnetPrefix: String = "192.168.1.",
    val connectionSummary: String = "Offline / Loopback"
) {
    val isConnected: Boolean get() = ipAddress != "127.0.0.1" && networkType != NetworkType.LOCAL_LOOPBACK
    val localIp: String? get() = if (isConnected) ipAddress else null
    val ssid: String? get() = wifiSsid
}

object NetworkUtils {

    /**
     * Inspects active network interfaces and detects whether the device is connected
     * to a Wi-Fi router, running a Wi-Fi Hotspot / Tethering AP, or on LAN.
     */
    fun getNetworkInfo(context: Context? = null): NetworkInfoState {
        var detectedIp = "127.0.0.1"
        var detectedInterface = "lo"
        var isHotspot = false
        var type = NetworkType.LOCAL_LOOPBACK

        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Prioritize Wi-Fi and Hotspot interfaces: ap0, softap0, wlan0, swlan0, rndis0
            val sortedInterfaces = interfaces.sortedWith(Comparator { a, b ->
                val scoreA = interfacePriorityScore(a.name)
                val scoreB = interfacePriorityScore(b.name)
                scoreB.compareTo(scoreA)
            })

            for (intf in sortedInterfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (!host.startsWith("127.")) {
                            detectedIp = host
                            detectedInterface = intf.name

                            // Detect hotspot interface naming patterns or standard hotspot subnets
                            val nameLower = intf.name.lowercase()
                            isHotspot = nameLower.contains("ap") || 
                                        nameLower.contains("softap") || 
                                        nameLower.contains("swlan") || 
                                        nameLower.contains("hotspot") || 
                                        nameLower.contains("tether") ||
                                        host.startsWith("192.168.43.") ||
                                        host.startsWith("192.168.49.") ||
                                        host.startsWith("192.168.50.")

                            type = when {
                                isHotspot -> NetworkType.HOTSPOT
                                nameLower.contains("wlan") -> NetworkType.WIFI
                                nameLower.contains("eth") -> NetworkType.ETHERNET
                                nameLower.contains("rndis") || nameLower.contains("usb") -> NetworkType.HOTSPOT
                                else -> NetworkType.WIFI
                            }
                            break
                        }
                    }
                }
                if (detectedIp != "127.0.0.1") break
            }
        } catch (_: Exception) {}

        var ssid: String? = null
        if (context != null && type == NetworkType.WIFI) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = wifiManager?.connectionInfo
                val rawSsid = info?.ssid?.replace("\"", "")
                if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                    ssid = rawSsid
                }
            } catch (_: Exception) {}
        }

        val subnet = getSubnetPrefix(detectedIp)
        val summary = when (type) {
            NetworkType.HOTSPOT -> "Personal Hotspot Active ($detectedIp)"
            NetworkType.WIFI -> if (ssid != null) "Connected to $ssid ($detectedIp)" else "Wi-Fi Connected ($detectedIp)"
            NetworkType.ETHERNET -> "Ethernet Connected ($detectedIp)"
            NetworkType.LOCAL_LOOPBACK -> "No active Wi-Fi or Hotspot ($detectedIp)"
            else -> "Local Network ($detectedIp)"
        }

        return NetworkInfoState(
            ipAddress = detectedIp,
            networkType = type,
            interfaceName = detectedInterface,
            isHotspotActive = isHotspot,
            wifiSsid = ssid,
            subnetPrefix = subnet,
            connectionSummary = summary
        )
    }

    private fun interfacePriorityScore(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.startsWith("ap") || lower.startsWith("softap") -> 100
            lower.startsWith("wlan") -> 80
            lower.startsWith("swlan") -> 70
            lower.startsWith("rndis") || lower.startsWith("usb") -> 60
            lower.startsWith("eth") -> 50
            else -> 10
        }
    }

    /**
     * Extracts subnet prefix from IP (e.g. 192.168.43.1 -> 192.168.43.)
     */
    fun getSubnetPrefix(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}."
        } else {
            "192.168.1."
        }
    }

    /**
     * Finds active local IPv4 address (e.g. 192.168.x.x or 10.x.x.x)
     */
    fun getLocalIpAddress(context: Context? = null): String {
        return getNetworkInfo(context).ipAddress
    }

    /**
     * Launches Android Hotspot & Tethering Settings screen
     */
    fun openHotspotSettings(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.settings", "com.android.settings.TetherSettings")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Launches Android Wi-Fi Settings screen
     */
    fun openWifiSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
