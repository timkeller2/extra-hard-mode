package dev.extrahardmode.feature;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Rewrites an ordinary fletcher's 32-stick emerald buy into 32 flint. Residents keep their own shops. */
public final class FletcherTrades {
    private FletcherTrades() {}

    public static void apply(Villager villager) {
        if (villager == null || !villager.getVillagerData().profession().is(VillagerProfession.FLETCHER)) {
            return;
        }
        MerchantOffers offers = villager.getOffers();
        if (offers == null) {
            return;
        }
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer != null && isStickBuy(offer)) {
                offers.set(i, flintBuy(offer));
            }
        }
    }

    static boolean isStickBuy(MerchantOffer offer) {
        ItemStack cost = offer.getBaseCostA();
        ItemStack result = offer.getResult();
        return FletcherTradeRules.isNoviceStickBuy(
                cost != null && cost.is(Items.STICK),
                cost == null ? 0 : cost.getCount(),
                offer.getItemCostB().isPresent(),
                result != null && result.is(Items.EMERALD),
                result == null ? 0 : result.getCount());
    }

    /** Same uses, demand, experience, and price multiplier. Only the first cost item changes. */
    static MerchantOffer flintBuy(MerchantOffer offer) {
        MerchantOffer replaced = new MerchantOffer(
                new ItemCost(Items.FLINT, FletcherTradeRules.COUNT),
                offer.getItemCostB(),
                offer.getResult().copy(),
                offer.getUses(),
                offer.getMaxUses(),
                offer.getXp(),
                offer.getPriceMultiplier(),
                offer.getDemand());
        replaced.setSpecialPriceDiff(offer.getSpecialPriceDiff());
        return replaced;
    }
}
