package edu.vanderbilt.isis.meta.link;

import edu.vanderbilt.isis.meta.MetaLinkMsg;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public enum DesignMsgDistributor {
    INSTANCE;
    private static final Logger logger = LoggerFactory
            .getLogger(DesignMsgDistributor.class);

    private Map<ChannelHandlerContext, ChannelHandlerContext> channelMap;

    private DesignMsgDistributor() {
        this.channelMap = new HashMap<ChannelHandlerContext, ChannelHandlerContext>();
    }

    /**
     * Send the data to all channels except the incoming channel.
     *
     * @param sourceCtx
     * @param msg
     */
    public void send(ChannelHandlerContext sourceCtx, final MetaLinkMsg.Edit msg) {
        for (final ChannelHandlerContext ctx : this.channelMap.values()) {
            if (ctx.equals(sourceCtx)) {
                continue;
            }
            /*
            if (!(ctx.())) {
                logger.warn("no outbound buffer {}", ctx);
                continue;
            }
            */
            ctx.write(msg);
        }
    }

    public void register(ChannelHandlerContext ctx) {
        logger.info("registration {}", ctx);
        this.channelMap.put(ctx, ctx);
    }

    public void unregister(ChannelHandlerContext ctx) {
        logger.info("unregister {}", ctx);
        this.channelMap.remove(ctx);
    }


}
