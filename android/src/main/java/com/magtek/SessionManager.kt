package com.magtek

import android.util.Log
import com.magtek.mobile.android.mtusdk.BaseData
import com.magtek.mobile.android.mtusdk.ConnectionState
import com.magtek.mobile.android.mtusdk.CoreAPI
import com.magtek.mobile.android.mtusdk.DeviceType
import com.magtek.mobile.android.mtusdk.DirectoryEntry
import com.magtek.mobile.android.mtusdk.EnhancedInputRequest
import com.magtek.mobile.android.mtusdk.EventType
import com.magtek.mobile.android.mtusdk.IConfigurationCallback
import com.magtek.mobile.android.mtusdk.IData
import com.magtek.mobile.android.mtusdk.IDevice
import com.magtek.mobile.android.mtusdk.IEventSubscriber
import com.magtek.mobile.android.mtusdk.IMQTTDeviceStatusCallback
import com.magtek.mobile.android.mtusdk.IResult
import com.magtek.mobile.android.mtusdk.InputRequest
import com.magtek.mobile.android.mtusdk.NFCDataBuilder
import com.magtek.mobile.android.mtusdk.NFCEvent
import com.magtek.mobile.android.mtusdk.NFCEventBuilder
import com.magtek.mobile.android.mtusdk.PANRequest
import com.magtek.mobile.android.mtusdk.PINData
import com.magtek.mobile.android.mtusdk.PINDataBuilder
import com.magtek.mobile.android.mtusdk.PINRequest
import com.magtek.mobile.android.mtusdk.Result
import com.magtek.mobile.android.mtusdk.StatusCode
import com.magtek.mobile.android.mtusdk.Transaction
import com.magtek.mobile.android.mtusdk.TransactionBuilder
import com.magtek.mobile.android.mtusdk.TransactionStatus
import com.magtek.mobile.android.mtusdk.TransactionStatusBuilder

open class SessionManager : IEventSubscriber, IConfigurationCallback, IMQTTDeviceStatusCallback {

    companion object {
        private val TAG = "SessionManager"
    }

    // Callback for processEvent
    var mProcessEventCallback: ProcessEventCallback? = null

    private var mDevice: IDevice? = null

    private var mDeviceList: List<IDevice>? = null

    private var mTransaction: Transaction? = null
    protected var mGetFileName: String = ""
    private var mPANRequest: PANRequest? = null
    protected var mGetSignatureFromDevice: Boolean = false
    protected var mNonDisplayDevice: Boolean = false

    protected enum class FileTransferMode { NONE, SEND_IMAGE, SEND_FILE, GET_FILE, UPDATE_FIRMWARE }
    protected var mFileTransferMode: FileTransferMode = FileTransferMode.NONE

    private enum class NFCState {
        NONE, ENABLED, TAG_DETECTED, GET_VERSION, FAST_READ, READY, WRITE,
        CLASSIC_1K_DETECTED, CLASSIC_4K_DETECTED, CLASSIC_1K_READ, CLASSIC_4K_READ,
        CLASSIC_1K_READY, CLASSIC_4K_READY, CLASSIC_1K_WRITE, CLASSIC_4K_WRITE,
        DESFIRE_DETECTED, DESFIRE_GET_VERSION_P1, DESFIRE_GET_VERSION_P2, DESFIRE_GET_VERSION_P3,
        DESFIRE_SELECT, DESFIRE_READ_DATA, DESFIRE_AUTHENTICATE_P1, DESFIRE_AUTHENTICATE_P2, DESFIRE_GET_VALUE
    }
    private var mNFCState: NFCState = NFCState.NONE
    private var mNDEFBytes: ByteArray? = null
    private var mNDEFBlock: Int = 0
    private var mNFCReadOnlyMode: Boolean = true
    private var mReadSector: Int = 0
    private var mWriteSector: Int = 0
    private var mClassicNFCData: MutableList<String>? = null
    private val CLASSIC_KEY_A = "FFFFFFFFFFFF"
    private val CLASSIC_KEY_B = "FFFFFFFFFFFF"
    private val ClassicKeyA = Array(40) { CLASSIC_KEY_A }
    private val ClassicKeyB = Array(40) { CLASSIC_KEY_B }

