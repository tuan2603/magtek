package com.magtek

import java.util.*

object MTParser {
    fun parseTLV(data: ByteArray?): List<Map<String, String>> {
        val fillMaps = ArrayList<Map<String, String>>()

        if (data != null) {
            val dataLen = data.size

            if (dataLen >= 2) {
                val tlvLen = data.size
                val tlvData = data

                if (tlvData != null) {
                    var iTLV: Int
                    var iTag: Int
                    var iLen: Int
                    var bTag: Boolean
                    var bMoreTagBytes: Boolean
                    var bConstructedTag: Boolean
                    var byteValue: Byte
                    var lengthValue: Int

                    var tagBytes: ByteArray? = null

                    val MoreTagBytesFlag1 = 0x1F.toByte()
                    val MoreTagBytesFlag2 = 0x80.toByte()
                    val ConstructedFlag = 0x20.toByte()
                    val SpecialTagFlag = 0xF0.toByte()
                    val MoreLengthFlag = 0x80.toByte()
                    val OneByteLengthMask = 0x7F.toByte()

                    val TagBuffer = ByteArray(50)

                    bTag = true
                    iTLV = 0

                    while (iTLV < tlvData.size) {
                        byteValue = tlvData[iTLV]

                        if (bTag) {
                            // Get Tag
                            iTag = 0
                            bMoreTagBytes = true

                            while (bMoreTagBytes && (iTLV < tlvData.size)) {
                                byteValue = tlvData[iTLV]
                                iTLV++

                                TagBuffer[iTag] = byteValue

                                if (iTag == 0) {
                                    bMoreTagBytes = (byteValue.toInt() and MoreTagBytesFlag1.toInt()) == MoreTagBytesFlag1.toInt()
                                } else {
                                    bMoreTagBytes = (byteValue.toInt() and MoreTagBytesFlag2.toInt()) == MoreTagBytesFlag2.toInt()
                                }

                                iTag++
                            }

                            tagBytes = ByteArray(iTag)
                            System.arraycopy(TagBuffer, 0, tagBytes, 0, iTag)

                            bTag = false
                        } else {
                            // Get Length
                            lengthValue = 0

                            if ((byteValue.toInt() and MoreLengthFlag.toInt()) == MoreLengthFlag.toInt()) {
                                val nLengthBytes = (byteValue.toInt() and OneByteLengthMask.toInt())

                                iTLV++
                                iLen = 0

                                while ((iLen < nLengthBytes) && (iTLV < tlvData.size)) {
                                    byteValue = tlvData[iTLV]
                                    iTLV++
                                    lengthValue = ((lengthValue and 0x000000FF) shl 8) + (byteValue.toInt() and 0x000000FF)
                                    iLen++
                                }
                            } else {
                                lengthValue = byteValue.toInt() and OneByteLengthMask.toInt()
                                iTLV++
                            }

                            if (tagBytes != null) {
                                val tagByte = tagBytes[0]

                                bConstructedTag = (tagByte.toInt() and ConstructedFlag.toInt()) == ConstructedFlag.toInt()

                                if (bConstructedTag) {
                                    // Constructed
                                    val map = HashMap<String, String>()
                                    map["tag"] = getHexString(tagBytes)
                                    map["len"] = lengthValue.toString()
                                    map["value"] = "[Container]"
                                    fillMaps.add(map)
                                } else {
                                    // Primitive
                                    var endIndex = iTLV + lengthValue

                                    if (endIndex > tlvData.size)
                                        endIndex = tlvData.size

                                    var valueBytes: ByteArray? = null
                                    val len = endIndex - iTLV
                                    if (len > 0) {
                                        valueBytes = ByteArray(len)
                                        System.arraycopy(tlvData, iTLV, valueBytes, 0, len)
                                    }

                                    val map = HashMap<String, String>()
                                    map["tag"] = getHexString(tagBytes)
                                    map["len"] = lengthValue.toString()

                                    if (valueBytes != null)
                                        map["value"] = getHexString(valueBytes)
                                    else
                                        map["value"] = ""

                                    fillMaps.add(map)

                                    iTLV += lengthValue
                                }
                            }

                            bTag = true
                        }
                    }
                }
            }
        }

        return fillMaps
    }

    fun getTagValue(fillMaps: List<Map<String, String>>, tagString: String): String {
        var valueString = ""

        val it = fillMaps.listIterator()

        while (it.hasNext()) {
            val map = it.next()

            if (map["tag"]?.equals(tagString, ignoreCase = true) == true) {
                valueString = map["value"] ?: ""
            }
        }

        return valueString
    }

    fun getTagByteArrayValue(fillMaps: List<Map<String, String>>, tagString: String): ByteArray? {
        var valueBytes: ByteArray? = null

        val valueString = getTagValue(fillMaps, tagString)

        valueBytes = getByteArrayFromHexString(valueString)

        return valueBytes
    }

    fun getTextString(data: ByteArray?, start: Int): String {
        var result = ""

        if (data != null && data.isNotEmpty()) {
            result = getTextString(data, start, data.size)
        }

        return result
    }

    fun getTextString(data: ByteArray?, start: Int, length: Int): String {
        var result = ""

        if (data != null && data.isNotEmpty()) {
            val stringBuilder = StringBuilder(data.size + 1)
            for (i in start until length) {
                try {
                    stringBuilder.append(String.format("%c", data[i]))
                } catch (ex: Exception) {
                    stringBuilder.append("<?>")
                }
            }
            result = stringBuilder.toString()
        }

        return result
    }

    fun getHexString(data: ByteArray?): String {
        var result = ""

        if (data != null && data.isNotEmpty()) {
            val byteLength = 2

            val stringBuilder = StringBuilder(data.size * byteLength + 1)

            for (i in data.indices) {
                try {
                    stringBuilder.append(String.format("%02X", data[i]))
                } catch (ex: Exception) {
                    stringBuilder.append("  ")
                }
            }
            result = stringBuilder.toString()
        }

        return result
    }

    fun getByteArrayFromHexString(hexString: String?): ByteArray? {
        val byteLength = 2

        var result: ByteArray? = null

        if (hexString != null) {
            result = ByteArray(hexString.length / byteLength)

            val hexCharArray = hexString.uppercase().toCharArray()

            for (i in result.indices) {
                val sbCurrent = StringBuffer("")
                sbCurrent.append(hexCharArray[i * byteLength].toString())
                sbCurrent.append(hexCharArray[i * byteLength + 1].toString())
                try {
                    result[i] = sbCurrent.toString().toInt(16).toByte()
                } catch (ex: Exception) {
                    // Handle exception
                }
            }
        }

        return result
    }
} 