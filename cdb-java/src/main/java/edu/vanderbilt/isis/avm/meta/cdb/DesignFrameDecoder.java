package edu.vanderbilt.isis.avm.meta.cdb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This frame decoder scans for the "magic".
 */
public class DesignFrameDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory
            .getLogger(DesignFrameDecoder.class);

    static final int MAGIC_NUMBER = 0xdeadbeef;
    static final byte[] MAGIC_NUMBER_ARRAY;

    static {
        final ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(ByteOrder.BIG_ENDIAN).putInt(MAGIC_NUMBER);
        MAGIC_NUMBER_ARRAY = buff.array();
    }

    private static String toHexString(final byte[] byteArray) {
        StringBuffer stringBuffer = new StringBuffer("[ ");
        for (final byte byte_var : byteArray) {
            stringBuffer.append(String.format("%02X ", byte_var));
        }
        return stringBuffer.append("]").toString();
    }


    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in,
                          MessageBuf<Object> out) throws Exception {
        logger.debug("top {}", in);
        if (in.readableBytes() < 20) {
            logger.debug("not a full header yet, only {} bytes of {}",
                    in.readableBytes(), in.capacity());
            return;
        }
        logger.debug("looking for magic");
        while (0 < in.readableBytes()) {
            if (in.getByte(in.readerIndex()) != MAGIC_NUMBER_ARRAY[0]) {
                final byte notMagic = in.readByte();
                logger.debug("not magic {}", String.format("%02X ", notMagic));
                continue;
            }
            final ByteBuf wip = in.order(ByteOrder.BIG_ENDIAN);
            if (wip.readableBytes() < 20) {
                logger.debug("not even a full header yet, only {}:{} bytes",
                        in.readableBytes(), wip.readableBytes());
                return;
            }
            final int headerStartPos = wip.readerIndex();
            logger.debug("header start position {}", headerStartPos);

            final int magicNumber = wip.readInt();
            if (magicNumber != MAGIC_NUMBER) {
                logger.error("Magic number mismatch: {} != {} (expected)",
                        Integer.toHexString(magicNumber), Integer.toHexString(MAGIC_NUMBER));
                continue;
            }
            logger.trace("skip the magic");
            in.readerIndex(wip.readerIndex());

            final int size = wip.readInt();

            @SuppressWarnings("unused")
            final byte priority = wip.readByte();
            @SuppressWarnings("unused")
            final byte error = wip.readByte();

            /** two reserved bytes; not used */
            wip.readBytes(2);

            final int payloadChecksum = wip.readInt();

            final int headerEndPos = wip.readerIndex();
            logger.debug("header end position {}", headerEndPos);
            final byte[] header = new byte[headerEndPos - headerStartPos];
            wip.getBytes(headerStartPos, header);
            logger.debug("raw header {}", toHexString(header));

            logger.trace("verify header checksum");
            final CRC32 crc = new CRC32();
            crc.update(header, 0, header.length);
            final int computedHeaderChecksum = (int) crc.getValue();

            final int headerChecksum = wip.readInt();
            if (headerChecksum != computedHeaderChecksum) {
                logger.error("Header checksum mismatch: {} (passed) != {} (computed)",
                        Integer.toHexString(headerChecksum), Integer.toHexString(computedHeaderChecksum));
                continue;
            }
            logger.debug("header checksum {}", Integer.toHexString(headerChecksum));

            if (wip.readableBytes() < size) {
                logger.debug("not enough data to continue: {} < {}", wip.readableBytes(), size);
                return;
            }

            final byte[] payload = new byte[size];
            wip.readBytes(payload, 0, size);
            logger.debug("read payload [{}]:{}", size, toHexString(payload));
            final int payloadChecksumTrailer = wip.readInt();
            logger.debug("read payload {}", Integer.toHexString(payloadChecksumTrailer));

            final CRC32 dataCrc = new CRC32();
            dataCrc.update(payload);
            int computedPayloadChecksum = (int) dataCrc.getValue();

            if (payloadChecksum != computedPayloadChecksum || payloadChecksum != payloadChecksumTrailer) {
                logger.error("Payload checksum mismatch: passed[{}] computed[{}] trailer[{}])",
                        Integer.toHexString(payloadChecksum),
                        Integer.toHexString(computedPayloadChecksum),
                        Integer.toHexString(payloadChecksumTrailer));
            }

            in.readerIndex(wip.readerIndex());
            out.add(Unpooled.wrappedBuffer(payload));
        }
    }

}