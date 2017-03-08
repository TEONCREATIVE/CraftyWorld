package club.kazza.kazzacraft;

import club.kazza.kazzacraft.network.PcConnectionServer
import club.kazza.kazzacraft.network.PeConnectionServer
import club.kazza.kazzacraft.world.anvil.loadWorld
import io.vertx.core.Vertx

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val before = System.currentTimeMillis()
    val world = loadWorld("C:/Development/Minecraft/Kazza/Code/KazzaCraft/neus")
    val elapsed = System.currentTimeMillis() - before
    println("world loading: $elapsed ms")

    //val pcServer = PcConnectionServer(25565, world)
    //vertx.deployVerticle(pcServer)

    val peServer = PeConnectionServer(19132)
    vertx.deployVerticle(peServer)
    
    println("Shard running")
}