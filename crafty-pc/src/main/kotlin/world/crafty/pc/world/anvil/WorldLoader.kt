package world.crafty.pc.world.anvil

import world.crafty.common.serialization.MinecraftInputStream
import world.crafty.common.utils.NibbleArray
import world.crafty.nbt.NbtInputStream
import world.crafty.nbt.tags.*
import world.crafty.pc.world.PcChunkColumn
import world.crafty.pc.world.PcChunk
import world.crafty.pc.world.Location
import world.crafty.pc.world.World
import java.io.File
import java.io.FileInputStream
import java.util.stream.Collectors
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

fun loadWorld(folder: String) : World {
    val levelStream = NbtInputStream(GZIPInputStream(FileInputStream("$folder/level.dat")))
    val levelCompound = (levelStream.readTag() as NbtCompound)["Data"] as NbtCompound
    val spawnPoint = Location(
            (levelCompound["SpawnX"] as NbtInt).value.toDouble(),
            (levelCompound["SpawnY"] as NbtInt).value.toDouble(),
            (levelCompound["SpawnZ"] as NbtInt).value.toDouble()
    )

    val regions = File("$folder/region").listFiles().filter { it.path.endsWith("mca") }.map(::readRegion)
    val chunks = regions.flatten()

    return World(chunks, spawnPoint)
}

data class ChunkEntry(val offset: Int, val size: Int)
fun readRegion(file: File) : List<PcChunkColumn> {
    val regionBytes = file.readBytes()
    val regionStream = MinecraftInputStream(regionBytes)
    val chunkEntries = (0 until 1024).map {
        val entry = regionStream.readInt()
        val chunkOffset = entry ushr 8
        val chunkSize = entry and 0xF
        ChunkEntry(chunkOffset * 4096, chunkSize * 4096)
    }.filterNot { it.offset == 0 && it.size == 0 }

    return chunkEntries.parallelStream().map {
        val headerStream = MinecraftInputStream(regionBytes, it.offset, it.size)
        val chunkSize = headerStream.readInt() - 1 // - 1 because of the compressionScheme byte
        val compressionScheme = headerStream.readByte().toInt()
        val chunkStream = MinecraftInputStream(regionBytes, it.offset + 5, chunkSize)
        val compressionStream = if(compressionScheme == 1) GZIPInputStream(chunkStream) else InflaterInputStream(chunkStream)
        val chunkNbtStream = NbtInputStream(compressionStream)
        val chunkCompound = (chunkNbtStream.readTag() as NbtCompound)["Level"] as NbtCompound
        readChunk(chunkCompound)
    }.collect(Collectors.toList<PcChunkColumn>())
}

fun readChunk(nbt: NbtCompound) : PcChunkColumn {
    val chunkX = (nbt["xPos"] as NbtInt).value
    val chunkZ = (nbt["zPos"] as NbtInt).value

    val biomes = (nbt["Biomes"] as NbtByteArray).value
    val sections = arrayOfNulls<PcChunk>(16)

    (nbt["Sections"] as NbtList).forEach {
        val sectionCompound = it as NbtCompound
        val sectionY = (sectionCompound["Y"] as NbtByte).value

        val blockLight = NibbleArray((sectionCompound["BlockLight"] as NbtByteArray).value)
        val skyLight = NibbleArray((sectionCompound["SkyLight"] as NbtByteArray).value)

        val section = PcChunk(blockLight, skyLight)

        val blocksId = MinecraftInputStream((sectionCompound["Blocks"] as NbtByteArray).value)
        val blocksData = NibbleArray((sectionCompound["Data"] as NbtByteArray).value)

        for(y in 0..15) {
            for(z in 0..15) {
                for(x in 0..15) {
                    val id = blocksId.readUnsignedByte()
                    val extraIndex = y * 16 * 16 + z * 16 + x
                    val data = blocksData[extraIndex]
                    section.setTypeAndData(x, y, z, id, data)
                }
            }
        }
        sections[sectionY] = section
    }

    val chunk = PcChunkColumn(chunkX, chunkZ, sections, biomes)

    return chunk
}

fun streamFromNbt(compound: NbtCompound, tag: String) : MinecraftInputStream {
    return MinecraftInputStream((compound[tag] as NbtByteArray).value)
}