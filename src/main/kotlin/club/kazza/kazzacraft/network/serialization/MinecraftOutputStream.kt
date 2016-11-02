package club.kazza.kazzacraft.network.serialization

import club.kazza.kazzacraft.Location
import club.kazza.kazzacraft.network.protocol.PcPacket
import club.kazza.kazzacraft.world.ChunkSection
import io.vertx.core.json.Json
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.*

class MinecraftOutputStream(stream: OutputStream) : DataOutputStream(stream) {
    constructor() : this(ByteArrayOutputStream())

    fun writeVarInt(value: Int) {
        var v = value
        val bytes = ArrayList<Byte>(4)
        while((v and -0x80) != 0x00) {
            bytes.add((v and 0x7F or 0x80).toByte())
            v = (v shr 7)
        }
        bytes.add(v.toByte())
        write(bytes.toByteArray())
    }

    fun writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeVarInt(bytes.size)
        write(bytes)
    }

    fun writeJson(value: Any) {
        val json = Json.encode(value)
        writeString(json)
    }

    fun writeBlockLocation(value: Location) {
        val x = value.x.toLong()
        val y = value.y.toLong()
        val z = value.z.toLong()
        writeLong((x and 0x3ffffff) shl 38 or (y and 0xfff) shl 26 or z and 0x3ffffff)
    }

    fun writeUUID(value: UUID) {
        writeLong(value.mostSignificantBits)
        writeLong(value.leastSignificantBits)
    }

    fun writeChunkSection(section: ChunkSection) {

    }

    fun writePacket(packet: PcPacket, compressed: Boolean = false) {
        val contentStream = ByteArrayOutputStream()
        val contentMcStream = MinecraftOutputStream(contentStream)
        packet.serialize(contentMcStream)
        val contentBytes = contentStream.toByteArray()

        writeVarInt(contentBytes.size + 1 + (if(compressed) 1 else 0))
        if(compressed) writeVarInt(0)
        writeVarInt(packet.id)
        write(contentBytes)
    }

    companion object {
        fun varIntSize(value: Int) : Int {
            var v = value
            var size = 0
            while((v and -0x80) != 0x00) {
                ++size
                v = (v shr 7)
            }
            return size + 1
        }
    }
}