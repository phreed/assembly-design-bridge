package edu.vanderbilt.isis.meta.cdb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.buffer.BufUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This frame decoder scans for the "magic".
 * <table>
 * <tr><th>size (bytes)</th><th>encoding</th><th>purpose</th></tr>
 * <tr><td>4</td><td>0xdeadbeef</td><td>magic indicates start of frame </td></tr>
 * <tr><td>4</td><td>big endian 32 bit integer, bytes</td><td>size of the payload</td></tr>
 * <tr><td>1</td><td>error code</td><td>error</td></tr>
 * <tr><td>1</td><td>8 bit integer, higher is greater</td><td>priority</td></tr>
 * <tr><td>2</td><td>bits</td><td>reserved</td></tr>
 * <tr><td>4</td><td>crc32 checksum</td><td>payload validation</td></tr>
 * <tr><td>4</td><td>crc32 checksum</td><td>header validation (previous 16 bytes)</td></tr>
 * <tr><td>(size of the payload)</td><td>protocol buffer bytes</td><td>payload</td></tr>
 * <tr><td>4</td><td>crc32 checksum</td><td>payload validation (repeated)</td></tr>
 * </table>
 */
public class MagicLengthFrameDecoder extends ByteToMessageDecoder {
    static final int MAGIC_NUMBER = 0xdeadbeef;
    static final byte[] MAGIC_NUMBER_ARRAY;

    static {
        final ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(ByteOrder.BIG_ENDIAN).putInt(MAGIC_NUMBER);
        MAGIC_NUMBER_ARRAY = buff.array();
    }

    private static final Logger logger = LoggerFactory
            .getLogger(MagicLengthFrameDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in,
                          MessageBuf<Object> out) throws Exception {
        if (in.readableBytes() < 20) {
            logger.debug("cannot decode, not a full header yet, only {} bytes of {}",
                    in.readableBytes(), in.capacity());
            return;
        }
        logger.debug("looking for magic in {}", BufUtil.hexDump(in));
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
            logger.debug("payload checksum {}", Integer.toHexString(payloadChecksum));

            final int headerLength = wip.readerIndex() - headerStartPos;
            logger.debug("header length {}", headerLength);
            final byte[] header = new byte[headerLength];
            wip.getBytes(headerStartPos, header);
            logger.debug("raw header {}", BufUtil.hexDump(wip, headerStartPos, headerLength));

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
            logger.debug("payload [{}]:{}", size, Hex.encodeHexString(payload));
            final int payloadChecksumTrailer = wip.readInt();
            logger.debug("payload checksum trailer {}", Integer.toHexString(payloadChecksumTrailer));

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
