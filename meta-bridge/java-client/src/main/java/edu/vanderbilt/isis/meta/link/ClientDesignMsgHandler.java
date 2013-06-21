package edu.vanderbilt.isis.meta.link;

import edu.vanderbilt.isis.meta.MetaLinkMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// @Sharable
public class ClientDesignMsgHandler extends ChannelInboundMessageHandlerAdapter<MetaLinkMsg.Edit> {
    private static final Logger logger = LoggerFactory
            .getLogger(ClientDesignMsgHandler.class);
    final MetaLinkMsg.Edit message;

    public ClientDesignMsgHandler(final MetaLinkMsg.Edit message) {
        this.message = message;
    }

    @Override
    public void messageReceived(ChannelHandlerContext channelHandlerContext, MetaLinkMsg.Edit in) throws Exception {
        logger.info("message received {}\n{}", Integer.toHexString(in.hashCode()), in);
        for (final MetaLinkMsg.RawPayload item : in.getRawList()) {
            switch (item.getEncoding()) {
                case PROTOBUF: {
                    final MetaLinkMsg.Payload payload = MetaLinkMsg.Payload.newBuilder()
                            .mergeFrom(item.getPayload())
                            .build();
                    logger.info("protobuf {} \n{}", Integer.toHexString(in.hashCode()), payload);
                }

                break;
                case XML:
                    logger.info("xml \n{}", item.getPayload());
                    break;
                default:
                    logger.error("unknown encoding type {}", item.getEncoding());
            }
        }
    }

    /**
     * This method is called when there is a problem.
     * The Throwable is logged and the Channel is closed, which means the connection to the server is closed.
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
     * Called once the connection to the Servier is established.
     * Register with the distributor.
     * This method will be called once the connection is established.
     * Once the connection is established, a sequence of bytes is promptly sent to the Server.
     * Overriding this method ensures that something is written to the server as soon as possible.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("generating sample message\n{}", this.message);
        ctx.write(this.message);
    }

    /**
     * Called once the connection to the server is dropped.
     * Unregister with the distributor.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.warn("channel closed {}", ctx);
    }

}