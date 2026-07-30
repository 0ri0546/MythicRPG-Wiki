package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import java.util.Optional;

public enum LandMountType {
    COW("cow", "item.mythicrpg.cow_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    GOAT("goat", "item.mythicrpg.goat_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    SPIDER("spider", "item.mythicrpg.spider_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    CHICKEN("chicken", "item.mythicrpg.chicken_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    MOOSHROOM("mooshroom", "item.mythicrpg.mooshroom_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    SHEEP("sheep", "item.mythicrpg.sheep_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    PIG("pig", "item.mythicrpg.pig_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    LLAMA("llama", "item.mythicrpg.llama_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    PANDA("panda", "item.mythicrpg.panda_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    SNOW_GOLEM("snow_golem", "item.mythicrpg.snow_golem_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    TURTLE("turtle", "item.mythicrpg.turtle_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    POLAR_BEAR("polar_bear", "item.mythicrpg.polar_bear_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    HOGLIN("hoglin", "item.mythicrpg.hoglin_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    RAVAGER("ravager", "item.mythicrpg.ravager_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    ZOGLIN("zoglin", "item.mythicrpg.zoglin_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    VILLAGER("villager", "item.mythicrpg.villager_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    ENDERMAN("enderman", "item.mythicrpg.enderman_saddle", BonusType.LAND_MOUNTS, "skill_tree.mythicrpg.traveling.17.name", false),
    PHANTOM("phantom", "item.mythicrpg.phantom_saddle", BonusType.FLYING_MOUNTS, "skill_tree.mythicrpg.traveling.19.name", true),
    BLAZE("blaze", "item.mythicrpg.blaze_saddle", BonusType.FLYING_MOUNTS, "skill_tree.mythicrpg.traveling.19.name", true),
    BREEZE("breeze", "item.mythicrpg.breeze_saddle", BonusType.FLYING_MOUNTS, "skill_tree.mythicrpg.traveling.19.name", true),
    GHAST("ghast", "item.mythicrpg.ghast_saddle", BonusType.FLYING_MOUNTS, "skill_tree.mythicrpg.traveling.19.name", true),
    BEE("bee", "item.mythicrpg.bee_saddle", BonusType.FLYING_MOUNTS, "skill_tree.mythicrpg.traveling.19.name", true);

    private final String id;
    private final String saddleTranslationKey;
    private final BonusType requiredBonus;
    private final String requiredPerkTranslationKey;
    private final boolean flying;

    LandMountType(
            String id,
            String saddleTranslationKey,
            BonusType requiredBonus,
            String requiredPerkTranslationKey,
            boolean flying
    ) {
        this.id = id;
        this.saddleTranslationKey = saddleTranslationKey;
        this.requiredBonus = requiredBonus;
        this.requiredPerkTranslationKey = requiredPerkTranslationKey;
        this.flying = flying;
    }

    public String id() {
        return id;
    }

    public String saddleTranslationKey() {
        return saddleTranslationKey;
    }

    public net.minecraft.text.Text displayName() {
        return net.minecraft.text.Text.translatable("mount.mythicrpg." + id);
    }

    public BonusType requiredBonus() {
        return requiredBonus;
    }

    public net.minecraft.text.Text requiredPerkName() {
        return net.minecraft.text.Text.translatable(requiredPerkTranslationKey);
    }

    public boolean isFlying() {
        return flying;
    }

    /**
     * Food used to heal this adopted mount. Natural vanilla breeding food is
     * preferred; species without one use a representative saddle ingredient.
     */
    public boolean isHealingItem(ItemStack stack) {
        return switch (this) {
            case COW, MOOSHROOM, SHEEP, GOAT -> stack.isOf(Items.WHEAT);
            case SPIDER -> stack.isOf(Items.SPIDER_EYE);
            case CHICKEN -> stack.isOf(Items.WHEAT_SEEDS)
                    || stack.isOf(Items.PUMPKIN_SEEDS)
                    || stack.isOf(Items.MELON_SEEDS)
                    || stack.isOf(Items.BEETROOT_SEEDS);
            case PIG -> stack.isOf(Items.CARROT)
                    || stack.isOf(Items.POTATO)
                    || stack.isOf(Items.BEETROOT);
            case LLAMA -> stack.isOf(Items.WHEAT) || stack.isOf(Items.HAY_BLOCK);
            case PANDA -> stack.isOf(Items.BAMBOO);
            case SNOW_GOLEM -> stack.isOf(Items.SNOWBALL);
            case TURTLE -> stack.isOf(Items.SEAGRASS);
            case POLAR_BEAR -> stack.isOf(Items.COD) || stack.isOf(Items.SALMON);
            case HOGLIN -> stack.isOf(Items.CRIMSON_FUNGUS);
            case RAVAGER -> stack.isOf(Items.EMERALD);
            case ZOGLIN -> stack.isOf(Items.ROTTEN_FLESH);
            case VILLAGER -> stack.isOf(Items.BREAD);
            case ENDERMAN -> stack.isOf(Items.ENDER_PEARL);
            case PHANTOM -> stack.isOf(Items.PHANTOM_MEMBRANE);
            case BLAZE -> stack.isOf(Items.BLAZE_POWDER);
            case BREEZE -> stack.isOf(Items.WIND_CHARGE);
            case GHAST -> stack.isOf(Items.GHAST_TEAR);
            case BEE -> stack.isIn(ItemTags.FLOWERS);
        };
    }

    public boolean matches(MobEntity entity) {
        return switch (this) {
            case COW -> entity instanceof CowEntity && !(entity instanceof MooshroomEntity);
            case GOAT -> entity instanceof GoatEntity;
            case SPIDER -> entity instanceof SpiderEntity;
            case CHICKEN -> entity instanceof ChickenEntity;
            case MOOSHROOM -> entity instanceof MooshroomEntity;
            case SHEEP -> entity instanceof SheepEntity;
            case PIG -> entity instanceof PigEntity;
            case LLAMA -> entity instanceof LlamaEntity;
            case PANDA -> entity instanceof PandaEntity;
            case SNOW_GOLEM -> entity instanceof SnowGolemEntity;
            case TURTLE -> entity instanceof TurtleEntity;
            case POLAR_BEAR -> entity instanceof PolarBearEntity;
            case HOGLIN -> entity instanceof HoglinEntity;
            case RAVAGER -> entity instanceof RavagerEntity;
            case ZOGLIN -> entity instanceof ZoglinEntity;
            case VILLAGER -> entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity;
            case ENDERMAN -> entity instanceof EndermanEntity;
            case PHANTOM -> entity instanceof PhantomEntity;
            case BLAZE -> entity instanceof BlazeEntity;
            case BREEZE -> entity instanceof BreezeEntity;
            case GHAST -> entity instanceof GhastEntity;
            case BEE -> entity instanceof BeeEntity;
        };
    }

    public static Optional<LandMountType> fromEntity(MobEntity entity) {
        if (entity instanceof MooshroomEntity) {
            return Optional.of(MOOSHROOM);
        }
        if (entity instanceof CowEntity) {
            return Optional.of(COW);
        }
        if (entity instanceof GoatEntity) {
            return Optional.of(GOAT);
        }
        if (entity instanceof SpiderEntity) {
            return Optional.of(SPIDER);
        }
        if (entity instanceof ChickenEntity) {
            return Optional.of(CHICKEN);
        }
        if (entity instanceof SheepEntity) {
            return Optional.of(SHEEP);
        }
        if (entity instanceof PigEntity) {
            return Optional.of(PIG);
        }
        if (entity instanceof LlamaEntity) {
            return Optional.of(LLAMA);
        }
        if (entity instanceof PandaEntity) {
            return Optional.of(PANDA);
        }
        if (entity instanceof SnowGolemEntity) {
            return Optional.of(SNOW_GOLEM);
        }
        if (entity instanceof TurtleEntity) {
            return Optional.of(TURTLE);
        }
        if (entity instanceof PolarBearEntity) {
            return Optional.of(POLAR_BEAR);
        }
        if (entity instanceof HoglinEntity) {
            return Optional.of(HOGLIN);
        }
        if (entity instanceof RavagerEntity) {
            return Optional.of(RAVAGER);
        }
        if (entity instanceof ZoglinEntity) {
            return Optional.of(ZOGLIN);
        }
        if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
            return Optional.of(VILLAGER);
        }
        if (entity instanceof EndermanEntity) {
            return Optional.of(ENDERMAN);
        }
        if (entity instanceof PhantomEntity) {
            return Optional.of(PHANTOM);
        }
        if (entity instanceof BlazeEntity) {
            return Optional.of(BLAZE);
        }
        if (entity instanceof BreezeEntity) {
            return Optional.of(BREEZE);
        }
        if (entity instanceof GhastEntity) {
            return Optional.of(GHAST);
        }
        if (entity instanceof BeeEntity) {
            return Optional.of(BEE);
        }
        return Optional.empty();
    }

    public static Optional<LandMountType> fromId(String id) {
        for (LandMountType type : values()) {
            if (type.id.equals(id)) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }
}
