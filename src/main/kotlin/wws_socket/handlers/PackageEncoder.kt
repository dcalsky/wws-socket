package wws_socket.handlers

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import wws_socket.models.PackageT

class PackageEncoder : MessageToByteEncoder<PackageT>() {
    override fun encode(ctx: ChannelHandlerContext, msg: PackageT, out: ByteBuf) {
        out.writeShortLE(msg.messageType.value.toInt())
        out.writeIntLE(msg.messageLength)
        out.writeLongLE(msg.sendTime)
        out.writeBytes(msg.hash)
        out.writeBytes(msg.body)
    }
}