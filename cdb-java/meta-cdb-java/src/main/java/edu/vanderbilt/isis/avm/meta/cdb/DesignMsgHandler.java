package edu.vanderbilt.isis.avm.meta.cdb;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


  public class DesignMsgHandler extends ChannelInboundMessageHandlerAdapter<ByteBuf> {
	  private static final Logger logger = LoggerFactory
				.getLogger(AssemblyDesignBridgeServer.class);
	  
     @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
         cause.printStackTrace();
         logger.warn("exception caught");
         ctx.close();
      }

	@Override
	public void messageReceived(ChannelHandlerContext ctx, ByteBuf in) {
		
		while(in.isReadable()) {
			final int cnt = in.readableBytes();
			final byte[] data = new byte[cnt];
			in.readBytes(data);
			logger.debug("in : {}", data);
			
			DesignMsgDistributor.INSTANCE.send(ctx, data);
		}
	}
	
	/** 
	 * Register with the distributor.
	 * 
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		
		DesignMsgDistributor.INSTANCE.register(ctx);
	}
	
	/**
	 * Unregister with the distributor.
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		
		DesignMsgDistributor.INSTANCE.unregister(ctx);
	}

 }