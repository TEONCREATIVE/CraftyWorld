package world.crafty.pc.proto.packets.client

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.pc.proto.PcPacket

class StatusRequestPcPacket(

) : PcPacket() {
    override val id = Codec.id
    override val codec = Codec
    companion object Codec : PcPacketCodec() {
        override val id = 0x00
        override fun serialize(obj: Any, stream: MinecraftOutputStream) {
            if(obj !is StatusRequestPcPacket) throw IllegalArgumentException()
        }
        override fun deserialize(stream: MinecraftInputStream) : PcPacket {
            return StatusRequestPcPacket()
        }
    }
}