package com.signaldrivelogger.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Monitors multiple SIM cards (physical + eSIM) and provides signal data for each.
 */
class MultiSimMonitor(private val context: Context) {
    private val subscriptionManager: SubscriptionManager? =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        } else {
            null
        }

    /**
     * Gets all active subscription IDs (SIM cards).
     */
    fun getActiveSubscriptionIds(): List<Int> {
        if (!hasPermission()) return emptyList()

        return try {
            if (subscriptionManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptions = subscriptionManager.activeSubscriptionInfoList
                subscriptions?.mapNotNull { it.subscriptionId } ?: emptyList()
            } else {
                // Fallback: use default subscription (single SIM)
                val defaultSubId = getDefaultSubscriptionId()
                if (defaultSubId >= 0) listOf(defaultSubId) else emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets subscription info for a given subscription ID.
     */
    fun getSubscriptionInfo(subscriptionId: Int): SubscriptionInfo? {
        if (!hasPermission() || subscriptionManager == null) return null

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                subscriptionManager.getActiveSubscriptionInfo(subscriptionId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets TelephonyManager for a specific subscription.
     */
    fun getTelephonyManagerForSubscription(subscriptionId: Int): TelephonyManager? {
        if (!hasPermission()) return null

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.createForSubscriptionId(subscriptionId)
            } else {
                // Fallback to default
                context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets SIM slot index for a subscription.
     */
    fun getSimSlotIndex(subscriptionId: Int): Int {
        val info = getSubscriptionInfo(subscriptionId) ?: return 0
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                info.simSlotIndex
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Checks if SIM is embedded (eSIM).
     */
    fun isEmbeddedSim(subscriptionId: Int): Boolean {
        val info = getSubscriptionInfo(subscriptionId) ?: return false
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                info.isEmbedded
            } else {
                // Heuristic: eSIM typically has slot index >= phone slot count
                val slotIndex = getSimSlotIndex(subscriptionId)
                val phoneCount = getPhoneCount()
                slotIndex >= phoneCount
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets display name for a subscription.
     */
    fun getSimDisplayName(subscriptionId: Int): String {
        val info = getSubscriptionInfo(subscriptionId) ?: return "SIM $subscriptionId"
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}"
            } else {
                "SIM 1"
            }
        } catch (e: Exception) {
            "SIM $subscriptionId"
        }
    }

    /**
     * Gets all signal data updates from all SIMs as a Flow.
     * Each SIM emits its own SignalData with subscription ID.
     */
    fun getAllSimSignalUpdates(): Flow<SimSignalData> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }

        val subscriptionIds = getActiveSubscriptionIds()
        val monitors = mutableListOf<TelephonyMonitorForSubscription>()
        val jobs = mutableListOf<kotlinx.coroutines.Job>()

        // Use a coroutine scope for launching collection jobs
        val scope = CoroutineScope(Dispatchers.Main)

        if (subscriptionIds.isEmpty()) {
            // Fallback: monitor default SIM
            val defaultSubId = getDefaultSubscriptionId()
            if (defaultSubId >= 0) {
                val monitor = TelephonyMonitorForSubscription(context, defaultSubId, this@MultiSimMonitor)
                monitors.add(monitor)
                val job = scope.launch {
                    monitor.getSignalStrengthUpdates().collect { signalData ->
                        trySend(SimSignalData(defaultSubId, signalData))
                    }
                }
                jobs.add(job)
            } else {
                close()
                return@callbackFlow
            }
        } else {
            // Monitor all SIMs
            subscriptionIds.forEach { subId ->
                val monitor = TelephonyMonitorForSubscription(context, subId, this@MultiSimMonitor)
                monitors.add(monitor)

                // Launch collection for each SIM
                val job = scope.launch {
                    monitor.getSignalStrengthUpdates().collect { signalData ->
                        trySend(SimSignalData(subId, signalData))
                    }
                }
                jobs.add(job)
            }
        }

        awaitClose {
            jobs.forEach { it.cancel() }
            monitors.forEach { it.stopMonitoring() }
        }
    }

    private fun getDefaultSubscriptionId(): Int {
        return try {
            if (subscriptionManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                val activeSubs = subscriptionManager.activeSubscriptionInfoList
                activeSubs?.firstOrNull()?.subscriptionId ?: -1
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    private fun getPhoneCount(): Int {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.phoneCount ?: 1
        } catch (e: Exception) {
            1
        }
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Signal data with subscription ID.
 */
data class SimSignalData(
    val subscriptionId: Int,
    val signalData: SignalData
)

/**
 * TelephonyMonitor for a specific subscription.
 */
private class TelephonyMonitorForSubscription(
    private val context: Context,
    val subscriptionId: Int,
    private val multiSimMonitor: MultiSimMonitor
) {
    private val telephonyManager: TelephonyManager? = multiSimMonitor.getTelephonyManagerForSubscription(subscriptionId)

    fun getSignalStrengthUpdates(): Flow<SignalData> = callbackFlow {
        if (telephonyManager == null) {
            close()
            return@callbackFlow
        }

        val handler = android.os.Handler(context.mainLooper)
        var updateRunnable: Runnable? = null

        fun emitSignalData() {
            val signalStrength = getCurrentSignalStrength() ?: return
            val cellId = getCurrentCellId() ?: 0
            val networkType = getCurrentNetworkType()

            val signalData = SignalData(
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

            trySend(signalData)
        }

        // Initial emit
        emitSignalData()

        // Use periodic polling for multi-SIM support (more reliable than PhoneStateListener)
        // PhoneStateListener with subscription ID has API compatibility issues
        updateRunnable = object : Runnable {
            override fun run() {
                emitSignalData()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(updateRunnable)

        awaitClose {
            updateRunnable?.let { handler.removeCallbacks(it) }
        }
    }

    fun stopMonitoring() {
        // Cleanup handled by awaitClose
    }

    private fun getCurrentSignalStrength(): Int? {
        return try {
            val cellInfoList = telephonyManager?.allCellInfo
            cellInfoList?.firstOrNull()?.let { cellInfo ->
                when (cellInfo) {
                    is android.telephony.CellInfoLte -> {
                        val signalStrength = cellInfo.cellSignalStrength as android.telephony.CellSignalStrengthLte
                        signalStrength.rsrp
                    }
                    is android.telephony.CellInfoNr -> {
                        val signalStrength = cellInfo.cellSignalStrength as android.telephony.CellSignalStrengthNr
                        signalStrength.csiRsrp
                    }
                    else -> null
                }
            } ?: run {
                val signalStrength = telephonyManager?.signalStrength
                signalStrength?.let { convertToDbm(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentCellId(): Int? {
        return try {
            val cellLocation = telephonyManager?.cellLocation
            when (cellLocation) {
                is android.telephony.gsm.GsmCellLocation -> cellLocation.cid
                is android.telephony.cdma.CdmaCellLocation -> cellLocation.baseStationId
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentNetworkType(): String {
        return try {
            val networkType = telephonyManager?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
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

    private fun getDataState(): String {
        return try {
            when (telephonyManager?.dataState) {
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

    private fun getDataActivity(): String {
        return try {
            when (telephonyManager?.dataActivity) {
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

    private fun getSimState(): String {
        return try {
            when (telephonyManager?.simState) {
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

    private fun getSimOperatorName(): String {
        return try {
            telephonyManager?.simOperatorName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getSimMcc(): String {
        return try {
            val simOperator = telephonyManager?.simOperator ?: ""
            if (simOperator.length >= 3) simOperator.substring(0, 3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getSimMnc(): String {
        return try {
            val simOperator = telephonyManager?.simOperator ?: ""
            if (simOperator.length > 3) simOperator.substring(3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getOperatorName(): String {
        return try {
            telephonyManager?.networkOperatorName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getMcc(): String {
        return try {
            val networkOperator = telephonyManager?.networkOperator ?: ""
            if (networkOperator.length >= 3) networkOperator.substring(0, 3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getMnc(): String {
        return try {
            val networkOperator = telephonyManager?.networkOperator ?: ""
            if (networkOperator.length > 3) networkOperator.substring(3) else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getPhoneType(): String {
        return try {
            when (telephonyManager?.phoneType) {
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

    private fun convertToDbm(signalStrength: android.telephony.SignalStrength): Int? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                signalStrength.cellSignalStrengths.firstOrNull()?.let {
                    when (it) {
                        is android.telephony.CellSignalStrengthLte -> it.rsrp
                        is android.telephony.CellSignalStrengthNr -> it.csiRsrp
                        else -> null
                    }
                }
            } else {
                val asu = signalStrength.gsmSignalStrength
                if (asu != 99 && asu != -1) {
                    -113 + (2 * asu)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
