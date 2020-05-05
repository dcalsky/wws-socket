package wws_socket.handlers

import io.netty.buffer.*
import io.netty.channel.*
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory
import wws_socket.models.*
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

class PackageHandler : SimpleChannelInboundHandler<PackageT>() {
    private lateinit var user: User

    companion object {
        private val logger = LoggerFactory.getLogger(PackageHandler::class.java)
        private val users = ConcurrentHashMap<User, Channel>()
        private val rooms = ConcurrentHashMap<Int, Room>()
        private var counter = 0
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        cause?.printStackTrace()
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: PackageT) {
        logger.info("[${counter}] Received a request from client: $msg")
        counter += 1
        val r = this.handleRequest(msg)
        ctx.channel().writeAndFlush(r)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()
        user = User(
            name = "Unnamed",
            ip = channel.remoteAddress().toString().substring(1).split(":").first()
        )
        users[user] = ctx.channel()
    }


    override fun channelInactive(ctx: ChannelHandlerContext) {
        users.remove(user)
    }

    private fun handleRequest(req: PackageT): PackageT {
        val emptyBuf = Unpooled.EMPTY_BUFFER
        val body = when (req.messageType) {
            MessageType.PING_MSG -> Pair(
                Unpooled.copyLong(req.sendTime),
                MessageType.PONG_MSG
            )
            MessageType.SET_NAME -> {
                this.setName(req)
            }
            MessageType.GET_ROOM_LIST -> {
                val body = this.getAllRoomDetails()
                Pair(
                    body, MessageType.ROOM_LIST
                )
            }
            MessageType.HOST_NEW_ROOM -> {
                this.hostNewRoom(req)
            }
            MessageType.CONNECT_ROOM -> {
                this.connectRoom(req)
            }
            MessageType.CHECK_ROOM_STATUS -> {
                val roomId = req.body.readIntLE()
                val room = rooms[roomId]
                val body = if (room == null) Unpooled.EMPTY_BUFFER else this.getRoomDetail(room, true)
                Pair(
                    body, MessageType.ROOM_STATUS
                )
            }
            MessageType.DISCONNECT_ROOM -> {
                this.disconnectRoom(req)
            }
            MessageType.SEND_MSG -> {
                this.sendMessage(req)
            }
            else -> Pair(Unpooled.EMPTY_BUFFER, MessageType.UNKNOWN_MSG)
        }
        val messageLength = body.first.readableBytes()
        return PackageT(
            messageType = body.second,
            messageLength = messageLength,
            sendTime = System.currentTimeMillis(),
            body = body.first,
            hash = req.hash
        )
    }


    private fun getAllRoomDetails(): ByteBuf {
        val buf = Unpooled.buffer().writeIntLE(rooms.size)
        rooms.values.forEach {
            buf.writeBytes(this.getRoomDetail(it, false))
        }
        return buf
    }

    private fun getRoomDetail(room: Room, isFull: Boolean): ByteBuf {
        val buf = Unpooled.buffer()
        buf.writeIntLE(room.id)
        buf.writeIntLE(room.users.size)
        val names = Unpooled.buffer(40).writeBytes(room.name.toByteArray()).writerIndex(40)
        buf.writeBytes(names)
        if (isFull) {
            room.users.forEach {
                buf.writeLongLE(it.joinTime!!)
                val b1 = Unpooled.buffer(20).writeBytes(it.ip.toByteArray()).writerIndex(20)
                buf.writeBytes(b1)
                val b2 = Unpooled.buffer(40).writeBytes(it.name.toByteArray()).writerIndex(40)
                buf.writeBytes(b2)
            }
        }
        return buf
    }

    private fun userQuitRoom() {
        val room = user.currentRoom
        if (room != null) {
            room.users.remove(user)
            user.joinTime = 0
            user.currentRoom = null
        }
    }

