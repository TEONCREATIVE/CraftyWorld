package world.crafty.pc

import io.vertx.core.AbstractVerticle
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import world.crafty.common.serialization.MinecraftOutputStream
import world.crafty.mojang.MojangClient
import world.crafty.pc.proto.packets.server.ServerKeepAlivePcPacket
import world.crafty.pc.proto.PrecompressedPayload
import world.crafty.proto.ConcurrentColumnsCache
import world.crafty.proto.registerVertxCraftyCodecs
import world.crafty.skinpool.protocol.registerVertxSkinPoolCodecs
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher

class PcConnectionServer(val port: Int, val worldServer: String) : AbstractVerticle() {
    lateinit var server: NetServer
    val sessions = mutableMapOf<NetSocket, PcNetworkSession>()
    private val worldCaches = ConcurrentHashMap<String, ConcurrentColumnsCache<PrecompressedPayload>>() // TODO: share between connections server somehow

    lateinit var mojang: MojangClient

    val decipher: Cipher
    val x509PubKey: ByteArray
    val encodedBrand: ByteArray

    init {
        val rsaKey = generateKeyPair()
        x509PubKey = convertKeyToX509(rsaKey.public).encoded
        decipher = createDecipher(rsaKey)
        
        encodedBrand = MinecraftOutputStream.serialized {
            it.writeSignedString("crafty")
        }
    }

    override fun start() {
        val eb = vertx.eventBus()
        registerVertxCraftyCodecs(eb)
        registerVertxSkinPoolCodecs(eb)
        
        mojang = MojangClient(vertx)

        server = vertx.createNetServer()
        server.connectHandler {
            val session = PcNetworkSession(this, worldServer, it)
            sessions[it] = session
            it.handler { session.receive(it) }
            println("Received PC connection from ${it.remoteAddress()}")
        }
        server.listen(port)

        vertx.setPeriodic(1000) {
            sessions.values.filter { it.state == PcNetworkSession.State.PLAY }.forEach { session ->
                session.send(ServerKeepAlivePcPacket(0))
            }
        }

        vertx.setPeriodic(1000) {
            val toRemove = sessions.filter { it.value.lastUpdate.elapsed.seconds > 3 }
            for(key in toRemove.keys)
                sessions.remove(key)
        }
    }
    
    fun getWorldCache(worldName: String) : ConcurrentColumnsCache<PrecompressedPayload> {
        return worldCaches.getOrPut(worldName) { ConcurrentColumnsCache() }
    }
}