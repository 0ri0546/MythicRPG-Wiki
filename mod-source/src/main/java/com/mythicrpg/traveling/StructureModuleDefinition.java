package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.List;

public record StructureModuleDefinition(
        String id,
        String translationKey,
        Realm realm,
        List<RegistryKey<Structure>> structures
) {

    public boolean isUsableIn(RegistryKey<World> dimension) {
        return realm.matches(dimension);
    }

    public BonusType requiredBonus() {
        return realm == Realm.OVERWORLD
                ? BonusType.STRUCTURE_MODULES_OVERWORLD
                : BonusType.STRUCTURE_MODULES_NETHER_END;
    }

    public enum Realm {
        OVERWORLD,
        NETHER,
        END;

        public boolean matches(RegistryKey<World> dimension) {
            return switch (this) {
                case OVERWORLD -> World.OVERWORLD.equals(dimension);
                case NETHER -> World.NETHER.equals(dimension);
                case END -> World.END.equals(dimension);
            };
        }

        public String translationKey() {
            return "tooltip.mythicrpg.structure_module.realm." + name().toLowerCase();
        }
    }
}