    private fun userJoinRoom(room: Room) {
        if (user.currentRoom != room) {
            userQuitRoom()
            if (!room.users.contains(user)) {
                room.users.add(user)
            }
            user.joinTime = System.currentTimeMillis()
            user.currentRoom = room
        }
    }

    private fun hostNewRoom(packageT: PackageT): Pair<ByteBuf, MessageType> {
        val type: MessageType
        val buf: ByteBuf
        if (packageT.messageLength !in 1..40 && user.currentRoom == null) {
            // TODO: user must be not in the other room
            type = MessageType.HOST_ROOM_FAILED
            buf = Unpooled.EMPTY_BUFFER
        } else {
            val room = Room(
                name = packageT.body.toString(Charset.defaultCharset()),
                host = user
            )
            rooms[room.id] = room
            userJoinRoom(room)
            type = MessageType.HOST_ROOM_SUCCESS
            buf = Unpooled.buffer(4)
            buf.writeIntLE(room.id)
            logger.info("User: ${user.name} hosted the room: ${room.id}")
        }
        return Pair(buf, type)
    }

    private fun connectRoom(packageT: PackageT): Pair<ByteBuf, MessageType> {
        val type: MessageType
        if (user.currentRoom !== null) {
            this.leaveRoom()
        }

        val roomId = packageT.body.readIntLE()
        val room = rooms[roomId]
        if (room == null) {
            type = MessageType.CONNECT_ROOM_FAILED
        } else {
            type = MessageType.CONNECT_ROOM_SUCCESS
            userJoinRoom(room)
            logger.info("User: ${user.name} joined the room: ${room.id}")
        }
        return Pair(Unpooled.EMPTY_BUFFER, type)
    }

    private fun setName(packageT: PackageT): Pair<ByteBuf, MessageType> {
        val type: MessageType
        val buf: ByteBuf = Unpooled.EMPTY_BUFFER
        if (packageT.messageLength in 1..40) {
            type = MessageType.SET_NAME_SUCCESS
            val newName = packageT.body.toString(CharsetUtil.UTF_8)
            user.name = newName
        } else {
            type = MessageType.SET_NAME_FAILED
        }
        return Pair(buf, type)
    }

    private fun sendMessage(packageT: PackageT): Pair<ByteBuf, MessageType> {
        val type: MessageType
        val room = user.currentRoom
        if (room != null && packageT.messageLength < 8192) {
            type = MessageType.SEND_MSG_SUCCESS
            val buf = Unpooled.buffer()
            // Append 64 bytes extra body
            buf.writeIntLE(packageT.messageLength)
            val b1 = Unpooled.buffer(20).writeBytes(user.ip.toByteArray()).writerIndex(20)
            buf.writeBytes(b1)
            val b2 = Unpooled.buffer(40).writeBytes(user.name.toByteArray()).writerIndex(40)
            buf.writeBytes(b2)
            buf.writeBytes(packageT.body)

            val msgPackage = PackageT(
                messageType = MessageType.ROOM_MSG,
                messageLength = packageT.messageLength + 64,
                sendTime = System.currentTimeMillis(),
                body = buf,
                hash = Unpooled.buffer().writerIndex(6)
            )
            if (room.host == user) {
                room.users.forEach {
                    val channel = users[it]
                    channel?.writeAndFlush(msgPackage)
                }
            } else {
                val channel = users[room.host]
                channel?.writeAndFlush(msgPackage)
            }
        } else {
            type = MessageType.SEND_MSG_FAILED
        }
        return Pair(Unpooled.EMPTY_BUFFER, type)
    }

    private fun leaveRoom() {
        val room = user.currentRoom ?: return
        room.users.remove(user)
        user.currentRoom = null
    }

    private fun disconnectRoom(packageT: PackageT): Pair<ByteBuf, MessageType> {
        val type = MessageType.DISCONNECT_ROOM_SUCCESS
        this.leaveRoom()
        return Pair(Unpooled.EMPTY_BUFFER, type)
    }
}