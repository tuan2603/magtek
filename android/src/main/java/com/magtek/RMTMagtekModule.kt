package com.magtek

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.magtek.mobile.android.mtusdk.ConnectionType
import com.magtek.mobile.android.mtusdk.CoreAPI
import com.magtek.mobile.android.mtusdk.DeviceType
import com.magtek.mobile.android.mtusdk.ErrorType
import com.magtek.mobile.android.mtusdk.EventType
import com.magtek.mobile.android.mtusdk.IConfigurationCallback
import com.magtek.mobile.android.mtusdk.IData
import com.magtek.mobile.android.mtusdk.IDevice
import com.magtek.mobile.android.mtusdk.IDeviceListCallback
import com.magtek.mobile.android.mtusdk.IEventSubscriber
import com.magtek.mobile.android.mtusdk.IMQTTDeviceStatusCallback
import com.magtek.mobile.android.mtusdk.IResult
import com.magtek.mobile.android.mtusdk.ISystemStatusCallback
import com.magtek.mobile.android.mtusdk.PaymentMethod
import com.magtek.mobile.android.mtusdk.Result
import com.magtek.mobile.android.mtusdk.StatusCode
import com.magtek.mobile.android.mtusdk.Transaction
import com.magtek.mobile.android.mtusdk.TransactionBuilder
import com.magtek.mobile.android.mtusdk.VASMode
import com.magtek.mobile.android.mtusdk.VASProtocol
import org.json.JSONArray
import org.json.JSONObject

const val TAG = "RMTMagtekModule"

