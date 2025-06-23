package com.magtek

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.magtek.mobile.android.mtusdk.EventType
import com.magtek.mobile.android.mtusdk.IConfigurationCallback
import com.magtek.mobile.android.mtusdk.IData
import com.magtek.mobile.android.mtusdk.IDevice
import com.magtek.mobile.android.mtusdk.IEventSubscriber
import com.magtek.mobile.android.mtusdk.IMQTTDeviceStatusCallback
import com.magtek.mobile.android.mtusdk.IResult
import com.magtek.mobile.android.mtusdk.PaymentMethod
import com.magtek.mobile.android.mtusdk.Result
import com.magtek.mobile.android.mtusdk.StatusCode
import com.magtek.mobile.android.mtusdk.Transaction
import com.magtek.mobile.android.mtusdk.TransactionBuilder
import com.magtek.mobile.android.mtusdk.VASMode
import com.magtek.mobile.android.mtusdk.VASProtocol

const val TAG = "RMTMagtekModule"

@ReactModule(name = RMTMagtekModule.NAME)
class RMTMagtekModule(reactContext: ReactApplicationContext) :
  NativeRMTMagtekSpec(reactContext), IEventSubscriber, IConfigurationCallback,
  IMQTTDeviceStatusCallback {

  var mDevice: IDevice? = null

  private var mTransaction: Transaction? = null

  override fun getName(): String {
    return NAME
  }

  override fun startTransaction(transactionMap: ReadableMap, promise: Promise) {
    try {
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
}
