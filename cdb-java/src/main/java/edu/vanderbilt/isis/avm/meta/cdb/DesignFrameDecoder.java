package edu.vanderbilt.isis.avm.meta.cdb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteOrder;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class DesignFrameDecoder extends ByteToMessageDecoder {
	private static final Logger logger = LoggerFactory
			.getLogger(DesignFrameDecoder.class);
	static final int MAGIC_NUMBER = 0xdeadbeef;

	private String toHexString( byte[] byteArray) {
		StringBuffer stringBuffer = new StringBuffer();
		for( byte byte_var : byteArray ) {
			stringBuffer.append( Integer.toHexString( byte_var ) );
		}
		return stringBuffer.toString();
	}
	
	@Override
	protected void decode(ChannelHandlerContext arg0, ByteBuf in,
			MessageBuf<Object> out) throws Exception {

		if (in.readableBytes() < 20) {
			logger.debug("haven't gotten a full header yet {}",
					in.readableBytes());
			return;
		}

		in.markReaderIndex();

		final byte[] header = new byte[20];

		in.readBytes(header, 0, 20);

		final ByteBuf headerBuf = Unpooled.wrappedBuffer(header);
		final ByteBuf headerBufBigEndian = headerBuf
				.order(ByteOrder.BIG_ENDIAN);

		final int magicNumber = headerBufBigEndian.readInt();
		final int size = headerBufBigEndian.readInt();
		
		@SuppressWarnings("unused")
		final byte priority = headerBufBigEndian.readByte();
		@SuppressWarnings("unused")
		final byte error = headerBufBigEndian.readByte();
		
		/** two reserved bytes; not used */
		headerBufBigEndian.readBytes(2); 
		
		final int payloadChecksum = headerBufBigEndian.readInt();
		final int headerChecksum = headerBufBigEndian.readInt();

		logger.trace("verify header checksum");
		final CRC32 crc = new CRC32();
		crc.update(header, 0, 16);
		final int expectedChecksum = (int) crc.getValue();
		if (magicNumber != MAGIC_NUMBER) {
			logger.error("Magic number mismatch: {} != {} (expected)",
					Integer.toHexString( magicNumber ), Integer.toHexString( MAGIC_NUMBER ));
			return;
		} else {
			logger.debug("Got magic number {}",
					Integer.toHexString( magicNumber ));			
		}
		if (headerChecksum != expectedChecksum) {
			logger.error("Header checksum mismatch: {} != {} (expected)",
					Integer.toHexString( headerChecksum ), Integer.toHexString( expectedChecksum ));
		} else {
			logger.debug("Got header checksum {}",
					Integer.toHexString( headerChecksum ));			
		}
		
		if (in.readableBytes() < size) {
			logger.debug("not enough data to continue: {} < {}", in.readableBytes(), size);
			in.resetReaderIndex();
			return;
		}

		final byte[] data = new byte[size];

		in.readBytes(data, 0, size);
		logger.debug("Read data {}", size);

		final CRC32 dataCrc = new CRC32();
		dataCrc.update(data);
		int expectedDataChecksum = (int) dataCrc.getValue();

		if (payloadChecksum != expectedDataChecksum) {
			logger.error("Payload checksum mismatch: {} != {} (expected)",
					Integer.toHexString( payloadChecksum ), Integer.toHexString( expectedDataChecksum ));
		} else {
			logger.debug("Got payload checksum {}",
					Integer.toHexString( payloadChecksum ));			
		}
		out.add(Unpooled.wrappedBuffer(data));
		logger.debug("Payload is as follows: {}", toHexString(data));
		

	}

}