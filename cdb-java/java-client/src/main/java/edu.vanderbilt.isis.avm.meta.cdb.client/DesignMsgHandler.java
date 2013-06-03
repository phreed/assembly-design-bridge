package edu.vanderbilt.isis.avm.meta.cdb.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// @Sharable
public class DesignMsgHandler extends ChannelInboundMessageHandlerAdapter<ByteBuf> {
    private static final Logger logger = LoggerFactory
            .getLogger(AssemblyDesignBridgeClient.class);

    private final static ByteBuf MSG = Unpooled.copiedBuffer("CDB Client", CharsetUtil.UTF_8);

    /**
     *  This method is called when there is a problem.
     *  The Throwable is logged and the Channel is closed, which means the connection to the server is closed.
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.warn("exception caught", cause);
        ctx.close();
    }

    /**
     * Called each time data is received from the server.
     * It should be noted that the bytes might be fragmented.
     * Fragmenting implies that, for example, if the server writes 5 bytes it is not guarantee that all 5 bytes will be received at once.
     * If could be that for 5 bytes this method could be called twice (or more).
     * The first time it may be called with 3 bytes and the second time with 2 bytes.
     * The only guarantee is that the bytes will be received in the same order as they are sent.
     *
     * @param ctx
     * @param in
     */
    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) {
        logger.info("client received: {}",
                BufUtil.hexDump(in.readableBytes(in.readableBytes())));
    }

    /**
     * Called once the connection to the Servier is established.
     * Register with the distributor.
     * This method will be called once the connection is established.
     * Once the connection is established, a sequence of bytes is promptly sent to the Server.
     * Overriding this method ensures that something is written to the server as soon as possible.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        ctx.write(MSG.duplicate());
    }

    /**
     * Called once the connection to the server is dropped.
     * Unregister with the distributor.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

}