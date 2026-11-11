package dev.extrahardmode.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.extrahardmode.feature.CouncilMissionCodec;
import dev.extrahardmode.feature.CouncilMissionRules;
import org.junit.jupiter.api.Test;

class CouncilMissionCodecTest {
    @Test
    void dimensionRoundTripsAndOlderSavesStayEmpty() {
        CouncilMissionRules.Mission given = CouncilMissionRules.assign(
                CouncilMissionRules.NONE, "a", "Ada", 12, 3, 0, 0, CouncilMissionRules.NETHER);
        JsonObject encoded = object(CouncilMissionCodec.CODEC
                .encodeStart(JsonOps.INSTANCE, given)
                .result()
                .orElseThrow());
        assertEquals(CouncilMissionRules.NETHER, encoded.get("dimension").getAsString());
        assertFalse(encoded.has("accepted"));
        assertTrue(given.accepted());
        CouncilMissionRules.Mission decoded = CouncilMissionCodec.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .result()
                .orElseThrow();
        assertEquals(given, decoded);

        JsonObject older = encoded.deepCopy();
        older.remove("dimension");
        CouncilMissionRules.Mission legacy = CouncilMissionCodec.CODEC
                .parse(JsonOps.INSTANCE, older)
                .result()
                .orElseThrow();
        assertEquals("", legacy.dimension());
        assertTrue(legacy.accepted());
        assertEquals(given.mobId(), legacy.mobId());
        assertTrue(CouncilMissionRules.spawnsIn(legacy, CouncilMissionRules.NETHER));
        assertFalse(CouncilMissionRules.spawnsIn(legacy, CouncilMissionRules.OVERWORLD));

        JsonObject bare = object(CouncilMissionCodec.CODEC
                .encodeStart(JsonOps.INSTANCE, given.withDimension(""))
                .result()
                .orElseThrow());
        assertFalse(bare.has("dimension"));

        CouncilMissionRules.Mission offer = given.asOffer();
        JsonObject waiting = object(CouncilMissionCodec.CODEC
                .encodeStart(JsonOps.INSTANCE, offer)
                .result()
                .orElseThrow());
        assertFalse(waiting.get("accepted").getAsBoolean());
        assertEquals(offer, CouncilMissionCodec.CODEC.parse(JsonOps.INSTANCE, waiting).result().orElseThrow());
        JsonObject olderOffer = waiting.deepCopy();
        olderOffer.remove("accepted");
        CouncilMissionRules.Mission alreadyGiven =
                CouncilMissionCodec.CODEC.parse(JsonOps.INSTANCE, olderOffer).result().orElseThrow();
        assertTrue(alreadyGiven.accepted());
        assertTrue(alreadyGiven.active());
    }

    private static JsonObject object(JsonElement element) {
        assertTrue(element.isJsonObject());
        return element.getAsJsonObject();
    }
}
