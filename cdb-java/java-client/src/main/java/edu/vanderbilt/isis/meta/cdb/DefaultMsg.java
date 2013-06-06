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

    final CdbMsg.Control message;

    private DefaultMsg() {

        final AssemblyInterface.CADComponentType cadComponentType =
                AssemblyInterface.CADComponentType.newBuilder()
                .setComponentID("{bdd0008c4-4149-40ab-a6980e84ab00afd3}")
                .setName("FuelTank12345")
                .build();

        final CdbMsg.Payload payload = CdbMsg.Payload.newBuilder()
        .setCadComponent(cadComponentType)
                .build();

        final CdbMsg.PayloadRaw payloadRaw = CdbMsg.PayloadRaw.newBuilder()
             .setEncoding(CdbMsg.PayloadRaw.EncodingType.PB)
                .setPayload(payload.toByteString())
                .build();

        this.message =  CdbMsg.Control.newBuilder()
        .setAction(CdbMsg.Control.ActionType.UPDATE)
                .addTopic("cdb")
                .addPayload(payloadRaw)
        .build();
    }


    public String asString() {
        return "type: UPDATE\n" +
                "cadComponent {\n" +
                "  ComponentID: \"{bdd0008c4-4149-40ab-a6980e84ab00afd3}\"\n" +
                "  Name: \"FuelTank12345\"\n" +
                "}" ;
    }


    public CdbMsg.Control asMessage() {
              return this.message;
    }
}