@ReactModule(name = RMTMagtekModule.NAME)
class RMTMagtekModule(reactContext: ReactApplicationContext) :
  NativeRMTMagtekSpec(reactContext), IEventSubscriber, IConfigurationCallback,
  IMQTTDeviceStatusCallback, ISystemStatusCallback, IDeviceListCallback {

  private var mSessionManager: SessionManager? = null

  var mDevice: IDevice? = null

  private var mTransaction: Transaction? = null

  private var mRefreshListPromise: Promise? = null

  override fun getName(): String {
    return NAME
  }

  init {
    mSessionManager = SessionManager()
  }

  override fun startTransaction(transactionStr: String, promise: Promise) {
    try {
      // Parse JSON string to ReadableMap
      val transactionMap: ReadableMap = try {
        val jsonObject = JSONObject(transactionStr)
        val readableMap = Arguments.createMap()

        val keys = jsonObject.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          when (val value = jsonObject.get(key)) {
            is Boolean -> readableMap.putBoolean(key, value)
            is Int -> readableMap.putInt(key, value)
            is Long -> readableMap.putDouble(key, value.toDouble())
            is Double -> readableMap.putDouble(key, value)
            is String -> readableMap.putString(key, value)
            else -> readableMap.putString(key, value.toString())
          }
        }
        readableMap
      } catch (e: Exception) {
        promise.reject("INVALID_JSON", "Failed to parse transaction JSON: ${e.message}")
        return
      }

      val amount = if (transactionMap.hasKey("amount")) transactionMap.getDouble("amount") else 0.0
      val msr = if (transactionMap.hasKey("msr")) transactionMap.getBoolean("msr") else false
      val contact = if (transactionMap.hasKey("contact")) transactionMap.getBoolean("contact") else false
      val contactless = if (transactionMap.hasKey("contactless")) transactionMap.getBoolean("contactless") else false
      val vas = if (transactionMap.hasKey("vas")) transactionMap.getBoolean("vas") else false
      val gvas = if (transactionMap.hasKey("gvas")) transactionMap.getBoolean("gvas") else false
      val nfc = if (transactionMap.hasKey("nfc")) transactionMap.getBoolean("nfc") else false
      val bcr = if (transactionMap.hasKey("bcr")) transactionMap.getBoolean("bcr") else false
      val cashBack =
        if (transactionMap.hasKey("cashBack")) transactionMap.getString("cashBack") else null
      val quickChip = if (transactionMap.hasKey("quickChip")) transactionMap.getBoolean("quickChip") else false
      val emvOnly = if (transactionMap.hasKey("emvOnly")) transactionMap.getBoolean("emvOnly") else false
      val nfcReadOnlyMode = if (transactionMap.hasKey("nfcReadOnlyMode")) transactionMap.getBoolean("nfcReadOnlyMode") else false
      val signature = if (transactionMap.hasKey("signature")) transactionMap.getBoolean("signature") else false
      val fallback = if (transactionMap.hasKey("fallback")) transactionMap.getBoolean("fallback") else false
      val showAmount = if (transactionMap.hasKey("showAmount")) transactionMap.getBoolean("showAmount") else false
      val showTipOptions = if (transactionMap.hasKey("showTipOptions")) transactionMap.getBoolean("showTipOptions") else false
      val showTax = if (transactionMap.hasKey("showTax")) transactionMap.getBoolean("showTax") else false

      val basePaymentMethods: List<PaymentMethod> =
        TransactionBuilder.GetPaymentMethods(msr, contact, contactless, false)

      val paymentMethods = basePaymentMethods.toMutableList()

      if (nfc) {
        paymentMethods.add(PaymentMethod.NFC)
      }

      if (vas) {
        paymentMethods.add(PaymentMethod.AppleVAS)
      }

      if (gvas) {
        paymentMethods.add(PaymentMethod.GoogleVAS)
      }

      if (bcr) {
        paymentMethods.add(PaymentMethod.Barcode)
      }

      val timeout: Byte = 45
      val transactionType: Byte = 0 // purchase

      val transaction =
        Transaction(timeout, paymentMethods,
          amount.toString(), cashBack, quickChip, emvOnly, transactionType)

      val currencyCode = byteArrayOf(0x08, 0x40)
      transaction.setCurrencyCode(currencyCode)

      if (contactless && (vas || gvas)) {
        transaction.setAppleVASMode(VASMode.Single)

        transaction.setAppleVASProtocol(VASProtocol.Full)
      } else if (vas || gvas) {
        transaction.setAppleVASMode(VASMode.VASOnly)

        transaction.setAppleVASProtocol(VASProtocol.Full)
      }

      transaction.setDisplayAmountForQuickChip(showAmount)

      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("START_TRANSACTION_ERROR", e)
    }
  }

  companion object {
    const val NAME = "RMTMagtek"
  }

  override fun OnEvent(eventType: EventType?, data: IData?) {
    Log.d(
     TAG,
      "OnEvent: eventType=$eventType"
    )
    try {
      if (data != null) {
        Log.d(
          TAG,
          "OnEvent: data=" + data.StringValue()
        )
      }
    } catch (ex: Exception) {
      ex.message?.let { Log.d(TAG, it) }
    }
  }

  override fun refreshList(promise: Promise?) {
    mRefreshListPromise = promise

    val context: Context = reactApplicationContext.applicationContext

    val toast = Toast.makeText(context, "Scanning...", Toast.LENGTH_LONG)
    toast.show()

    // Set up MQTT Parameters
    val mqttSettings: MQTTSettings? = mSessionManager?.mMQTTSettings

    if (mqttSettings != null) {
      CoreAPI.setMQTTBrokerInfo(mqttSettings.URI, mqttSettings.Username, mqttSettings.Password)
      CoreAPI.setMQTTSubscribeTopic(mqttSettings.SubscribeTopic)
      CoreAPI.setMQTTPublishTopic(mqttSettings.PublishTopic)
      CoreAPI.setMQTTDeviceDiscoveryTimeout(5000)
    }

    CoreAPI.setSystemStatusCallback(this)
    val deviceList: List<IDevice> = CoreAPI.getDeviceList(
      context,
      DeviceType.MMS,
      this
    )

    updateList(deviceList)
  }

  override fun OnProgress(p0: Int) {
//    TODO("Not yet implemented")
  }

  override fun OnResult(p0: StatusCode?, p1: ByteArray?) {
//    TODO("Not yet implemented")
  }

  override fun OnCalculateMAC(p0: Byte, p1: ByteArray?): IResult {
    val result: IResult = Result(StatusCode.UNAVAILABLE)
    return result
  }

  override fun OnConnected(p0: String?) {
//    TODO("Not yet implemented")
  }

  override fun OnDisconnected(p0: String?) {
//    TODO("Not yet implemented")
  }

  override fun OnError(p0: ErrorType?, p1: String?) {
    TODO("Not yet implemented")
  }

  override fun OnDeviceList(p0: MutableList<IDevice>?) {
    if (p0 != null) {
      updateList(p0)
    }
  }

  private fun updateList(p0: List<IDevice?>) {
    val deviceList = addDevices(p0)
    val deviceArray = JSONArray()

    deviceList.forEach { device ->
      Log.d(TAG, "updateList: " + device.Name())
      val deviceObject = JSONObject()
      deviceObject.put("name", device.Name())
      deviceArray.put(deviceObject)
    }

    mSessionManager?.startMQTTDeviceStatusMonitoring()

    // Resolve the promise with the JSON string
    mRefreshListPromise?.resolve(deviceArray.toString())
    mRefreshListPromise = null
  }

  private fun addDevices(deviceList: List<IDevice?>): MutableList<IDevice> {
    val context: Context = reactApplicationContext.applicationContext
    val mutableDeviceList = mutableListOf<IDevice>()

    // Add mock devices
    mutableDeviceList.add(
      CoreAPI.createDevice(
        context,
        DeviceType.MMS,
        ConnectionType.WEBSOCKET,
        "",
        "DynaFlex",
        "WebSocket",
        ""
      )
    )
    mutableDeviceList.add(
      CoreAPI.createDevice(
        context,
        DeviceType.MMS,
        ConnectionType.WEBSOCKET_TRUST,
        "",
        "DynaFlex",
        "WebSocket_Trust",
        ""
      )
    )
    mutableDeviceList.add(
      CoreAPI.createDevice(
        context,
        DeviceType.MMS,
        ConnectionType.SERIAL,
        "Serial",
        "DynaProx",
        "Serial",
        ""
      )
    )

    // Add existing devices from the input list
    deviceList.forEach { device ->
      if (device != null) {
        mutableDeviceList.add(device)
      }
    }

    return mutableDeviceList
  }
}
