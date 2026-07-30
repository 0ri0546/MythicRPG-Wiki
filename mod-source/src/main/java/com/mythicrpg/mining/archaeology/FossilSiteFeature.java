package com.mythicrpg.mining.archaeology;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Generates compact sites using the shared fossil site generator. */
public final class FossilSiteFeature extends Feature<DefaultFeatureConfig> {

    public FossilSiteFeature() {
        super(DefaultFeatureConfig.CODEC);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        FossilFamily family = FossilFamily.random(random);
        FossilRarity dominantRarity = FossilRarity.rollGeneration(random);
        int desiredCount = FossilSiteGenerator.MIN_SITE_SIZE
                + random.nextInt(FossilSiteGenerator.MAX_SITE_SIZE - FossilSiteGenerator.MIN_SITE_SIZE + 1);

        BlockPos origin = context.getOrigin();
        if (tryGenerate(world, origin, family, dominantRarity, desiredCount, random)) {
            return true;
        }

        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos candidate = origin.add(
                    random.nextBetween(-3, 3),
                    random.nextBetween(-3, 3),
                    random.nextBetween(-3, 3)
            );
            if (tryGenerate(world, candidate, family, dominantRarity, desiredCount, random)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryGenerate(
            StructureWorldAccess world,
            BlockPos seed,
            FossilFamily family,
            FossilRarity dominantRarity,
            int desiredCount,
            Random random
    ) {
        return FossilSiteGenerator.generateAt(
                world,
                seed,
                family,
                dominantRarity,
                desiredCount,
                random
        ).isPresent();
    }
}
