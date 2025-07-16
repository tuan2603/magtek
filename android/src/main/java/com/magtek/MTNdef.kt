package com.magtek

import java.io.ByteArrayOutputStream

object MTNdef {
    fun getNDEFMessages(tlvString: String): List<String> {
        val results = ArrayList<String>()

        val tagData = MTParser.getByteArrayFromHexString(tlvString)

        var offset = 0
        while (offset < tagData!!.size) {
            val tag = tagData[offset++]

            if (tag == 0x03.toByte()) // NDEF Message
            {
                var len = 0

                if (offset < tagData.size) {
                    len = (tagData[offset++].toInt() and 0x0FF)
                    if (len == 255) // 2-byte length follows
                    {
                        if ((offset + 1) < tagData.size) {
                            len = ((tagData[offset++].toInt() and 0x0FF) shl 8)
                            len = len or (tagData[offset++].toInt() and 0x0FF)
                        }
                    }
                }

                if (len > 0) {
                    val msgBytes = ByteArray(len)
                    System.arraycopy(tagData, offset, msgBytes, 0, len)
                    val msgString = MTParser.getHexString(msgBytes)
                    results.add(msgString)

                    offset += len
                }
            } else if (tag == 0xFE.toByte()) {
                break
            }
        }

        return results
    }

    fun Parse(data: ByteArray?): List<MTNdefRecord> {
        val records = ArrayList<MTNdefRecord>()

        var chunkStream: ByteArrayOutputStream? = null

        var record: MTNdefRecord? = null

        var i = 0
        while (i < data!!.size) {
            if (record == null)
                record = MTNdefRecord()

            val flag = data[i++]

            val be = (flag.toInt() and 0x80) != 0
            val me = (flag.toInt() and 0x40) != 0
            val cf = (flag.toInt() and 0x20) != 0
            val sr = (flag.toInt() and 0x10) != 0
            val il = (flag.toInt() and 0x08) != 0

            record!!.TNF = (flag.toInt() and 0x07).toByte()

            var headerLen = 1
            headerLen += if (sr) 1 else 4
            headerLen += if (il) 1 else 0

            var typeLen: Byte = 0
            var payloadLen = 0

            if ((i + headerLen) < data.size) {
                typeLen = data[i++]

                if (sr) {
                    payloadLen = data[i++].toInt()
                } else {
                    payloadLen = payloadLen or ((data[i++].toInt()) shl 24)
                    payloadLen = payloadLen or ((data[i++].toInt()) shl 16)
                    payloadLen = payloadLen or ((data[i++].toInt()) shl 8)
                    payloadLen = payloadLen or ((data[i++].toInt()) shl 0)
                }
            }

            var idLen: Byte = 0

            if ((il) && (i < data.size)) {
                idLen = data[i++]
            }

            val totalLen = typeLen.toInt() + payloadLen + idLen.toInt()

            if ((i + totalLen) <= data.size) {
                if (typeLen > 0) {
                    record.Type = ByteArray(typeLen.toInt())
                    System.arraycopy(data, i, record.Type, 0, typeLen.toInt())
                    i += typeLen.toInt()
                }

                if (idLen > 0) {
                    record.ID = ByteArray(idLen.toInt())
                    System.arraycopy(data, i, record.ID, 0, idLen.toInt())
                    i += idLen.toInt()
                }

                if (payloadLen > 0) {
                    val payload = ByteArray(payloadLen)
                    System.arraycopy(data, i, payload, 0, payloadLen)
                    i += payloadLen

                    if (cf) {
                        if (chunkStream == null)
                            chunkStream = ByteArrayOutputStream()

                        chunkStream.write(payload, 0, payload.size)
                    } else if (chunkStream != null) {
                        chunkStream.write(payload, 0, payload.size)
                        record.Payload = chunkStream.toByteArray()
                        chunkStream = null
                    } else {
                        record.Payload = payload
                    }
                }
            }

            if (!cf) {
                records.add(record)
                record = null

                if (me)
                    break
            }
        }

        return records
    }

    fun BuildNDEFMessage(records: List<MTNdefRecord>): ByteArray? {
        var messageBytes: ByteArray? = null

        val recordsStream = ByteArrayOutputStream()

        val n = records.size

        for (i in 0 until n) {
            val record = records[i]
            val recordBytes = record.toBytes()

            if (i == 0) {
                recordBytes[0] = (recordBytes[0].toInt() or 0x80).toByte() // MB
            } else if (i == (n - 1)) {
                recordBytes[0] = (recordBytes[0].toInt() or 0x40).toByte() // ME
            }

            recordsStream.write(recordBytes, 0, recordBytes.size)
        }

        val recordsArray = recordsStream.toByteArray()

        if (recordsArray != null) {
            val len = recordsArray.size

            if (len < 255) {
                messageBytes = ByteArray(len + 2)
                messageBytes[0] = 3 // NDEF Tag
                messageBytes[1] = (len and 0xFF).toByte()
                System.arraycopy(recordsArray, 0, messageBytes, 2, len)
            } else {
                messageBytes = ByteArray(len + 4)
                messageBytes[0] = 3 // NDEF Tag
                messageBytes[1] = 0xFF.toByte()
                messageBytes[2] = ((len shr 8) and 0xFF).toByte()
                messageBytes[3] = (len and 0xFF).toByte()
                System.arraycopy(recordsArray, 0, messageBytes, 4, len)
            }
        }

        return messageBytes
    }
} 