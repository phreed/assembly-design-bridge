package edu.vanderbilt.isis.meta.cdb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteOrder;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DesignFrameEncoder extends MessageToByteEncoder<ByteBuf> {
	private static final Logger logger = LoggerFactory
			.getLogger(DesignFrameEncoder.class);
	
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf in, ByteBuf out) throws Exception {
       
        logger.trace("generate the header");
        final ByteBuf headerBuf = Unpooled.buffer(20);
        final ByteBuf headerBufLittleEndian = headerBuf.order(ByteOrder.BIG_ENDIAN);
        headerBufLittleEndian.writeInt(DesignFrameDecoder.MAGIC_NUMBER);       //Magic number
        //headerBufLittleEndian.writeInt(in.length);                       //Data length (not including header)
        //headerBufLittleEndian.writeByte(in.getMessagePriority());     //Message priority
        headerBufLittleEndian.writeByte(0);                                       //Error code
        headerBufLittleEndian.writeByte(0);                                       //Reserved byte
        headerBufLittleEndian.writeByte(0);                                       //Reserved byte

        final CRC32 dataCrc = new CRC32();
        //dataCrc.update(messageData);
        headerBufLittleEndian.writeInt((int) dataCrc.getValue());                 //Data CRC32

        final CRC32 headerCrc = new CRC32();
        headerCrc.update(headerBufLittleEndian.array(), 0, 16);
        headerBufLittleEndian.writeInt((int) headerCrc.getValue());               //Header CRC32 (all 16 bytes of the header, not including itself)

        //send the header, then send the data
        //byteBuf.writeBytes(headerBufLittleEndian);
        //byteBuf.writeBytes(messageData);
    }
}