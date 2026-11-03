package dev.extrahardmode.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class ClientboundSyncPayloadTest {
    @Test
    void weightBitsRoundTripAndOlderPacketsStillMatchInactive() {
        ClientboundSyncPayload inactive = ClientboundSyncPayload.inactive();
        assertFalse(inactive.worldActive());
        ByteBuf buf = Unpooled.buffer();
        try {
            ClientboundSyncPayload.STREAM_CODEC.encode(buf, inactive);
            int full = buf.readableBytes();
            assertEquals(inactive, ClientboundSyncPayload.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());

            buf.clear();
            ClientboundSyncPayload.STREAM_CODEC.encode(buf, inactive);
            buf.writerIndex(full - ClientboundSyncPayload.DisplayExtras.WeightBits.ENCODED_BYTES);
            ClientboundSyncPayload older = ClientboundSyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(0, buf.readableBytes());
            assertEquals(inactive, older);
            assertTrue(older.extras().weight().enabled());
            assertEquals(18.0, older.extras().weight().maxPoints());
        } finally {
            buf.release();
        }
    }
}
