package edu.vanderbilt.isis.avm.meta.cdb;

import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;

public enum DesignMsgDistributor {
	INSTANCE;
	
	private Map<ChannelHandlerContext, ChannelHandlerContext> channelMap;
	
	private DesignMsgDistributor() {
		this.channelMap = new HashMap<ChannelHandlerContext, ChannelHandlerContext>();
	}

	/**
	 * Send the data to all channels except the incoming channel.
	 * 
	 * @param sourceCtx
	 * @param data
	 */
	public void send(ChannelHandlerContext sourceCtx, byte[] data) {
		for (final ChannelHandlerContext ctx : this.channelMap.values()) {
			if (ctx.equals(sourceCtx)) continue;
			ctx.outboundMessageBuffer().add(data);
		}
	}

	public void register(ChannelHandlerContext ctx) {
		this.channelMap.put(ctx, ctx);
	}

	public void unregister(ChannelHandlerContext ctx) {
		this.channelMap.remove(ctx);
	}



}
