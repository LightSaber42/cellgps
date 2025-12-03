package com.signaldrivelogger.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.telephony.cdma.CdmaCellLocation
import android.telephony.gsm.GsmCellLocation
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors telephony signal strength and cell information.
 * Provides signal data as a Flow.
 */
class TelephonyMonitor(private val context: Context) {
    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val _currentSignalStrength = MutableStateFlow<Int?>(null)
    val currentSignalStrength: StateFlow<Int?> = _currentSignalStrength.asStateFlow()

    private val _currentCellId = MutableStateFlow<Int?>(null)
    val currentCellId: StateFlow<Int?> = _currentCellId.asStateFlow()

    private val _currentNetworkType = MutableStateFlow<String>("Unknown")
    val currentNetworkType: StateFlow<String> = _currentNetworkType.asStateFlow()

    private var signalStrengthCallback: Any? = null // SignalStrengthCallback for API 31+
    private var phoneStateListener: PhoneStateListener? = null

    /**
     * Checks if phone state permission is granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets current signal strength in dBm (RSRP for LTE/5G).
     */
    fun getCurrentSignalStrength(): Int? {
        if (!hasPermission()) return null

        return try {
            val cellInfoList = telephonyManager.allCellInfo
            cellInfoList?.firstOrNull()?.let { cellInfo ->
                when (cellInfo) {
                    is CellInfoLte -> {
                        val signalStrength = cellInfo.cellSignalStrength as CellSignalStrengthLte
                        signalStrength.rsrp
                    }
                    is CellInfoNr -> {
                        val signalStrength = cellInfo.cellSignalStrength as CellSignalStrengthNr
                        signalStrength.csiRsrp
                    }
                    else -> null
                }
            } ?: run {
                // Fallback: try to get from SignalStrength
                val signalStrength = telephonyManager.signalStrength
                signalStrength?.let { convertToDbm(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets current cell ID.
     */
    fun getCurrentCellId(): Int? {
        if (!hasPermission()) return null

        return try {
            val cellLocation = telephonyManager.cellLocation
            when (cellLocation) {
                is GsmCellLocation -> cellLocation.cid
                is CdmaCellLocation -> cellLocation.baseStationId
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets current network type as string (4G, 5G, LTE, etc.).
     */
    fun getCurrentNetworkType(): String {
        if (!hasPermission()) return "Unknown"

        return try {
            val networkType = telephonyManager.dataNetworkType
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Starts monitoring signal strength and emits updates as a Flow.
     */
    fun getSignalStrengthUpdates(): Flow<SignalData> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }

        val handler = android.os.Handler(context.mainLooper)
        var updateRunnable: Runnable? = null

        // Function to emit signal data
        fun emitSignalData() {
            val signalStrength = getCurrentSignalStrength() ?: _currentSignalStrength.value
            val cellId = getCurrentCellId()
            val networkType = getCurrentNetworkType()

            _currentSignalStrength.value = signalStrength
            _currentCellId.value = cellId
            _currentNetworkType.value = networkType

            // Emit to flow if we have valid data with all cell details
            signalStrength?.let {
                trySend(getAllCellDetails())
            }
        }

        // Update initial values
        emitSignalData()

        // Use PhoneStateListener for all Android versions
        // Note: SignalStrengthCallback (API 31+) requires different approach, using PhoneStateListener for compatibility
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                val dbm = convertToDbm(signalStrength)
                _currentSignalStrength.value = dbm
                emitSignalData()
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        // Periodically emit updates
        updateRunnable = object : Runnable {
            override fun run() {
                emitSignalData()
                handler.postDelayed(this, 2000) // Every 2 seconds
            }
        }
        handler.post(updateRunnable)

        awaitClose {
            updateRunnable?.let { handler.removeCallbacks(it) }
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    /**
     * Converts SignalStrength to dBm (approximate).
     */
    private fun convertToDbm(signalStrength: SignalStrength): Int? {
        return try {
            // Try to get RSRP directly if available
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                signalStrength.cellSignalStrengths.firstOrNull()?.let {
                    when (it) {
                        is CellSignalStrengthLte -> it.rsrp
                        is CellSignalStrengthNr -> it.csiRsrp
                        else -> null
                    }
                }
            } else {
                // Fallback: approximate conversion from ASU
                val asu = signalStrength.gsmSignalStrength
                if (asu != 99 && asu != -1) {
                    -113 + (2 * asu) // Approximate conversion
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets data state as string.
     */
    fun getDataState(): String {
        if (!hasPermission()) return "Unknown"
        return try {
            when (telephonyManager.dataState) {
                TelephonyManager.DATA_CONNECTED -> "Connected"
                TelephonyManager.DATA_CONNECTING -> "Connecting"
                TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
                TelephonyManager.DATA_SUSPENDED -> "Suspended"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Gets data activity as string.
     */
    fun getDataActivity(): String {
        if (!hasPermission()) return "None"
        return try {
            when (telephonyManager.dataActivity) {
                TelephonyManager.DATA_ACTIVITY_IN -> "In"
                TelephonyManager.DATA_ACTIVITY_OUT -> "Out"
                TelephonyManager.DATA_ACTIVITY_INOUT -> "InOut"
                TelephonyManager.DATA_ACTIVITY_DORMANT -> "Dormant"
                else -> "None"
            }
        } catch (e: Exception) {
            "None"
        }
    }

    /**
     * Gets SIM state as string.
     */
    fun getSimState(): String {
        if (!hasPermission()) return "Unknown"
        return try {
            when (telephonyManager.simState) {
                TelephonyManager.SIM_STATE_READY -> "Ready"
                TelephonyManager.SIM_STATE_ABSENT -> "Absent"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
                TelephonyManager.SIM_STATE_UNKNOWN -> "Unknown"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Gets SIM operator name.
     */
    fun getSimOperatorName(): String {
        if (!hasPermission()) return ""
        return try {
            telephonyManager.simOperatorName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Gets SIM MCC.
     */
    fun getSimMcc(): String {
        if (!hasPermission()) return ""
        return try {
            val simOperator = telephonyManager.simOperator
            if (simOperator.length >= 3) simOperator.substring(0, 3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Gets SIM MNC.
     */
    fun getSimMnc(): String {
        if (!hasPermission()) return ""
        return try {
            val simOperator = telephonyManager.simOperator
            if (simOperator.length > 3) simOperator.substring(3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Gets network operator name.
     */
    fun getOperatorName(): String {
        if (!hasPermission()) return ""
        return try {
            telephonyManager.networkOperatorName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Gets network MCC.
     */
    fun getMcc(): String {
        if (!hasPermission()) return ""
        return try {
            val networkOperator = telephonyManager.networkOperator
            if (networkOperator.length >= 3) networkOperator.substring(0, 3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Gets network MNC.
     */
    fun getMnc(): String {
        if (!hasPermission()) return ""
        return try {
            val networkOperator = telephonyManager.networkOperator
            if (networkOperator.length > 3) networkOperator.substring(3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Gets phone type as string.
     */
    fun getPhoneType(): String {
        if (!hasPermission()) return "Unknown"
        return try {
            when (telephonyManager.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                TelephonyManager.PHONE_TYPE_NONE -> "None"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Gets all cell details as SignalData.
     */
    fun getAllCellDetails(): SignalData {
        val signalStrength = getCurrentSignalStrength() ?: -100
        val cellId = getCurrentCellId() ?: 0
        val networkType = getCurrentNetworkType()

        return SignalData(
            signalStrength = signalStrength,
            cellId = cellId,
            networkType = networkType,
            dataState = getDataState(),
            dataActivity = getDataActivity(),
            isRoaming = telephonyManager.isNetworkRoaming,
            simState = getSimState(),
            simOperatorName = getSimOperatorName(),
            simMcc = getSimMcc(),
            simMnc = getSimMnc(),
            operatorName = getOperatorName(),
            mcc = getMcc(),
            mnc = getMnc(),
            phoneType = getPhoneType()
        )
    }

    /**
     * Estimates data rate in Kbps (simplified).
     */
    fun estimateDataRate(): Int {
        val networkType = getCurrentNetworkType()
        val signalStrength = _currentSignalStrength.value ?: -100

        return when (networkType) {
            "5G" -> {
                when {
                    signalStrength > -70 -> 50000 // Strong 5G
                    signalStrength > -90 -> 30000 // Medium 5G
                    else -> 10000 // Weak 5G
                }
            }
            "LTE", "4G" -> {
                when {
                    signalStrength > -70 -> 20000 // Strong LTE
                    signalStrength > -90 -> 10000 // Medium LTE
                    else -> 5000 // Weak LTE
                }
            }
            "HSPA+", "HSPA" -> 5000
            "3G" -> 2000
            else -> 1000
        }
    }

    /**
     * Stops monitoring.
     */
    fun stopMonitoring() {
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        signalStrengthCallback = null
        phoneStateListener = null
    }
}

/**
 * Data class for signal information with extended cell details.
 */
data class SignalData(
    val signalStrength: Int, // dBm
    val cellId: Int,
    val networkType: String,
    val dataState: String = "Unknown",
    val dataActivity: String = "None",
    val isRoaming: Boolean = false,
    val simState: String = "Unknown",
    val simOperatorName: String = "",
    val simMcc: String = "",
    val simMnc: String = "",
    val operatorName: String = "",
    val mcc: String = "",
    val mnc: String = "",
    val phoneType: String = "Unknown"
)
