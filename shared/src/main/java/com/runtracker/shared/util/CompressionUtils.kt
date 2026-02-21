package com.runtracker.shared.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CompressionUtils {

    /**
     * Compresses a byte array using GZIP compression.
     * Typically achieves 60-80% compression on JSON data.
     */
    fun gzipCompress(data: ByteArray): ByteArray {
        ByteArrayOutputStream().use { byteStream ->
            GZIPOutputStream(byteStream).use { gzipStream ->
                gzipStream.write(data)
            }
            return byteStream.toByteArray()
        }
    }

    /**
     * Decompresses a GZIP-compressed byte array.
     */
    fun gzipDecompress(compressedData: ByteArray): ByteArray {
        ByteArrayInputStream(compressedData).use { byteStream ->
            GZIPInputStream(byteStream).use { gzipStream ->
                return gzipStream.readBytes()
            }
        }
    }
}
