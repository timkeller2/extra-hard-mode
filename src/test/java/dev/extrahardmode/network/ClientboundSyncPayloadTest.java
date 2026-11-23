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

            ClientboundSyncPayload custom = inactive.withExtras(inactive.extras().withCropBounds(10, 40));
            buf.clear();
            ClientboundSyncPayload.STREAM_CODEC.encode(buf, custom);
            ClientboundSyncPayload customRead = ClientboundSyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(custom, customRead);
            assertEquals(10, customRead.extras().minCropLoss());
            assertEquals(40, customRead.extras().maxCropLoss());
            assertFalse(customRead.extras().legacySeasonCycle());

            buf.clear();
            ClientboundSyncPayload.STREAM_CODEC.encode(buf, inactive);
            buf.writerIndex(full - ClientboundSyncPayload.DisplayExtras.CROP_LOSS_BYTES);
            ClientboundSyncPayload previous = ClientboundSyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(0, buf.readableBytes());
            assertTrue(previous.extras().legacySeasonCycle());
            assertFalse(previous.worldActive());
            assertTrue(previous.extras().weight().enabled());
            assertEquals(18.0, previous.extras().weight().maxPoints());

            buf.clear();
            ClientboundSyncPayload.STREAM_CODEC.encode(buf, inactive);
            buf.writerIndex(full
                    - ClientboundSyncPayload.DisplayExtras.CROP_LOSS_BYTES
                    - ClientboundSyncPayload.DisplayExtras.WeightBits.ENCODED_BYTES);
            ClientboundSyncPayload older = ClientboundSyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(0, buf.readableBytes());
            assertTrue(older.extras().legacySeasonCycle());
            assertFalse(older.worldActive());
            assertTrue(older.extras().weight().enabled());
            assertEquals(18.0, older.extras().weight().maxPoints());
        } finally {
            buf.release();
        }
    }
}
