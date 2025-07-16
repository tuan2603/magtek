package com.magtek

import java.io.ByteArrayOutputStream

class MTNdefRecord {
    companion object {
        const val TNF_EMPTY: Byte = 0x00
        const val TNF_WELL_KNOWN: Byte = 0x01
        const val TNF_MIME_MEDIA: Byte = 0x02
        const val TNF_ABSOLUTE_URI: Byte = 0x03
        const val TNF_EXTERNAL_TYPE: Byte = 0x04
        const val TNF_UNKNOWN: Byte = 0x05
        const val TNF_UNCHANGED: Byte = 0x06
        const val TNF_RESERVED: Byte = 0x07

        val RTD_TEXT: ByteArray = byteArrayOf(0x54)  // "T"
        val RTD_URI: ByteArray = byteArrayOf(0x55)   // "U"
        val RTD_SMART_POSTER: ByteArray = byteArrayOf(0x53, 0x70)  // "Sp"

        val URI_MAP = arrayOf(
            "", // 0x00
            "http://www.", // 0x01
            "https://www.", // 0x02
            "http://", // 0x03
            "https://", // 0x04
            "tel:", // 0x05
            "mailto:", // 0x06
            "ftp://anonymous:anonymous@", // 0x07
            "ftp://ftp.", // 0x08
            "ftps://", // 0x09
            "sftp://", // 0x0A
            "smb://", // 0x0B
            "nfs://", // 0x0C
            "ftp://", // 0x0D
            "dav://", // 0x0E
            "news:", // 0x0F
            "telnet://", // 0x10
            "imap:", // 0x11
            "rtsp://", // 0x12
            "urn:", // 0x13
            "pop:", // 0x14
            "sip:", // 0x15
            "sips:", // 0x16
            "tftp:", // 0x17
            "btspp://", // 0x18
            "btl2cap://", // 0x19
            "btgoep://", // 0x1A
            "tcpobex://", // 0x1B
            "irdaobex://", // 0x1C
            "file://", // 0x1D
            "urn:epc:id:", // 0x1E
            "urn:epc:tag:", // 0x1F
            "urn:epc:pat:", // 0x20
            "urn:epc:raw:", // 0x21
            "urn:epc:", // 0x22
            "urn:nfc:", // 0x23
        )

        fun createTextRecord(textPayload: ByteArray): MTNdefRecord {
            return MTNdefRecord(TNF_WELL_KNOWN, RTD_TEXT, null, textPayload)
        }

        fun createUriRecord(uriPayload: ByteArray): MTNdefRecord {
            return MTNdefRecord(TNF_WELL_KNOWN, RTD_URI, null, uriPayload)
        }

        fun createMimeRecord(mimeType: ByteArray, mimePayload: ByteArray): MTNdefRecord {
            return MTNdefRecord(TNF_MIME_MEDIA, mimeType, null, mimePayload)
        }

        fun createAbsoluteUriRecord(uri: ByteArray): MTNdefRecord {
            return MTNdefRecord(TNF_ABSOLUTE_URI, uri, null, null)
        }

        fun createExternalRecord(extType: ByteArray, extPayload: ByteArray): MTNdefRecord {
            return MTNdefRecord(TNF_EXTERNAL_TYPE, extType, null, extPayload)
        }
    }

    var TNF: Byte = 0
    var Type: ByteArray? = null
    var ID: ByteArray? = null
    var Payload: ByteArray? = null

    constructor() {
        TNF = 0
        Type = null
        ID = null
        Payload = null
    }

    constructor(tnf: Byte, type: ByteArray?, id: ByteArray?, payload: ByteArray?) {
        TNF = tnf
        Type = type
        ID = id
        Payload = payload
    }

    fun toBytes(): ByteArray {
        val recordStream = ByteArrayOutputStream()

        var b0 = TNF

        b0 = (b0.toInt() or 0x10).toByte()

        if (ID != null)
            b0 = (b0.toInt() or 0x08).toByte()

        if (Payload != null) {
            recordStream.write(byteArrayOf(b0, 1, Payload!!.size.toByte()), 0, 3)

            if (Type != null) {
                recordStream.write(Type, 0, Type!!.size)
            }
            /*
                if (ID != null)
                {
                    recordStream.Write(ID, 0, ID.Length);
                }
            */
            if (Payload != null) {
                recordStream.write(Payload, 0, Payload!!.size)
            }
        }

        return recordStream.toByteArray()
    }

    fun isRtdType(typeName: ByteArray?): Boolean {
        var result = false

        //if (TNF == TNF_WELL_KNOWN) // Well Known Types
        if ((Type != null) && (typeName != null) && (typeName.isNotEmpty())) {
            if (Type!!.size == typeName.size) {
                result = true

                for (i in typeName.indices) {
                    if (Type!![i] != typeName[i])
                        result = false
                }
            }
        }

        return result
    }

    fun isUri(): Boolean {
        return isRtdType(RTD_URI)
    }

    fun isText(): Boolean {
        return isRtdType(RTD_TEXT)
    }

    fun isWellKnownType(): Boolean {
        return (TNF == TNF_WELL_KNOWN)
    }

    fun isMimeType(): Boolean {
        return (TNF == TNF_MIME_MEDIA)
    }

    fun isAbsoluteUriType(): Boolean {
        return (TNF == TNF_ABSOLUTE_URI)
    }

    fun isExternalType(): Boolean {
        return (TNF == TNF_EXTERNAL_TYPE)
    }

    fun getUriString(): String {
        var uriString = ""

        if (isUri()) {
            uriString = ""

            if ((Payload != null) && (Payload!!.size > 1)) {
                val uriPrefix = Payload!![0]

                if (uriPrefix >= 0 && uriPrefix <= 0x23) {
                    uriString = URI_MAP[uriPrefix.toInt()]
                }

                val len = Payload!!.size - 1

                if (len > 0) {
                    val textBytes = ByteArray(len)

                    System.arraycopy(Payload, 1, textBytes, 0, len)

                    uriString += String(textBytes)
                }
            }
        } else if (isAbsoluteUriType()) {
            if (Type != null) {
                uriString = String(Type!!)
            }
        }

        return uriString
    }

    fun getTextString(): String {
        var textString = ""

        if (isText()) {
            if (Payload != null) {
                val len = Payload!!.size
                var i = 0

                if (len > 0) {
                    val utf8 = (Payload!![0].toInt() and 0x80) == 0

                    val lenLang = (Payload!![i++].toInt() and 0x3F).toByte()

                    var remainingLen = len - 1

                    if (remainingLen >= lenLang.toInt()) {
                        val langBytes = ByteArray(lenLang.toInt())

                        System.arraycopy(Payload, i, langBytes, 0, lenLang.toInt())

                        i += lenLang.toInt()
                        remainingLen -= lenLang.toInt()
                    }

                    if (remainingLen > 0) {
                        val textBytes = ByteArray(remainingLen)

                        System.arraycopy(Payload, i, textBytes, 0, remainingLen)

                        if (utf8) {
                            textString = String(textBytes)
                        } else {
                            textString = String(textBytes)
                        }
                    }
                }
            }
        } else if (isMimeType()) {
            if (Payload != null) {
                textString = String(Payload!!)
            }
        } else if (isExternalType()) {
            if (Payload != null) {
                textString = String(Payload!!)
            }
        }

        return textString
    }
}
