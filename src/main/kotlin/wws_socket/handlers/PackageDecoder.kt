package wws_socket.handlers

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.slf4j.LoggerFactory
import wws_socket.models.*

class PackageDecoder : ByteToMessageDecoder() {
    companion object {
        private val logger = LoggerFactory.getLogger(PackageDecoder::class.java)
        private const val headerLen = 20
    }

    override fun decode(ctx: ChannelHandlerContext?, buf: ByteBuf, out: MutableList<Any>) {
        if (buf.readableBytes() < headerLen) {
            // Missing Header. Waiting for next frame
            return
        }
        // Read header
        val headerOffset = buf.readerIndex()
        val messageType = buf.getShortLE(headerOffset)
        val messageLen = buf.getIntLE(headerOffset + 2)
        val sendTime = buf.getLongLE(headerOffset + 6)
        val hash = Unpooled.buffer()
        buf.getBytes(headerOffset + 14, hash, 6)

        val requestLen = headerLen + messageLen
        if (buf.readableBytes() < requestLen) {
            // Missing body. Waiting for next frame
            return
        }
        // Skip already read header
        buf.skipBytes(headerLen)

        // Read Body
        val bodyFrame = buf.readBytes(messageLen)
        val request = PackageT(
            messageType = MessageType.valueOf(messageType),
            messageLength = messageLen,
            sendTime = sendTime,
            body = bodyFrame,
            hash = hash
        )
        out.add(request)
    }
}