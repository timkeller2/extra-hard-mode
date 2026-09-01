package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.tag.EhmTags;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * Optional POST-1.0 module, default off (not original EHM).
 *
 * <p>26.2 still uses datapack trades ({@code data/minecraft/villager_trade/},
 * tags, {@code trade_set/}). Static overrides cannot honor WorldGate, so this
 * filters generated {@link MerchantOffer}s at runtime.
 *
 * <p>Diamond-gear result keys (26.2 format 107.1) that this removes:
 * {@code armorer/4/emerald_enchanted_diamond_boots},
 * {@code armorer/4/emerald_enchanted_diamond_leggings},
 * {@code armorer/5/emerald_enchanted_diamond_chestplate},
 * {@code armorer/5/emerald_enchanted_diamond_helmet},
 * {@code toolsmith/3/emerald_diamond_hoe},
 * {@code toolsmith/4/emerald_enchanted_diamond_axe},
 * {@code toolsmith/4/emerald_enchanted_diamond_shovel},
 * {@code toolsmith/5/emerald_enchanted_diamond_pickaxe},
 * {@code weaponsmith/4/emerald_enchanted_diamond_axe},
 * {@code weaponsmith/5/emerald_enchanted_diamond_sword}.
 * Buying diamonds ({@code diamond_emerald}) is kept — result is an emerald.
 *
 * <p>Librarian enchanted-book keys (Mending is a random {@code #minecraft:tradeable}
 * roll, not its own file):
 * {@code librarian/1/emerald_and_book_enchanted_book},
 * {@code librarian/2/emerald_and_book_enchanted_book},
 * {@code librarian/3/emerald_and_book_enchanted_book},
 * {@code librarian/4/emerald_book_and_enchanted_book}.
 * Novice–apprentice (levels 1–2) Mending is stripped; Master may sell it at
 * {@link #MASTER_MENDING_EMERALDS} (2× a typical ~10 emerald treasure roll).
 *
 * <p>Farmer crops, cartographer maps, and copper-age trades are not touched.
 */
public final class VillagerNerf implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("villager_nerf");

    /** 2× typical vanilla treasure Mending (~10 emeralds). Capped well below 64. */
    public static final int MASTER_MENDING_EMERALDS = 20;

    public static final int NOVICE = 1;
    public static final int APPRENTICE = 2;
    public static final int MASTER = 5;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    public static void apply(Villager villager) {
        Level level = villager.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        apply(villager, villager.getOffers(), ConfigManager.world(serverLevel));
    }

    public static void apply(Villager villager, MerchantOffers offers, WorldConfig config) {
        if (offers == null) {
            return;
        }
        int villagerLevel = villager.getVillagerData().level();
        boolean librarian = villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN);
        boolean blockDiamond = config.villagerNerfBlockDiamondGear();
        boolean nerfMending = config.villagerNerfNoviceMending();
        Holder<Enchantment> mending = nerfMending ? mending(villager) : null;

        apply(
                offers,
                offer -> isDiamondGearResult(offer.getResult()),
                offer -> isMendingBook(offer.getResult(), mending),
                villagerLevel,
                librarian,
                blockDiamond,
                nerfMending,
                () -> masterMendingOffer(mending));
    }

    /**
     * Mutates {@code offers} in place. Production {@link #apply(Villager, MerchantOffers, WorldConfig)}
     * and unit tests share this so diamond-pick / wheat / Mending rows are not tautologies of
     * {@link #shouldDropOffer}.
     */
    public static <T> void apply(
            List<T> offers,
            Predicate<T> diamondGear,
            Predicate<T> mendingBook,
            int villagerLevel,
            boolean librarian,
            boolean blockDiamondGear,
            boolean nerfNoviceMending,
            Supplier<T> masterMending) {
        if (offers == null) {
            return;
        }
        offers.removeIf(offer -> shouldDropOffer(
                diamondGear.test(offer),
                mendingBook.test(offer),
                villagerLevel,
                librarian,
                blockDiamondGear,
                nerfNoviceMending));
        boolean hasMending = false;
        for (T offer : offers) {
            if (mendingBook.test(offer)) {
                hasMending = true;
                break;
            }
        }
        if (shouldOfferMasterMending(librarian, villagerLevel, hasMending, nerfNoviceMending)) {
            offers.add(masterMending.get());
        }
    }

    public static boolean shouldDropOffer(
            boolean diamondGearResult,
            boolean mendingBook,
            int villagerLevel,
            boolean librarian,
            boolean blockDiamondGear,
            boolean nerfNoviceMending) {
        if (blockDiamondGear && diamondGearResult) {
            return true;
        }
        return nerfNoviceMending && librarian && mendingBook && villagerLevel <= APPRENTICE;
    }

    public static boolean shouldOfferMasterMending(
            boolean librarian, int villagerLevel, boolean alreadyHasMending, boolean nerfNoviceMending) {
        return nerfNoviceMending && librarian && villagerLevel >= MASTER && !alreadyHasMending;
    }

    public static boolean isDiamondGearResult(ItemStack result) {
        return result != null && !result.isEmpty() && result.is(EhmTags.VILLAGER_NERF_GEAR);
    }

    static boolean isMendingBook(ItemStack result, Holder<Enchantment> mending) {
        if (mending == null || result == null || result.isEmpty() || !result.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        return EnchantmentHelper.getEnchantmentsForCrafting(result).getLevel(mending) > 0;
    }

    private static Holder<Enchantment> mending(Villager villager) {
        return villager.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.MENDING);
    }

    private static MerchantOffer masterMendingOffer(Holder<Enchantment> mending) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(mending, 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, MASTER_MENDING_EMERALDS),
                Optional.of(new ItemCost(Items.BOOK)),
                book,
                12,
                30,
                0.2f);
    }
}
