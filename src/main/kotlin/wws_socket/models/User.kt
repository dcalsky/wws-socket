package wws_socket.models

class User(
    var name: String = "",
    val ip: String
) {
    var joinTime: Long? = 0L
    var currentRoom: Room? = null
}