package club.kazza.kazzacraft.network.raknet

import club.kazza.kazzacraft.network.PeNetworkSession
import java.time.Duration
import java.time.Instant

class RakSentDatagram(val datagram: RakDatagram, val recipient: PeNetworkSession, firstSend: Instant = Instant.now()) {
    private val sends = mutableListOf<Instant>()
    init {
        sends.add(firstSend)
    }
    
    val sinceLastSend: Duration
        get() = Duration.between(Instant.now(), sends.last())
    
    fun incSend() {
        sends.add(Instant.now())
    }
}