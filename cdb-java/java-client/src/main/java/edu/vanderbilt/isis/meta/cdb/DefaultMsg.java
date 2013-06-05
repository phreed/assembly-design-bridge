package edu.vanderbilt.isis.meta.cdb;

import com.google.protobuf.TextFormat;
import edu.vanderbilt.isis.meta.AssemblyInterface;
import edu.vanderbilt.isis.meta.CdbMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * User: Fred Eisele
 * Date: 6/4/13
 * Time: 4:20 PM
 */
public enum DefaultMsg {
    INSTANCE;
    private static final Logger logger = LoggerFactory
            .getLogger(DefaultMsg.class);

    final CdbMsg.Message payload;
    private DefaultMsg() {
        final CdbMsg.Message.Builder builder =  CdbMsg.Message.newBuilder();
        this.payload = builder.setType(CdbMsg.Message.MessageType.UPDATE)
                .setCadComponent(
                        AssemblyInterface.CADComponentType.newBuilder()
                        .setComponentID("{bdd0008c4-4149-40ab-a6980e84ab00afd3}")
                        .setName("FuelTank12345")
                        .build()
                )
        .build();
    }

    /*
    public byte[] asByteArray() {
        try {
            return TextFormat.print(this.payload);
        } catch (UnsupportedEncodingException ex) {
            logger.error("bad message {}", ex);
        }
        return null;
    }

    public String asString() {
        return this.payload;
    }
    */

    public CdbMsg.Message asMessage() {
              return this.payload;
    }
}
