package edu.vanderbilt.isis.meta.cdb;

import com.google.protobuf.TextFormat;
import edu.vanderbilt.isis.meta.AssemblyInterface;
import edu.vanderbilt.isis.meta.MetaLinkMsg;
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

    final MetaLinkMsg.Edit message;

    private DefaultMsg() {

        final AssemblyInterface.CADComponentType cadComponentType =
                AssemblyInterface.CADComponentType.newBuilder()
                        .setComponentID("{bdd0008c4-4149-40ab-a6980e84ab00afd3}")
                        .setName("FuelTank12345")
                        .build();

        final MetaLinkMsg.Payload payload =
                MetaLinkMsg.Payload.newBuilder()
                        .addCadComponent(cadComponentType)
                        .build();

        final MetaLinkMsg.RawPayload rawPayload =
                MetaLinkMsg.RawPayload.newBuilder()
                        .setEncoding(MetaLinkMsg.RawPayload.EncodingType.PROTOBUF)
                        .setPayload(payload.toByteString())
                        .build();

        this.message =
                MetaLinkMsg.Edit.newBuilder()
                        .setAction(MetaLinkMsg.Edit.ActionType.EDIT)
                        .addTopic("cdb")
                        .addRaw(rawPayload)
                        .build();
    }


    public String asString() {
        return "type: UPDATE\n" +
                "cadComponent {\n" +
                "  ComponentID: \"{bdd0008c4-4149-40ab-a6980e84ab00afd3}\"\n" +
                "  Name: \"FuelTank12345\"\n" +
                "}";
    }


    public MetaLinkMsg.Edit asMessage() {
        return this.message;
    }
}