    private var mNDEFRecords: List<MTNdefRecord>? = null

    var mMQTTSettings: MQTTSettings = MQTTSettings()

    private var mUIStringList: Array<String> = DisplayStrings.StringList


    private fun displayUserSelections(title: String, selectionType: Int, selectionList: List<String>, timeout: Long) {
        val nSelections = selectionList.size

        if (nSelections > 0) {
            // TODO: Implement the rest of the function logic
        }
    }

    private fun displayEnhancedUserSelections(title: String, selectionType: Int, enhancedSelectionList: List<DirectoryEntry>, timeout: Long) {
        val nSelections = enhancedSelectionList.size

        if (nSelections > 0) {
            val selectionArray = Array(enhancedSelectionList.size + 1) { "" }
            // TODO: Implement the rest of the function logic
        }
    }

  fun startMQTTDeviceStatusMonitoring() {
    CoreAPI.startMQTTDeviceStatusMonitoring(this)
  }

  fun stopMQTTDeviceStatusMonitoring() {
    CoreAPI.stopMQTTDeviceStatusMonitoring()
  }

  fun setDeviceList(deviceList: List<IDevice>) {
    mDeviceList = deviceList
  }

  fun getDeviceList(): List<IDevice>? {
    return mDeviceList
  }

  fun setDevice(device: IDevice) {
    mDevice = device
  }

  fun getDevice(): IDevice? {
    return mDevice
  }

   private fun processInputRequest(data: ByteArray?) {
    val inputRequest = InputRequest(data)
    displayUserSelections(
      inputRequest.Title(),
      inputRequest.Type().toInt(),
      inputRequest.SelectionList(),
      inputRequest.Timeout().toLong()
    )
  }




   private fun processEnhancedInputRequest(data: ByteArray?) {
    val enhancedInputRequest = EnhancedInputRequest(data)

    displayEnhancedUserSelections(
      enhancedInputRequest.Title(),
      enhancedInputRequest.Type().toInt(),
      enhancedInputRequest.EnhancedSelectionList(),
      enhancedInputRequest.Timeout().toLong()
    )
  }

  private fun processEventPINData(data: ByteArray?) {
    val pinData = PINDataBuilder.GetPINData(
      mDevice?.connectionInfo?.deviceType ?: DeviceType.CMF,
      data
    )
    if (pinData != null) {
      if (mPANRequest != null) {
        processPINDataForPANRequest(pinData)
        mPANRequest = null
      } else {
        processPINData(pinData)
      }
    }
  }

  private fun processPINData(pinData: PINData) {
    val validPIN = true // PIN Validation Result from Payment Gateway

    val device: IDevice = mDevice!!

    if (device.connectionInfo.deviceType == DeviceType.MMS) {
      val pinRequest = PINRequest()

      val pinMode =
        if (validPIN) 0xFF.toByte() else 0xFE.toByte() // (PIN Entry Successful) : (PIN Entry SuccessfulFailed)
      pinRequest.setPINMode(pinMode)
      pinRequest.setFormat(0.toByte())

      if (!device.requestPIN(pinRequest)) {
        //setTransactionStatus(false);
        //clearDisplay();
        //sendToDisplay("\n\nREQUEST PIN NOT SUPPORTED");
      }
    }
  }


  private fun processPINDataForPANRequest(pinData: PINData) {
    val validPIN = true // PIN Validation Result from Payment Gateway

    val device: IDevice = mDevice!!

    if (device.connectionInfo.deviceType == DeviceType.MMS) {
      val panRequest =
        PANRequest(60.toByte(), TransactionBuilder.GetPaymentMethods(false, false, false, false))

      val pinRequest = PINRequest()

      val pinMode =
        if (validPIN) 0xFF.toByte() else 0xFE.toByte() // (PIN Entry Successful) : (PIN Entry SuccessfulFailed)
      pinRequest.setPINMode(pinMode)
      pinRequest.setFormat(0.toByte())

      if (!device.requestPAN(panRequest, pinRequest)) {
        //setTransactionStatus(false);
        //clearDisplay();
        //sendToDisplay("\n\nREQUEST PAN NOT SUPPORTED");
      }
    }
  }

  private fun sendToOutput(message: String){
    Log.d(TAG, message)
  }

