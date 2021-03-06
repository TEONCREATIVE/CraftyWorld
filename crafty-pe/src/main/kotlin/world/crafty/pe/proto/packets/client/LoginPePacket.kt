package world.crafty.pe.proto.packets.client

import io.vertx.core.json.JsonObject
import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.common.utils.CompressionAlgorithm
import world.crafty.common.utils.compressed
import world.crafty.common.utils.decompressed
import world.crafty.pe.jwt.PeJwt
import world.crafty.pe.proto.PePacket

class LoginPePacket(
        val protocolVersion: Int,
        val edition: Int,
        val certChain: List<PeJwt>,
        val clientData: PeJwt
) : PePacket() {
    override val id = Codec.id
    override val codec = Codec
    object Codec : PePacketCodec() {
        override val id = 0x01
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is LoginPePacket) throw IllegalArgumentException()
            stream.writeInt(obj.protocolVersion)
            stream.writeByte(obj.edition)
            throw NotImplementedError()
        }
        override fun deserialize(stream: MinecraftInputStream): PePacket {
            val protocolVersion = stream.readInt()
            val edition = stream.readByte().toInt()

            val payloadSize = stream.readUnsignedVarInt()
            val zlibedPayload = stream.readRemainingBytes()

            val payload = zlibedPayload.decompressed(CompressionAlgorithm.ZLIB, payloadSize)
            val pStream = MinecraftInputStream(payload)

            val chainStr = pStream.readUnsignedString(pStream.readIntLe())
            val chainJson = JsonObject(chainStr)
            val certChain = chainJson.getJsonArray("chain").map { PeJwt.parse(it as String) }

            val clientDataStr = pStream.readUnsignedString(pStream.readIntLe())
            val clientData = PeJwt.parse(clientDataStr)
            
            return LoginPePacket(
                    protocolVersion = protocolVersion,
                    edition = edition,
                    certChain = certChain,
                    clientData = clientData
            )
        }
    }
}