package wws_socket.models

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

const val SEED: Int = 123
val random = Random(SEED)

class Room(
    var name: String,
    var host: User
) {
    var id: Int = random.nextInt(10000)
    val users = ConcurrentLinkedQueue<User>().apply {
        this.add(host)
    }
}