  private fun processNFCResponse(data: IData) {
    if (mNFCState == NFCState.GET_VERSION) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcData = NFCDataBuilder.GetNFCData(deviceType, data.ByteArray())
      if (nfcData != null) {
        if (!nfcData.Encrypted()) {
          val responseString = MTParser.getHexString(nfcData.Data())
          sendToOutput("[NFCResponse: GetVersion=$responseString]")

          if (responseString.equals("0004040201000F03", ignoreCase = true)) {
            sendToOutput("[NTAG213]")
            val fastReadNTAG213 =
              "3A0427" // NTAG213 : 180 bytes / 45 pages (Read user memory page 4-39)
            mNFCState = NFCState.FAST_READ
            mDevice!!.sendNFCCommand(BaseData(fastReadNTAG213), mNFCReadOnlyMode, false)
          } else if (responseString.equals("0004040201001103", ignoreCase = true)) {
            sendToOutput("[NTAG215]")
            val fastReadNTAG215 =
              "3A0481" // NTAG215 : 540 bytes / 135 pages (Read user memory page 4-129)
            mNFCState = NFCState.FAST_READ
            mDevice!!.sendNFCCommand(BaseData(fastReadNTAG215), mNFCReadOnlyMode, false)
          } else if (responseString.equals("0004040201001303", ignoreCase = true)) {
            sendToOutput("[NTAG216]")
            val fastReadNTAG216 =
              "3A04E1" // NTAG216 : 924 bytes / 231 pages (Read user memory page 4-225)
            mNFCState = NFCState.FAST_READ
            mDevice!!.sendNFCCommand(BaseData(fastReadNTAG216), mNFCReadOnlyMode, false)
          }
        } else {
          sendToOutput("NFC Data=" + MTParser.getHexString(nfcData.Data()))
          sendToOutput("Encryption Type=" + nfcData.EncryptionType())
          sendToOutput("KSN=" + MTParser.getHexString(nfcData.KSN()))
          mNFCState = NFCState.READY
        }
      }
    } else if (mNFCState == NFCState.FAST_READ) {
      mNFCState = NFCState.READY
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcData = NFCDataBuilder.GetNFCData(deviceType, data.ByteArray())

      if ((nfcData != null) && (!nfcData.Encrypted())) {
        val responseString = MTParser.getHexString(nfcData.Data())
        sendToOutput("[NFCResponse: FastRead=$responseString]")

        val ndefMessages: List<String> = MTNdef.getNDEFMessages(responseString)

        for (i in ndefMessages.indices) {
          val msgString = ndefMessages[i]

          val msgBytes = MTParser.getByteArrayFromHexString(msgString)

          if (msgBytes != null) {
            try {
              mNDEFRecords = MTNdef.Parse(msgBytes)

              for (j in mNDEFRecords!!.indices) {
                val record: MTNdefRecord = mNDEFRecords!!.get(j)

                if (record.isWellKnownType()) {
                  if (record.isUri()) {
                    sendToOutput("Well Known Type RTD URI: " + record.getUriString())
                  } else if (record.isText()) {
                    sendToOutput("Well Known Type RTD Text: " + record.getTextString())
                  }
                } else if (record.isExternalType()) {
                  if (record.Type != null) {
                    sendToOutput("External Type Name: " + String(record.Type!!))

                    if (record.Payload != null) {
                      sendToOutput("External Type Payload: " + MTParser.getHexString(record.Payload))
                    }
                  }
                }
              }
            } catch (ex: java.lang.Exception) {
              sendToOutput("Exception: " + ex.message)
            }
          }
        }
      }
    } else if (mNFCState == NFCState.WRITE) {
      if (writeNDEFBlock(mNDEFBlock++) == false) {
        sendToOutput("[NFC writeNDEFMessage done]")
        mNFCState = NFCState.READY
      }
    } else if (mNFCState == NFCState.CLASSIC_1K_READ) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcData = NFCDataBuilder.GetNFCData(deviceType, data.ByteArray())

