package dev.extrahardmode.feature;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Rewrites an ordinary librarian's enchanted books to the enchantments that rank may sell, capped at that rank. */
public final class LibrarianBooks {
    private LibrarianBooks() {}

    public static void apply(Villager villager) {
        if (villager == null || !villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
            return;
        }
        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }
        int rank = villager.getVillagerData().level();
        List<Holder<Enchantment>> allowed = allowedEnchantments(villager, rank);
        if (allowed.isEmpty()) {
            return;
        }
        RandomSource random = villager.getRandom();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer == null || !offer.getResult().is(Items.ENCHANTED_BOOK)) {
                continue;
            }
            MerchantOffer fixed = fix(offer, rank, allowed, random);
            if (fixed != null) {
                offers.set(i, fixed);
            }
        }
    }

    static List<Holder<Enchantment>> allowedEnchantments(Villager villager, int rank) {
        Optional<HolderSet.Named<Enchantment>> traded = villager
                .level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(EnchantmentTags.TRADEABLE);
        List<Holder<Enchantment>> allowed = new ArrayList<>();
        if (traded.isEmpty()) {
            return allowed;
        }
        for (Holder<Enchantment> holder : traded.get()) {
            Enchantment enchantment = holder.value();
            if (LibrarianBookRules.enchantmentAllowed(rank, enchantment.getMaxLevel())
                    && LibrarianBookRules.levelCeiling(rank, enchantment.getMinLevel(), enchantment.getMaxLevel())
                            > 0) {
                allowed.add(holder);
            }
        }
        return allowed;
    }

    /** Null when the book already fits this rank. */
    static MerchantOffer fix(
            MerchantOffer offer, int rank, List<Holder<Enchantment>> allowed, RandomSource random) {
        ItemEnchantments stored = EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult());
        if (stored.size() == 1) {
            Object2IntMap.Entry<Holder<Enchantment>> entry = stored.entrySet().iterator().next();
            Holder<Enchantment> holder = entry.getKey();
            Enchantment enchantment = holder.value();
            int rolled = entry.getIntValue();
            if (LibrarianBookRules.enchantmentAllowed(rank, enchantment.getMaxLevel())) {
                int level = LibrarianBookRules.bookLevel(
                        rank, enchantment.getMinLevel(), enchantment.getMaxLevel(), rolled);
                if (level == rolled && level > 0) {
                    return null;
                }
                if (level > 0) {
                    return replace(offer, holder, level, random);
                }
            }
        }
        if (allowed.isEmpty()) {
            return null;
        }
        Holder<Enchantment> picked = allowed.get(random.nextInt(allowed.size()));
        Enchantment enchantment = picked.value();
        int ceiling = LibrarianBookRules.levelCeiling(rank, enchantment.getMinLevel(), enchantment.getMaxLevel());
        int floor = Math.max(1, enchantment.getMinLevel());
        int level = floor + random.nextInt(ceiling - floor + 1);
        return replace(offer, picked, level, random);
    }

    private static MerchantOffer replace(
            MerchantOffer offer, Holder<Enchantment> enchantment, int level, RandomSource random) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(enchantment, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        boolean treasure = enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE);
        int roll = random.nextInt(LibrarianBookRules.priceRollBound(level));
        int emeralds = LibrarianBookRules.emeraldCost(level, roll, treasure);
        MerchantOffer replaced = new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds),
                offer.getItemCostB(),
                book,
                offer.getUses(),
                offer.getMaxUses(),
                offer.getXp(),
                offer.getPriceMultiplier(),
                offer.getDemand());
        replaced.setSpecialPriceDiff(offer.getSpecialPriceDiff());
        return replaced;
    }
}
