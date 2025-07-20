package com.magtek

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.magtek.mobile.android.mtusdk.ConnectionType
import com.magtek.mobile.android.mtusdk.CoreAPI
import com.magtek.mobile.android.mtusdk.DeviceType
import com.magtek.mobile.android.mtusdk.ErrorType
import com.magtek.mobile.android.mtusdk.EventType
import com.magtek.mobile.android.mtusdk.IData
import com.magtek.mobile.android.mtusdk.IDevice
import com.magtek.mobile.android.mtusdk.IDeviceListCallback
import com.magtek.mobile.android.mtusdk.ISystemStatusCallback
import com.magtek.mobile.android.mtusdk.PaymentMethod
import com.magtek.mobile.android.mtusdk.Transaction
import com.magtek.mobile.android.mtusdk.TransactionBuilder
import com.magtek.mobile.android.mtusdk.VASMode
import com.magtek.mobile.android.mtusdk.VASProtocol
import org.json.JSONArray
import org.json.JSONObject

const val TAG = "RMTMagtekModule"

@ReactModule(name = RMTMagtekModule.NAME)
class RMTMagtekModule(reactContext: ReactApplicationContext) :
  NativeRMTMagtekSpec(reactContext),IDeviceListCallback, ISystemStatusCallback {

  private var mSessionManager: SessionManager? = null

  override fun getName(): String {
    return NAME
  }

  override fun addListener(eventName: String) {
    // Required by TurboModule interface
  }

  private fun sendDeviceListEvent(deviceArray: JSONArray) {
    try {
      val deviceList = Arguments.createArray()

      for (i in 0 until deviceArray.length()) {
        val deviceObject = deviceArray.getJSONObject(i)
        val deviceMap = Arguments.createMap()
        deviceMap.putString("name", deviceObject.getString("name"))
        deviceMap.putString("address", deviceObject.getString("address"))
        deviceList.pushMap(deviceMap)
      }

      reactApplicationContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(DEVICE_LIST_EVENT, deviceList)

      Log.d(TAG, "sendDeviceListEvent: sent ${deviceList.size()} devices")
    } catch (e: Exception) {
      Log.e(TAG, "sendDeviceListEvent: error sending event: ${e.message}")
    }
  }

  init {
    mSessionManager = SessionManager()
    // Set callback for processEvent
    mSessionManager?.mProcessEventCallback = object : ProcessEventCallback {
      override fun invoke(eventType: EventType?, data: IData?) {
        processEvent(eventType, data)
      }
    }
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
      val contact =
        if (transactionMap.hasKey("contact")) transactionMap.getBoolean("contact") else false
      val contactless =
        if (transactionMap.hasKey("contactless")) transactionMap.getBoolean("contactless") else false
      val vas = if (transactionMap.hasKey("vas")) transactionMap.getBoolean("vas") else false
      val gvas = if (transactionMap.hasKey("gvas")) transactionMap.getBoolean("gvas") else false
      val nfc = if (transactionMap.hasKey("nfc")) transactionMap.getBoolean("nfc") else false
      val bcr = if (transactionMap.hasKey("bcr")) transactionMap.getBoolean("bcr") else false
      val cashBack =
        if (transactionMap.hasKey("cashBack")) transactionMap.getString("cashBack") else null
      val quickChip =
        if (transactionMap.hasKey("quickChip")) transactionMap.getBoolean("quickChip") else false
      val emvOnly =
        if (transactionMap.hasKey("emvOnly")) transactionMap.getBoolean("emvOnly") else false
      val nfcReadOnlyMode =
        if (transactionMap.hasKey("nfcReadOnlyMode")) transactionMap.getBoolean("nfcReadOnlyMode") else false
      val signature =
        if (transactionMap.hasKey("signature")) transactionMap.getBoolean("signature") else false
      val fallback =
        if (transactionMap.hasKey("fallback")) transactionMap.getBoolean("fallback") else false
      val showAmount =
        if (transactionMap.hasKey("showAmount")) transactionMap.getBoolean("showAmount") else false
      val showTipOptions =
        if (transactionMap.hasKey("showTipOptions")) transactionMap.getBoolean("showTipOptions") else false
      val showTax =
        if (transactionMap.hasKey("showTax")) transactionMap.getBoolean("showTax") else false

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
        Transaction(
          timeout, paymentMethods,
          amount.toString(), cashBack, quickChip, emvOnly, transactionType
        )

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
    const val DEVICE_LIST_EVENT = "onDeviceListReceived"
    const val DEVICE_CONNECTION_EVENT = "onDeviceConnectionReceived"
  }


  private fun processEvent(eventType: EventType?, data: IData?) {
    try {
      Log.d(TAG, "processEvent: processing eventType=$eventType")
      sendEventToReact(DEVICE_CONNECTION_EVENT, data, eventType?.toString())
    } catch (e: Exception) {
      Log.e(TAG, "processEvent: error processing event: ${e.message}")
    }
  }

  private fun sendEventToReact(eventName: String, data: IData?, additionalData: String? = null) {
    try {
      val eventData = Arguments.createMap()

      if (data != null) {
        eventData.putString("data", data.StringValue())
      }

      if (additionalData != null) {
        eventData.putString("additionalData", additionalData)
      }

      eventData.putString("timestamp", System.currentTimeMillis().toString())

      reactApplicationContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(eventName, eventData)

      Log.d(TAG, "sendEventToReact: sent event $eventName")
    } catch (e: Exception) {
      Log.e(TAG, "sendEventToReact: error sending event $eventName: ${e.message}")
    }
  }

  override fun refreshList() {

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

    Log.d(TAG, "refreshList: found ${deviceList.size} real devices")
    deviceList.forEach { device ->
      Log.d(TAG, "refreshList: real device = ${device.Name()}")
    }

    updateList(deviceList)
  }

  override fun connect(deviceAddress: String, promise: Promise) {
    try {
      Log.d(TAG, "connect: attempting to connect to device: $deviceAddress")

      val jsonObject = JSONObject(deviceAddress)
      val name = jsonObject.getString("name")

      if(mSessionManager == null) {
        mSessionManager = SessionManager()
      }

      val deviceList = mSessionManager!!.getDeviceList()

      if (deviceList != null) {
          for (device in deviceList) {
            if (device.Name() == name) {
              mSessionManager!!.setDevice(device)
              mSessionManager!!.connectDevice()
              break
            }
          }
          Log.d(TAG, "connect: device connection requested for address: $deviceAddress")
          promise.resolve("Device connection requested for: $deviceAddress")
        } else {
          promise.reject("CONNECTION_ERROR", "No devices found")
        }
      } catch (e: Exception) {
        Log.e(TAG, "connect: error connecting to device: ${e.message}")
        promise.reject("CONNECTION_ERROR", e)
      }
  }

  override fun disconnect(promise: Promise) {
    try {
      Log.d(TAG, "disconnect: attempting to disconnect current device")
      if(mSessionManager == null) {
        mSessionManager = SessionManager()
      }
      mSessionManager!!.disconnectDevice()
      Log.d(TAG, "disconnect: device disconnection requested")
      promise.resolve("Device disconnection requested")
    } catch (e: Exception) {
      Log.e(TAG, "disconnect: error disconnecting device: ${e.message}")
      promise.reject("DISCONNECT_ERROR", e)
    }
  }

  override fun resetDevice(promise: Promise) {
    try {
      Log.d(TAG, "resetDevice: attempting to reset current device")
      if(mSessionManager == null) {
        mSessionManager = SessionManager()
      }
      mSessionManager!!.deviceReset()
      Log.d(TAG, "resetDevice: device reset requested")
      promise.resolve("Device reset requested")
    } catch (e: Exception) {
      Log.e(TAG, "resetDevice: error resetting device: ${e.message}")
      promise.reject("RESET_ERROR", e)
    }
  }

  override fun isConnected(promise: Promise?) {
    promise?.resolve(mSessionManager?.isConnected())
  }

  override fun isDisconnected(promise: Promise?) {
    promise?.resolve(mSessionManager?.isDisconnected())
  }

  override fun OnDeviceList(p0: MutableList<IDevice>?) {
    if (p0 != null) {
      Log.d(TAG, "OnDeviceList: found ${p0.size} real devices")
      updateList(p0)
    }
  }

  private fun updateList(p0: List<IDevice?>) {
    val allDevices = addDevices(p0)
    val deviceArray = JSONArray()

    allDevices.forEachIndexed { index, device ->
      try {
        Log.d(TAG, "updateList[$index]: processing device = ${device.Name()}")
        val deviceObject = JSONObject()
        val deviceName = device.Name()
        val address = device.connectionInfo.address



        if (deviceName != null && deviceName.length > 0) {
          deviceObject.put("name", device.Name())
          deviceObject.put("address", address)

        } else {
          deviceObject.put("name", "?")
          deviceObject.put("address", "")

        }
        deviceArray.put(deviceObject)

      } catch (e: Exception) {
        Log.e(TAG, "updateList[$index]: error processing device: ${e.message}")
        // Add a fallback device object
        val fallbackObject = JSONObject()
        fallbackObject.put("name", "Error Device")
        fallbackObject.put("address", "Error: ${e.message}")
        deviceArray.put(fallbackObject)
      }
    }

    Log.d(TAG, "updateList: " + deviceArray.toString())
    mSessionManager?.setDeviceList(allDevices)
    mSessionManager?.startMQTTDeviceStatusMonitoring()

    // Send event instead of resolving promise
    sendDeviceListEvent(deviceArray)
  }

  private fun addDevices(deviceList: List<IDevice?>): MutableList<IDevice> {
    val context: Context = reactApplicationContext.applicationContext
    val mutableDeviceList = mutableListOf<IDevice>()

    Log.d(TAG, "addDevices: input deviceList size = ${deviceList.size}")

    // Add existing devices from the input list
    deviceList.forEach { device ->
      if (device != null) {
        Log.d(TAG, "addDevices: adding real device = ${device.Name()}")
        mutableDeviceList.add(device)
      }
    }


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

    Log.d(TAG, "addDevices: total devices after adding mocks = ${mutableDeviceList.size}")

    return mutableDeviceList
  }

  override fun OnError(p0: ErrorType?, p1: String?) {
//    TODO("Not yet implemented")
  }


}
