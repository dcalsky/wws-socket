package wws_socket

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.LoggerFactory
import wws_socket.handlers.*

class Server {
    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
        private const val PORT = 10001
    }

    fun run() {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap().apply {
                group(bossGroup, workerGroup)
                channel(NioServerSocketChannel::class.java)
                childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(
                            PackageEncoder(),
                            PackageDecoder(),
                            PackageHandler()
                        )
                    }
                })
                option(ChannelOption.SO_BACKLOG, 128)
                childOption(ChannelOption.SO_KEEPALIVE, true)
            }
            logger.info("Server is listening port: $PORT")
            val f = b.bind(PORT).sync()
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }
}


fun main() {
    val s = Server()
    s.run()
}