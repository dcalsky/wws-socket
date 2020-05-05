package wws_socket.models

import io.netty.buffer.ByteBuf

enum class MessageType(val value: Short) {
    UNKNOWN_MSG(100),
    PING_MSG(0),
    PONG_MSG(1),
    SET_NAME(2),
    SET_NAME_SUCCESS(3),
    SET_NAME_FAILED(4),
    GET_ROOM_LIST(5),
    ROOM_LIST(6),
    HOST_NEW_ROOM(7),
    HOST_ROOM_SUCCESS(8),
    HOST_ROOM_FAILED(9),
    CONNECT_ROOM(10),
    CONNECT_ROOM_SUCCESS(11),
    CONNECT_ROOM_FAILED(12),
    CHECK_ROOM_STATUS(13),
    ROOM_STATUS(14),
    DISCONNECT_ROOM(15),
    DISCONNECT_ROOM_SUCCESS(16),
    ROOM_MSG(17),
    SEND_MSG(18),
    SEND_MSG_SUCCESS(19),
    SEND_MSG_FAILED(20),
    ROOM_CLEAN_OUT(21);

    companion object {
        private val map = values().associateBy(MessageType::value)
        fun valueOf(value: Short) = map.getOrDefault(value, UNKNOWN_MSG)
    }
}


data class PackageT(
    val messageType: MessageType,
    val messageLength: Int,
    val sendTime: Long,
    val body: ByteBuf,
    val hash: ByteBuf
)
