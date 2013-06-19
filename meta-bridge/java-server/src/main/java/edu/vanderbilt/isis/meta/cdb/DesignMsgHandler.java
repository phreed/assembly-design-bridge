package edu.vanderbilt.isis.meta.cdb;

import edu.vanderbilt.isis.meta.MetaLinkMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DesignMsgHandler extends ChannelInboundMessageHandlerAdapter<MetaLinkMsg.Edit> {
    private static final Logger logger = LoggerFactory
            .getLogger(AssemblyDesignBridgeServer.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.warn("exception caught", cause);
        ctx.close();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MetaLinkMsg.Edit req) {
        logger.debug("handling \n{}", req);
        DesignMsgDistributor.INSTANCE.send(ctx, req);
        /*
        final MetaLinkMsg.Message res = MetaLinkMsg.Message.newBuilder()
                .setType(req.getType())
                .setCadComponent(req.getCadComponent())
                .build();
        ctx.write(res);
        */
    }

    /**
     * Register with the distributor.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("channel activated {}", ctx);
        DesignMsgDistributor.INSTANCE.register(ctx);
    }

    /**
     * Unregister with the distributor.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channel deactivated {}", ctx);
        DesignMsgDistributor.INSTANCE.unregister(ctx);
        super.channelInactive(ctx);
    }

}