      if ((nfcData != null) && (!nfcData.Encrypted())) {
        val responseString = MTParser.getHexString(nfcData.Data())
        sendToOutput("[NFCResponse: Classic 1K Read Sector $mReadSector=$responseString]")

        var dataString = responseString

        if (responseString.length == 128) {
          val keyA = responseString.substring(96, 108)
          if (keyA.equals("000000000000", ignoreCase = true)) {
            dataString =
              responseString.substring(0, 96) + ClassicKeyA[mReadSector] + responseString.substring(
                108,
                128
              )
            sendToOutput("[Actual Sector ($mReadSector) Data=$dataString]")
          }
        }

        mClassicNFCData!!.add(dataString)
      }

      mReadSector++

      if (mReadSector <= 15) {
        var read1K =
          "30" + MTParser.getHexString(byteArrayOf(mReadSector.toByte())) + "000300" + ClassicKeyA[mReadSector]

        if (mReadSector == 5) {
          read1K =
            "30" + MTParser.getHexString(byteArrayOf(mReadSector.toByte())) + "000301" + ClassicKeyB[mReadSector]
        }

        sendToOutput("[NFCResponse: Classic 1K Read Sector ($mReadSector) Command=$read1K]")

        if (mReadSector == 15) mDevice!!.sendClassicNFCCommand(
          BaseData(read1K),
          mNFCReadOnlyMode,
          false
        )
        else mDevice!!.sendClassicNFCCommand(BaseData(read1K), false, false)
      } else {
        mNFCState = NFCState.CLASSIC_1K_READY
      }
    } else if (mNFCState == NFCState.CLASSIC_4K_READ) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcData = NFCDataBuilder.GetNFCData(deviceType, data.ByteArray())

      if ((nfcData != null) && (!nfcData.Encrypted())) {
        val responseString = MTParser.getHexString(nfcData.Data())
        sendToOutput("[NFCResponse: Classic 4K Read Sector $mReadSector=$responseString]")

        var dataString = responseString

        if (responseString.length == 128) {
          val keyA = responseString.substring(96, 108)
          if (keyA.equals("000000000000", ignoreCase = true)) {
            dataString =
              responseString.substring(0, 96) + ClassicKeyA[mReadSector] + responseString.substring(
                108,
                128
              )
            sendToOutput("[Actual Sector ($mReadSector) Data=$dataString]")
          }
        } else if (responseString.length == 512) {
          val keyA = responseString.substring(480, 492)
          if (keyA.equals("000000000000", ignoreCase = true)) {
            dataString = responseString.substring(
              0,
              480
            ) + ClassicKeyA[mReadSector] + responseString.substring(492, 512)
            sendToOutput("[Actual Sector ($mReadSector) Data=$dataString]")
          }
        }

        mClassicNFCData!!.add(dataString)
      }

      mReadSector++

      if (mReadSector <= 31) {
        val read4K =
          "30" + MTParser.getHexString(byteArrayOf(mReadSector.toByte())) + "000300" + CLASSIC_KEY_A // block 0-3

        sendToOutput("[NFCResponse: Classic 4K Read Sector ($mReadSector) Command=$read4K]")

        mDevice!!.sendClassicNFCCommand(BaseData(read4K), false, false)
      } else if (mReadSector <= 39) {
        val read4K =
          "30" + MTParser.getHexString(byteArrayOf(mReadSector.toByte())) + "000F00" + CLASSIC_KEY_A // blocks 0-15

        sendToOutput("[NFCResponse: Classic 4K Read Sector ($mReadSector) Command=$read4K")

        if (mReadSector == 39) mDevice!!.sendClassicNFCCommand(
          BaseData(read4K),
          mNFCReadOnlyMode,
          false
        )
        else mDevice!!.sendClassicNFCCommand(BaseData(read4K), false, false)
      } else {
        mNFCState = NFCState.CLASSIC_4K_READY
      }
    } else if (mNFCState == NFCState.CLASSIC_1K_WRITE) {
      sendToOutput("[NFC CLASSIC_1K_WRITE Sector ($mWriteSector) done]")
      mNFCState = NFCState.READY
    } else if (mNFCState == NFCState.CLASSIC_4K_WRITE) {
      sendToOutput("[NFC CLASSIC_4K_WRITE Sector ($mWriteSector) done]")
      mNFCState = NFCState.READY
    }
  }


  private fun writeNDEFBlock(block: Int): Boolean {
    if (mNDEFBytes != null) {
      val len = mNDEFBytes!!.size

      var i = block * 4

      sendToOutput("[NFC writeNDEFBlock i=$block Len=$len]")

      if ((i) < len) {
        val data = ByteArray(6)

        data[0] = 0xA2.toByte()

        val page = block + 4
        data[1] = (page).toByte()

        if (i < len) data[2] = mNDEFBytes!![i++]
        if (i < len) data[3] = mNDEFBytes!![i++]
        if (i < len) data[4] = mNDEFBytes!![i++]
        if (i < len) data[5] = mNDEFBytes!![i++]

        val dataString = MTParser.getHexString(data)

        sendToOutput("[NFC Write Page=$page Data=$dataString]")

        mNFCState = NFCState.WRITE

        var lastCommand = false

        if (i >= len) lastCommand = true

        mDevice!!.sendNFCCommand(BaseData(dataString), lastCommand, false)

        return true
      }
    }

    return false
  }

  fun deviceReset() {
    try {
      mDevice?.deviceControl?.deviceReset()
    } catch (ex: Exception) {
      Log.d(TAG, "deviceReset: ${ex.message}")
    }
  }

  private fun subscribeAll() {
    val device: IDevice? = mDevice
    if (device != null) {
      device.unsubscribeAll(this)
      device.subscribeAll(this)
    }
  }

  fun connectDevice() {
    try {
      subscribeAll()
      mDevice?.deviceControl?.open()
    } catch (ex: Exception) {
      Log.d(TAG, "connectDevice: ${ex.message}")
    }
  }

  fun disconnectDevice() {
    try {
      mDevice?.deviceControl?.close()
    } catch (ex: Exception) {
      Log.d(TAG, "disconnectDevice: ${ex.message}")
    }
  }


  override fun OnEvent(eventType: EventType?, data: IData?) {
    Log.d(TAG, "OnEvent: eventType=$eventType")
    try {
      if (data != null) {
        Log.d(TAG, "OnEvent: data=" + data.StringValue())
      }
    } catch (ex: Exception) {
      println(ex.message)
    }

    try {
        // Call processEvent callback if available
        mProcessEventCallback?.invoke(eventType, data)
        when (eventType) {
        EventType.TransactionStatus -> {
          val status = TransactionStatusBuilder.GetStatusCode(data!!.StringValue())
         if (status == TransactionStatus.TransactionStartedFromDevice) {
            sendToOutput("[TRANSACTION STARTED FROM DEVICE")
            mTransaction = Transaction()
            mTransaction!!.setQuickChip(false)
          } else if (status == TransactionStatus.TransactionStartedFromDeviceQuickChip) {
            sendToOutput("[TRANSACTION STARTED FROM DEVICE (QUICK_CHIP)]")
            mTransaction = Transaction()
            mTransaction!!.setQuickChip(true)
          } else if (status == TransactionStatus.TransactionCancelledFromDevice) {
            sendToOutput("[TRANSACTION CANCELLED FROM DEVICE]")
          }
        }

        EventType.InputRequest -> {
          sendToOutput("[InputRequest]\n")
          processInputRequest(data!!.ByteArray())
        }

        EventType.EnhancedInputRequest -> {
          sendToOutput("[EnhancedInputRequest]\n")
          processEnhancedInputRequest(data!!.ByteArray())
        }

        EventType.PINData -> {
          //pin data
          processEventPINData(data!!.ByteArray())
        }

        EventType.NFCEvent -> {
          val nfcEvent = NFCEventBuilder.GetEventValue(data!!.StringValue())
          if (nfcEvent == NFCEvent.TagRemoved) {
            mNFCState = NFCState.NONE
            sendToOutput("[NFCEvent: Tag Removed]")
          } else if (nfcEvent == NFCEvent.NFCMifareUltralight) {
            mNFCState = NFCState.TAG_DETECTED
            sendToOutput("[NFCEvent: NFC/Mifare Ultralight]")
          } else if (nfcEvent == NFCEvent.MifareClassic1K) {
            mNFCState = NFCState.CLASSIC_1K_DETECTED
            sendToOutput("[NFCEvent: NFCMifare 1K Classic]")
          } else if (nfcEvent == NFCEvent.MifareClassic4K) {
            mNFCState = NFCState.CLASSIC_4K_DETECTED
            sendToOutput("[NFCEvent: NFCMifare 4K Classic]")
          } else if (nfcEvent == NFCEvent.MifareDESFire) {
            mNFCState = NFCState.DESFIRE_DETECTED
            sendToOutput("[NFCEvent: NFC/Mifare DESFire]")
          } else if (nfcEvent == NFCEvent.Failed) {
            mNFCState = NFCState.READY
            sendToOutput("[NFCEvent: Failed]")
          } else if (nfcEvent == NFCEvent.IOFailed) {
            mNFCState = NFCState.READY
            sendToOutput("[NFCEvent: I/O Failed]")
          } else if (nfcEvent == NFCEvent.AuthenticationFailed) {
            val detail = NFCEventBuilder.GetDetail(data!!.StringValue())

            mNFCState = NFCState.READY
            sendToOutput("[NFCEvent: Authentication Failed, Block(0x$detail) ]")
          }
        }

        EventType.NFCData -> data?.let { processNFCData(it) }
        EventType.NFCResponse -> data?.let { processNFCResponse(it) }
        EventType.NFCAPDUResponse -> data?.let { processNFCAPDUResponse(it) }
        EventType.ConnectionState -> {
          sendToOutput("[CONNECTION STATE]")
        }
        EventType.DeviceResponse -> {
          sendToOutput("[DEVICE RESPONSE]")
        }
        EventType.DeviceExtendedResponse -> {
          sendToOutput("[DEVICE EXTENDED RESPONSE]")
        }
        EventType.DeviceNotification -> {
          sendToOutput("[DEVICE NOTIFICATION]")
        }
        EventType.CardData -> {
          sendToOutput("[CARD DATA]")
        }
        EventType.DisplayMessage -> {
          sendToOutput("[DISPLAY MESSAGE]")
        }
        EventType.ClearDisplay -> {
          sendToOutput("[CLEAR DISPLAY]")
        }
        EventType.AuthorizationRequest -> {
          sendToOutput("[AUTHORIZATION REQUEST]")
        }
        EventType.TransactionResult -> {
          sendToOutput("[TRANSACTION RESULT]")
        }
        EventType.PINBlock -> {
          sendToOutput("[PIN BLOCK]")
        }
        EventType.Signature -> {
          sendToOutput("[SIGNATURE]")
        }
        EventType.DeviceDataFile -> {
          sendToOutput("[DEVICE DATA FILE]")
        }
        EventType.DeviceEvent -> {
          sendToOutput("[DEVICE EVENT]")
        }
        EventType.UserEvent -> {
          sendToOutput("[USER EVENT]")
        }
        EventType.FeatureStatus -> {
          sendToOutput("[FEATURE STATUS]")
        }
        EventType.BarCodeData -> {
          sendToOutput("[BAR CODE DATA]")
        }
        EventType.TouchscreenSignatureCapture -> {
          sendToOutput("[TOUCHSCREEN SIGNATURE CAPTURE]")
        }
        EventType.TouchscreenFunctionalButtonSelected -> {
          sendToOutput("[TOUCHSCREEN FUNCTIONAL BUTTON SELECTED]")
        }
        EventType.TouchscreenAmountButtonSelected -> {
          sendToOutput("[TOUCHSCREEN AMOUNT BUTTON SELECTED]")
        }
        EventType.TouchscreenPresentCardFunctionalButtonSelected -> {
          sendToOutput("[TOUCHSCREEN PRESENT CARD FUNCTIONAL BUTTON SELECTED]")
        }
        EventType.OperationStatus -> {
          sendToOutput("[OPERATION STATUS]")
        }
        EventType.PANData -> {
          sendToOutput("[PAN DATA]")
        }
        EventType.TouchscreenTextStringButtonSelected -> {
          sendToOutput("[TOUCHSCREEN TEXT STRING BUTTON SELECTED]")
        }
        null -> {
          sendToOutput("[NULL]")
        }

      }
    } catch (ex: Exception) {
      println(ex.message)
    }
  }

  private fun processNFCAPDUResponse(data: IData) {
    if (mNFCState == NFCState.DESFIRE_GET_VERSION_P1) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcRAPDUData = NFCDataBuilder.GetNFCRAPDUData(deviceType, data.ByteArray())

      if (nfcRAPDUData != null) {
        if (nfcRAPDUData.Response() != null) {
          val responseString = MTParser.getHexString(nfcRAPDUData.Response())
          sendToOutput("[NFCAPDUResponse: Get Version Part 1 Response=$responseString]")
        }

        if (!nfcRAPDUData.Encrypted()) {
          val dataString = MTParser.getHexString(nfcRAPDUData.Data())
          sendToOutput("[NFCAPDUResponse: Get Version Part 1 Data=$dataString]")
        }
      }

      sendToOutput("[Mifare DESFire - Get Version P2]")
      mNFCState = NFCState.DESFIRE_GET_VERSION_P2
      mDevice!!.sendDESFireNFCCommand(BaseData("90AF000000"), false, false)
    } else if (mNFCState == NFCState.DESFIRE_GET_VERSION_P2) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcRAPDUData = NFCDataBuilder.GetNFCRAPDUData(deviceType, data.ByteArray())

      if (nfcRAPDUData != null) {
        if (nfcRAPDUData.Response() != null) {
          val responseString = MTParser.getHexString(nfcRAPDUData.Response())
          sendToOutput("[NFCAPDUResponse: Get Version Part 2 Response=$responseString]")
        }

        if (!nfcRAPDUData.Encrypted()) {
          val dataString = MTParser.getHexString(nfcRAPDUData.Data())
          sendToOutput("[NFCAPDUResponse: Get Version Part 2 Data=$dataString]")
        }
      }

      sendToOutput("[Mifare DESFire - Get Version P3]")
      mNFCState = NFCState.DESFIRE_GET_VERSION_P3
      mDevice!!.sendDESFireNFCCommand(BaseData("90AF000000"), false, false)
    } else if (mNFCState == NFCState.DESFIRE_GET_VERSION_P3) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcRAPDUData = NFCDataBuilder.GetNFCRAPDUData(deviceType, data.ByteArray())

      if (nfcRAPDUData != null) {
        if (nfcRAPDUData.Response() != null) {
          val responseString = MTParser.getHexString(nfcRAPDUData.Response())
          sendToOutput("[NFCAPDUResponse: Get Version Part 3 Response=$responseString]")
        }

        if (!nfcRAPDUData.Encrypted()) {
          val dataString = MTParser.getHexString(nfcRAPDUData.Data())
          sendToOutput("[NFCAPDUResponse: Get Version Part 3 Data=$dataString]")
        }
      }

      sendToOutput("[Mifare DESFire - Select]")
      mNFCState = NFCState.DESFIRE_SELECT
      //mDevice.sendDESFireNFCCommand(new BaseData("00A40000023F0000"), false, false);  // MF
      mDevice!!.sendDESFireNFCCommand(BaseData("00A4000C02DF0100"), false, false) // DF
      //mDevice.sendDESFireNFCCommand(new BaseData("00A4040007D276000085010100"), false, false); // NDEF
    } else if (mNFCState == NFCState.DESFIRE_SELECT) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcRAPDUData = NFCDataBuilder.GetNFCRAPDUData(deviceType, data.ByteArray())

      if (nfcRAPDUData != null) {
        if (nfcRAPDUData.Response() != null) {
          val responseString = MTParser.getHexString(nfcRAPDUData.Response())
          sendToOutput("[NFCAPDUResponse: Select File Response=$responseString]")
        }

        if (!nfcRAPDUData.Encrypted()) {
          val dataString = MTParser.getHexString(nfcRAPDUData.Data())
          sendToOutput("[NFCAPDUResponse: Select File Data=$dataString]")
        }
      }

      sendToOutput("[Mifare DESFire - Read Data File 1F]")
      mNFCState = NFCState.DESFIRE_READ_DATA
      mDevice!!.sendDESFireNFCCommand(
        BaseData("90AD0000071F00000000000000"),
        mNFCReadOnlyMode,
        false
      )
    } else if (mNFCState == NFCState.DESFIRE_READ_DATA) {
      val deviceType = mDevice!!.connectionInfo.deviceType
      val nfcRAPDUData = NFCDataBuilder.GetNFCRAPDUData(deviceType, data.ByteArray())

      if (nfcRAPDUData != null) {
        if (nfcRAPDUData.Response() != null) {
          val responseString = MTParser.getHexString(nfcRAPDUData.Response())
          sendToOutput("[NFCAPDUResponse: Read Data 1F Response=$responseString]")
        }

        if (!nfcRAPDUData.Encrypted()) {
          val dataString = MTParser.getHexString(nfcRAPDUData.Data())
          sendToOutput("[NFCAPDUResponse: Read Data 1F Data=$dataString]")
        }
      }

      mNFCState = NFCState.NONE
    }
  }

  private fun processNFCData(data: IData) {
    sendToOutput("[NFCData: UID=" + data.StringValue() + "]")

    if (mNFCState == NFCState.TAG_DETECTED) {
      val getVersion = "60"
      mNFCState = NFCState.GET_VERSION
      mDevice!!.sendNFCCommand(BaseData(getVersion), false, false)
    } else if (mNFCState == NFCState.CLASSIC_1K_DETECTED) {
      mClassicNFCData = ArrayList()
      mReadSector = 0

      val read1K =
        "30" + MTParser.getHexString(byteArrayOf(mReadSector.toByte())) + "000300" + ClassicKeyA[mReadSector]

      sendToOutput("[NFCResponse: Classic 1K Read Sector (0) Command=$read1K]")

      mNFCState = NFCState.CLASSIC_1K_READ
      mDevice!!.sendClassicNFCCommand(BaseData(read1K), false, false)
    } else if (mNFCState == NFCState.CLASSIC_4K_DETECTED) {
      mClassicNFCData = ArrayList()
      mReadSector = 0

      val read4K =
        "30" + MTParser.getHexString(byteArrayOf(mReadSector.toByte())) + "000300" + ClassicKeyA[mReadSector]

      sendToOutput("[NFCResponse: Classic 4K Read Sector (0) Command=$read4K]")

      mNFCState = NFCState.CLASSIC_4K_READ
      mDevice!!.sendClassicNFCCommand(BaseData(read4K), false, false)
    } else if (mNFCState == NFCState.DESFIRE_DETECTED) {
      sendToOutput("[Mifare DESFire - Get Version P1]")
      mNFCState = NFCState.DESFIRE_GET_VERSION_P1
      mDevice!!.sendDESFireNFCCommand(BaseData("9060000000"), false, false)
    }
  }

  override fun OnProgress(p0: Int) {
    when (mFileTransferMode) {
      FileTransferMode.SEND_IMAGE -> sendToOutput("Send Image Progress: $p0")
      FileTransferMode.SEND_FILE -> sendToOutput("Send File Progress: $p0")
      FileTransferMode.GET_FILE -> sendToOutput("Get File Progress: $p0")
      FileTransferMode.UPDATE_FIRMWARE -> sendToOutput("Update Firmware Progress: $p0")
      FileTransferMode.NONE -> {
        sendToOutput("OnProgress: $p0")
      }
    }
  }

  override fun OnResult(p0: StatusCode?, p1: ByteArray?) {
//    TODO("Not yet implemented")
  }

  override fun OnCalculateMAC(p0: Byte, p1: ByteArray?): IResult {
    val result: IResult = Result(StatusCode.UNAVAILABLE)
    return result
  }

  override fun OnConnected(p0: String?) {
    sendToOutput("MQTT Device Connected: $p0")
    notifyMQTTDeviceStatusChanged()
  }

  private fun notifyMQTTDeviceStatusChanged() {

  }

  override fun OnDisconnected(p0: String?) {
    sendToOutput("MQTT Device Disconnected: $p0")
    notifyMQTTDeviceStatusChanged()
  }

  fun isConnected(): Boolean {
    var connected = false

    val device = getDevice()

    if ((device != null) && (device.connectionState == ConnectionState.Connected)) {
      connected = true
    }

    return connected
  }

  fun isDisconnected(): Boolean {
    var disconnected = true

    val device = getDevice()

    if ((device != null) && (device.connectionState != ConnectionState.Disconnected)) {
      disconnected = false
    }

    return disconnected
  }

}
