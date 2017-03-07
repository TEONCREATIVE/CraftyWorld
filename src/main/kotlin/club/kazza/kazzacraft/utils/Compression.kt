package club.kazza.kazzacraft.utils

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

fun ByteArray.decompress(algo: CompressionAlgorithm, expectedSize: Int = 0) : ByteArray {    
    val inflater = Inflater()
    inflater.setInput(this)
    
    val bufferSize = (if(expectedSize == 0) size else expectedSize) + 32
    
    val bs = ByteArrayOutputStream(bufferSize)
    while(! inflater.finished()) {
        val buffer = ByteArray(bufferSize)
        val decompressedSize = inflater.inflate(buffer)
        bs.write(buffer, 0, decompressedSize)
    }
    return bs.toByteArray()
}

fun ByteArray.compress(algo: CompressionAlgorithm, level: Int = Deflater.DEFAULT_COMPRESSION) : ByteArray {
    val deflater = Deflater()
    deflater.setInput(this)
    deflater.setLevel(level)
    deflater.finish()
    
    val bufferSize = size + 32
    
    val bs = ByteArrayOutputStream(bufferSize)
    while(! deflater.finished()) {
        val buffer = ByteArray(bufferSize)
        val compressedSize = deflater.deflate(buffer)
        bs.write(buffer, 0, compressedSize)
    }
    return bs.toByteArray()
}

enum class CompressionAlgorithm {
    ZLIB
}