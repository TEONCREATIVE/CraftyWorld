package world.crafty.nbt.tags

import world.crafty.nbt.NbtInputStream
import java.io.DataOutput
import java.io.DataOutputStream

class NbtIntArray(name: String?, val value: IntArray) : NbtValueTag(name) {
    override val tagType = NbtTagType.INT_ARRAY
    override val codec = Codec

    override fun deepClone(): NbtTag {
        return NbtIntArray(name, value.clone())
    }

    override fun prettyValueStr(): String {
        return "[${value.size} ints]"
    }

    object Codec : NbtTagCodec() {
        override val id = 11

        override fun serialize(obj: Any, stream: DataOutput) {
            if(obj !is NbtIntArray) throw IllegalArgumentException()
            stream.writeInt(obj.value.size)
            obj.value.forEach { stream.writeInt(it) }
        }
        override fun deserialize(name: String?, stream: NbtInputStream): NbtTag {
            val len = stream.readInt()
            val array = IntArray(len)
            for(i in 0 until len)
                array[i] = stream.readInt()
            return NbtIntArray(name, array)
        }
    }
}