package com.mythicrpg.mining.archaeology;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FossilDebugCommands {

    private static final int TEST_SITE_HORIZONTAL_SEARCH = 12;
    private static final int TEST_SITE_VERTICAL_SEARCH = 16;
    private static final int TEST_GRAND_SITE_ATTEMPTS = 128;

    private FossilDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("mythicrpg")
                        .then(CommandManager.literal("locate")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("fossil_site")
                                        .executes(context -> locateNearestSmall(
                                                context.getSource(), null, null, false
                                        ))
                                        .then(CommandManager.literal("all")
                                                .executes(context -> locateNearestSmall(
                                                        context.getSource(), null, null, true
                                                ))
                                        )
                                        .then(CommandManager.argument("family", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        FossilFamily.ids(), builder
                                                ))
                                                .executes(context -> locateSmallWithArguments(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "family"),
                                                        null
                                                ))
                                                .then(CommandManager.argument("rarity", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                FossilRarity.ids(), builder
                                                        ))
                                                        .executes(context -> locateSmallWithArguments(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "family"),
                                                                StringArgumentType.getString(context, "rarity")
                                                        ))
                                                )
                                        )
                                )
                                .then(CommandManager.literal("grand_fossil_site")
                                        .executes(context -> locateNearestGrand(
                                                context.getSource(), null, null, false
                                        ))
                                        .then(CommandManager.literal("all")
                                                .executes(context -> locateNearestGrand(
                                                        context.getSource(), null, null, true
                                                ))
                                        )
                                        .then(CommandManager.argument("family", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        FossilFamily.ids(), builder
                                                ))
                                                .executes(context -> locateGrandWithArguments(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "family"),
                                                        null
                                                ))
                                                .then(CommandManager.argument("status", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                grandStatusIds(), builder
                                                        ))
                                                        .executes(context -> locateGrandWithArguments(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "family"),
                                                                StringArgumentType.getString(context, "status")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(CommandManager.literal("repair")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("fossil_site")
                                        .executes(context -> repairNearestSmall(context.getSource()))
                                )
                                .then(CommandManager.literal("grand_fossil_site")
                                        .executes(context -> repairNearestGrand(context.getSource()))
                                )
                        )
                        .then(CommandManager.literal("reset")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("fossil_skeletons")
                                        .executes(context -> resetFossilSkeletons(
                                                context.getSource(),
                                                context.getSource().getPlayerOrThrow()
                                        ))
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> resetFossilSkeletons(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player")
                                                ))
                                        )
                                )
                        )
                        .then(CommandManager.literal("test")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("generate_fossil_site")
                                        .then(CommandManager.argument("family", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        FossilFamily.ids(), builder
                                                ))
                                                .then(CommandManager.argument("rarity", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                FossilRarity.ids(), builder
                                                        ))
                                                        .then(CommandManager.argument(
                                                                        "size",
                                                                        IntegerArgumentType.integer(
                                                                                FossilSiteGenerator.MIN_SITE_SIZE,
                                                                                FossilSiteGenerator.MAX_SITE_SIZE
                                                                        )
                                                                )
                                                                .executes(context -> generateTestSmallSite(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "family"),
                                                                        StringArgumentType.getString(context, "rarity"),
                                                                        IntegerArgumentType.getInteger(context, "size")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                                .then(CommandManager.literal("generate_grand_fossil_site")
                                        .then(CommandManager.argument("family", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        FossilFamily.ids(), builder
                                                ))
                                                .then(CommandManager.argument("rarity", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                FossilRarity.ids(), builder
                                                        ))
                                                        .executes(context -> generateTestGrandSite(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "family"),
                                                                StringArgumentType.getString(context, "rarity")
                                                        ))
                                                )
                                        )
                                )
                                .then(CommandManager.literal("give_fossils")
                                        .then(CommandManager.argument("family", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        FossilFamily.ids(), builder
                                                ))
                                                .then(CommandManager.argument("rarity", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                FossilRarity.ids(), builder
                                                        ))
                                                        .executes(context -> giveTestFossils(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "family"),
                                                                StringArgumentType.getString(context, "rarity"),
                                                                FossilIncubationRecipe.REQUIRED_FOSSILS
                                                        ))
                                                        .then(CommandManager.argument(
                                                                        "count",
                                                                        IntegerArgumentType.integer(1, 64)
                                                                )
                                                                .executes(context -> giveTestFossils(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "family"),
                                                                        StringArgumentType.getString(context, "rarity"),
                                                                        IntegerArgumentType.getInteger(context, "count")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                                .then(CommandManager.literal("inspect_skeleton")
                                        .executes(context -> inspectHeldSkeleton(context.getSource()))
                                )
                        )
                )
        );
    }

    private static int locateSmallWithArguments(
            ServerCommandSource source,
            String familyId,
            String rarityId
    ) {
        Optional<FossilFamily> family = FossilFamily.byId(familyId);
        if (family.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.unknown_family", familyId));
            return 0;
        }

        FossilRarity rarity = null;
        if (rarityId != null) {
            Optional<FossilRarity> parsed = FossilRarity.byId(rarityId);
            if (parsed.isEmpty()) {
                source.sendError(Text.translatable("command.mythicrpg.fossil.unknown_rarity", rarityId));
                return 0;
            }
            rarity = parsed.get();
        }
        return locateNearestSmall(source, family.get(), rarity, false);
    }

    private static int locateNearestSmall(
            ServerCommandSource source,
            FossilFamily family,
            FossilRarity rarity,
            boolean includeDepleted
    ) {
        if (!requireOverworld(source)) {
            return 0;
        }

        BlockPos origin = BlockPos.ofFloored(source.getPosition());
        Optional<FossilSiteState.SiteRecord> nearest = FossilSiteState.get(source.getServer())
                .findNearest(origin, family, rarity, includeDepleted);
        if (nearest.isEmpty()) {
            source.sendError(Text.translatable(includeDepleted
                    ? "command.mythicrpg.fossil.no_registered_site"
                    : "command.mythicrpg.fossil.no_intact_site"));
            return 0;
        }

        FossilSiteState.SiteRecord site = nearest.get();
        double distance = Math.sqrt(site.center().getSquaredDistance(origin));
        source.sendFeedback(() -> Text.translatable(
                        "command.mythicrpg.fossil.locate_small",
                        coordinates(site.center()),
                        (int) Math.round(distance),
                        site.family().displayName(),
                        site.dominantRarity().displayName(),
                        site.remainingBlocks(),
                        site.initialBlocks()
                ).formatted(site.depleted() ? Formatting.RED : Formatting.GOLD), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int locateGrandWithArguments(
            ServerCommandSource source,
            String familyId,
            String statusId
    ) {
        Optional<FossilFamily> family = FossilFamily.byId(familyId);
        if (family.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.unknown_family", familyId));
            return 0;
        }
        GrandSiteStatus status = null;
        if (statusId != null) {
            Optional<GrandSiteStatus> parsed = GrandSiteStatus.byId(statusId);
            if (parsed.isEmpty()) {
                source.sendError(Text.translatable("command.mythicrpg.fossil.unknown_grand_status", statusId));
                return 0;
            }
            status = parsed.get();
        }
        return locateNearestGrand(source, family.get(), status, false);
    }

    private static int locateNearestGrand(
            ServerCommandSource source,
            FossilFamily family,
            GrandSiteStatus status,
            boolean includeDepleted
    ) {
        if (!requireOverworld(source)) {
            return 0;
        }
        BlockPos origin = BlockPos.ofFloored(source.getPosition());
        Optional<GrandFossilSiteState.GrandSiteRecord> nearest = GrandFossilSiteState
                .get(source.getServer())
                .findNearest(origin, family, status, includeDepleted);
        if (nearest.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.no_matching_grand_site"));
            return 0;
        }

        GrandFossilSiteState.GrandSiteRecord site = nearest.get();
        double distance = Math.sqrt(site.center().getSquaredDistance(origin));
        source.sendFeedback(() -> Text.translatable(
                        "command.mythicrpg.fossil.locate_grand",
                        coordinates(site.center()),
                        (int) Math.round(distance),
                        site.family().displayName(),
                        site.dominantRarity().displayName(),
                        site.status().displayName(),
                        site.remainingFossils(),
                        site.initialFossils(),
                        site.specialRollSucceeded(),
                        site.barrelPresent(),
                        site.owner().toString().substring(0, 8),
                        site.specimenId().toString().substring(0, 8)
                ).formatted(Formatting.GOLD), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int repairNearestSmall(ServerCommandSource source) {
        if (!requireOverworld(source)) {
            return 0;
        }
        BlockPos origin = BlockPos.ofFloored(source.getPosition());
        FossilSiteState state = FossilSiteState.get(source.getServer());
        Optional<FossilSiteState.SiteRecord> nearest = state.findNearest(origin, null, null, true);
        if (nearest.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.no_registered_site"));
            return 0;
        }

        FossilSiteState.SiteRecord site = nearest.get();
        if (site.center().getSquaredDistance(origin) > 32.0 * 32.0) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.repair_small.too_far"));
            return 0;
        }

        int actualRemaining = 0;
        for (BlockPos pos : BlockPos.iterate(
                site.center().add(-8, -8, -8),
                site.center().add(8, 8, 8)
        )) {
            if (source.getWorld().getBlockEntity(pos) instanceof FossilBlockEntity fossil
                    && fossil.belongsToSite(site.id())
                    && !fossil.isGrandSiteFossil()) {
                actualRemaining++;
            }
        }
        state.setRemaining(site.id(), actualRemaining);
        int repaired = actualRemaining;
        source.sendFeedback(() -> Text.translatable("command.mythicrpg.fossil.repair_small.success", repaired, site.initialBlocks()).formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int repairNearestGrand(ServerCommandSource source) {
        if (!requireOverworld(source)) {
            return 0;
        }
        BlockPos origin = BlockPos.ofFloored(source.getPosition());
        GrandFossilSiteState state = GrandFossilSiteState.get(source.getServer());
        Optional<GrandFossilSiteState.GrandSiteRecord> nearest = state.findNearest(
                origin, null, null, true
        );
        if (nearest.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.no_registered_grand_site"));
            return 0;
        }

        GrandFossilSiteState.GrandSiteRecord site = nearest.get();
        if (site.center().getSquaredDistance(origin) > 64.0 * 64.0) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.repair_grand.too_far"));
            return 0;
        }

        int actualRemaining = 0;
        for (BlockPos pos : BlockPos.iterate(
                site.center().add(-GrandFossilSiteGenerator.HALF_WIDTH - 1, -GrandFossilSiteGenerator.HALF_HEIGHT - 1, -GrandFossilSiteGenerator.HALF_WIDTH - 1),
                site.center().add(GrandFossilSiteGenerator.HALF_WIDTH + 1, GrandFossilSiteGenerator.HALF_HEIGHT + 1, GrandFossilSiteGenerator.HALF_WIDTH + 1)
        )) {
            if (source.getWorld().getBlockEntity(pos) instanceof FossilBlockEntity fossil
                    && fossil.belongsToGrandSite(site.id())) {
                actualRemaining++;
            }
        }
        state.setRemainingFossils(site.id(), actualRemaining);
        state.setBarrelPresent(site.id(), source.getWorld().getBlockState(site.barrelPos()).isOf(net.minecraft.block.Blocks.BARREL));
        int repaired = actualRemaining;
        source.sendFeedback(() -> Text.translatable("command.mythicrpg.fossil.repair_grand.success", repaired, site.initialFossils(), source.getWorld().getBlockState(site.barrelPos()).isOf(net.minecraft.block.Blocks.BARREL)).formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int giveTestFossils(
            ServerCommandSource source,
            String familyId,
            String rarityId,
            int count
    ) throws CommandSyntaxException {
        Optional<FossilFamily> family = FossilFamily.byId(familyId);
        Optional<FossilRarity> rarity = FossilRarity.byId(rarityId);
        if (family.isEmpty() || rarity.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.unknown_family_or_rarity"));
            return 0;
        }

        Optional<Item> fossilItem = FossilContentRegistry.fossilItem(family.get(), rarity.get());
        if (fossilItem.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.no_item_for_combination"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ItemStack stack = new ItemStack(fossilItem.get(), count);
        player.getInventory().offerOrDrop(stack);
        source.sendFeedback(() -> Text.translatable(
                        "command.mythicrpg.fossil.give",
                        count,
                        family.get().displayName(),
                        rarity.get().displayName()
                ).formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateTestSmallSite(
            ServerCommandSource source,
            String familyId,
            String rarityId,
            int size
    ) {
        if (!requireOverworld(source)) {
            return 0;
        }
        Optional<FossilFamily> family = FossilFamily.byId(familyId);
        Optional<FossilRarity> rarity = FossilRarity.byId(rarityId);
        if (family.isEmpty() || rarity.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.unknown_family_or_rarity"));
            return 0;
        }

        ServerWorld world = source.getWorld();
        BlockPos origin = BlockPos.ofFloored(source.getPosition());
        ArrayList<BlockPos> candidates = collectNearbyNaturalStone(world, origin);
        for (BlockPos candidate : candidates) {
            Optional<FossilSiteGenerator.GeneratedSite> generated = FossilSiteGenerator.generateAt(
                    world,
                    candidate,
                    family.get(),
                    rarity.get(),
                    size,
                    world.getRandom()
            );
            if (generated.isEmpty()) {
                continue;
            }

            FossilSiteGenerator.GeneratedSite site = generated.get();
            FossilSiteState.get(source.getServer()).registerSite(
                    site.id(), site.center(), site.family(), site.dominantRarity(), site.blockCount()
            );
            for (BlockPos fossilPos : site.positions()) {
                world.getChunkManager().markForUpdate(fossilPos);
            }
            source.sendFeedback(() -> Text.translatable(
                            "command.mythicrpg.fossil.generate_small.success",
                            coordinates(site.center()),
                            site.family().displayName(),
                            site.dominantRarity().displayName(),
                            site.blockCount(),
                            formatRarityCounts(site)
                    ).formatted(Formatting.GREEN), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendError(Text.translatable("command.mythicrpg.fossil.generate_small.no_area"));
        return 0;
    }

    private static int generateTestGrandSite(
            ServerCommandSource source,
            String familyId,
            String rarityId
    ) throws CommandSyntaxException {
        if (!requireOverworld(source)) {
            return 0;
        }
        Optional<FossilFamily> family = FossilFamily.byId(familyId);
        Optional<FossilRarity> rarity = FossilRarity.byId(rarityId);
        if (family.isEmpty() || rarity.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.unknown_family_or_rarity"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = source.getWorld();
        BlockPos origin = player.getBlockPos();
        GrandFossilSiteState state = GrandFossilSiteState.get(source.getServer());
        UUID specimenId = UUID.randomUUID();
        if (!state.tryReserveSpecimen(specimenId)) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.reserve_specimen_failed"));
            return 0;
        }

        for (int attempt = 0; attempt < TEST_GRAND_SITE_ATTEMPTS; attempt++) {
            BlockPos candidate = origin.add(
                    world.getRandom().nextBetween(-32, 32),
                    world.getRandom().nextBetween(-20, 20),
                    world.getRandom().nextBetween(-32, 32)
            );
            if (state.isAreaNearExistingSite(candidate, GrandFossilSiteGenerator.MIN_SITE_SEPARATION)
                    || !GrandFossilSiteGenerator.isSafeVolume(world, candidate)) {
                continue;
            }

            Optional<GrandFossilSiteGenerator.GeneratedGrandSite> generated =
                    GrandFossilSiteGenerator.generateAtForTesting(
                            world,
                            candidate,
                            family.get(),
                            rarity.get(),
                            player.getUuid(),
                            player.getUuid(),
                            specimenId,
                            player.getUuid()
                    );
            if (generated.isEmpty()) {
                continue;
            }

            GrandFossilSiteGenerator.GeneratedGrandSite site = generated.get();
            if (!state.registerCompletedSite(site.record())) {
                site.rollback(world);
                state.releaseSpecimen(specimenId);
                source.sendError(Text.translatable("command.mythicrpg.fossil.register_grand_failed"));
                return 0;
            }

            ItemStack dossier = ExpeditionDossierData.initialize(
                    new ItemStack(ModItems.EXPEDITION_DOSSIER),
                    site.record()
            );
            player.getInventory().offerOrDrop(dossier);
            source.sendFeedback(() -> Text.translatable(
                            "command.mythicrpg.fossil.generate_grand.success",
                            coordinates(site.record().center()),
                            site.record().family().displayName(),
                            site.record().dominantRarity().displayName(),
                            site.record().initialFossils(),
                            site.record().oreBlocks()
                    ).formatted(Formatting.GREEN), false);
            return Command.SINGLE_SUCCESS;
        }

        state.releaseSpecimen(specimenId);
        source.sendError(Text.translatable("command.mythicrpg.fossil.generate_grand.no_area"));
        return 0;
    }

    private static int inspectHeldSkeleton(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        Optional<FossilSpecimenData.Specimen> parsed = FossilSpecimenData.read(stack);
        if (parsed.isEmpty()) {
            source.sendError(Text.translatable("command.mythicrpg.fossil.inspect.invalid"));
            return 0;
        }
        FossilSpecimenData.Specimen specimen = parsed.get();
        boolean locked = GrandFossilSiteState.get(source.getServer())
                .isSpecimenAnalyzed(specimen.specimenId());
        source.sendFeedback(() -> Text.translatable(
                        "command.mythicrpg.fossil.inspect.success",
                        specimen.specimenId().toString(),
                        specimen.family().displayName(),
                        specimen.rarity().displayName(),
                        specimen.reconstructedBy().toString(),
                        specimen.analyzed(),
                        locked
                ).formatted(Formatting.GOLD), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetFossilSkeletons(
            ServerCommandSource source,
            ServerPlayerEntity target
    ) {
        Set<UUID> specimenIds = new HashSet<>();
        FossilCodexData previousCodex = ModAttachments.getFossilCodex(target);
        for (String registeredId : previousCodex.registeredSpecimenIds()) {
            try {
                specimenIds.add(UUID.fromString(registeredId));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy identifiers while still clearing the Codex.
            }
        }

        int resetItems = resetSkeletonsInInventory(
                target.getInventory(), target.getUuid(), specimenIds
        );
        resetItems += resetSkeletonsInInventory(
                target.getEnderChestInventory(), target.getUuid(), specimenIds
        );

        FossilCodexManager.reset(target);
        int removedLocks = GrandFossilSiteState.get(source.getServer())
                .resetSpecimenLocks(specimenIds);
        int finalResetItems = resetItems;
        source.sendFeedback(() -> Text.translatable("command.mythicrpg.fossil.reset.success", target.getName(), finalResetItems, removedLocks).formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetSkeletonsInInventory(
            Inventory inventory,
            UUID ownerUuid,
            Set<UUID> specimenIds
    ) {
        int reset = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!(stack.getItem() instanceof FossilSkeletonItem)) {
                continue;
            }
            Optional<FossilSpecimenData.Specimen> parsed = FossilSpecimenData.read(stack);
            if (parsed.isEmpty()) {
                continue;
            }
            FossilSpecimenData.Specimen specimen = parsed.get();
            if (!specimen.reconstructedBy().equals(ownerUuid)) {
                continue;
            }
            specimenIds.add(specimen.specimenId());
            FossilSpecimenData.markUnanalyzed(stack);
            reset++;
        }
        inventory.markDirty();
        return reset;
    }

    private static String formatRarityCounts(FossilSiteGenerator.GeneratedSite site) {
        StringBuilder result = new StringBuilder();
        for (FossilRarity rarity : FossilRarity.values()) {
            int count = site.rarityCounts().getOrDefault(rarity, 0);
            if (count <= 0) continue;
            if (result.length() > 0) result.append(", ");
            result.append(rarity.id()).append('=').append(count);
        }
        return result.toString();
    }

    private static ArrayList<BlockPos> collectNearbyNaturalStone(ServerWorld world, BlockPos origin) {
        ArrayList<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(
                origin.add(-TEST_SITE_HORIZONTAL_SEARCH, -TEST_SITE_VERTICAL_SEARCH, -TEST_SITE_HORIZONTAL_SEARCH),
                origin.add(TEST_SITE_HORIZONTAL_SEARCH, TEST_SITE_VERTICAL_SEARCH, TEST_SITE_HORIZONTAL_SEARCH)
        )) {
            if (FossilSiteGenerator.isValidFossilPosition(world, pos)) {
                candidates.add(pos.toImmutable());
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(origin)));
        return candidates;
    }

    private static Text coordinates(BlockPos pos) {
        return Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ())
                .formatted(Formatting.AQUA);
    }

    private static String[] grandStatusIds() {
        GrandSiteStatus[] values = GrandSiteStatus.values();
        String[] ids = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ids[i] = values[i].id();
        }
        return ids;
    }

    private static boolean requireOverworld(ServerCommandSource source) {
        if (source.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
            return true;
        }
        source.sendError(Text.translatable("command.mythicrpg.fossil.overworld_only"));
        return false;
    }
}
