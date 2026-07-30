package com.mythicrpg.client;

import com.mythicrpg.network.ResetTreePayload;
import com.mythicrpg.core.SkillTreeNode;
import com.mythicrpg.core.SkillTreeRegistry;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.network.UnlockRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mythicrpg.core.SkillTreeManager;

import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.Set;
import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.core.ModItems;

public class SkillTreeScreen extends Screen {

    private static final int NODE_SIZE = 16;

    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP = 4;
    private static final int TAB_Y = 6;

    private static final int HEADER_TOP = 30;
    private static final int HEADER_HEIGHT = 64;

    // Zone scrollable de l'arbre. On laisse de la place au header + tabs.
    private static final int VIEWPORT_TOP = HEADER_TOP + HEADER_HEIGHT + 8;
    private static final int VIEWPORT_MARGIN_BOTTOM = 12;

    private static final int CONTENT_WIDTH = 260;
    private static final int CONTENT_HEIGHT = 330;

    private static final int RESET_BUTTON_WIDTH = 125;
    private static final int RESET_BUTTON_HEIGHT = 20;

    private static final long PURCHASE_ANIM_DURATION_MS = 500;

    private final Map<Integer, Long> purchaseAnimStart = new HashMap<>();
    private Set<Integer> lastKnownUnlocked = new HashSet<>();

    private static final long RESET_CONFIRM_DURATION_MS = 3000;

    private long resetConfirmUntilMs = 0L;
    private SkillType resetConfirmSkill = null;

    private static final Map<SkillType, Item> TAB_ICONS = new java.util.EnumMap<>(SkillType.class);
    static {
        TAB_ICONS.put(SkillType.MINING, Items.NETHERITE_PICKAXE);
        TAB_ICONS.put(SkillType.FIGHTING, Items.NETHERITE_SWORD);
        TAB_ICONS.put(SkillType.WOODCUTTING, Items.NETHERITE_AXE);
        TAB_ICONS.put(SkillType.FARMING, Items.WHEAT);
        TAB_ICONS.put(SkillType.CRAFTING, Items.CRAFTING_TABLE);
        TAB_ICONS.put(SkillType.FISHING, Items.FISHING_ROD);
        TAB_ICONS.put(SkillType.BUILDING, Items.BRICKS);
        TAB_ICONS.put(SkillType.TRAVELING, Items.GOLDEN_BOOTS);
        TAB_ICONS.put(SkillType.EATING, Items.COOKED_BEEF);
    }


    private static final Map<Integer, Item> MINING_ICONS = new HashMap<>();

    static {
        MINING_ICONS.put(1, Items.NETHERITE_PICKAXE);
        MINING_ICONS.put(2, Items.IRON_NUGGET);
        MINING_ICONS.put(3, Items.GOLD_NUGGET);
        MINING_ICONS.put(4, Items.EMERALD);
        MINING_ICONS.put(5, Items.SPYGLASS);
        MINING_ICONS.put(6, Items.SPYGLASS);
        MINING_ICONS.put(7, Items.SPYGLASS);
        MINING_ICONS.put(8, Items.GLOWSTONE_DUST);
        MINING_ICONS.put(9, Items.IRON_NUGGET);
        MINING_ICONS.put(10, Items.GOLD_NUGGET);
        MINING_ICONS.put(11, Items.DIAMOND);
        MINING_ICONS.put(12, Items.EXPERIENCE_BOTTLE);
        MINING_ICONS.put(13, Items.EXPERIENCE_BOTTLE);
        MINING_ICONS.put(14, Items.EXPERIENCE_BOTTLE);
        MINING_ICONS.put(15, Items.BLAZE_POWDER);
        MINING_ICONS.put(16, Items.FEATHER);
        MINING_ICONS.put(17, Items.TURTLE_HELMET);
        MINING_ICONS.put(18, Items.TNT);
        MINING_ICONS.put(19, Items.GLOWSTONE_DUST);
        MINING_ICONS.put(20, Items.ANVIL);
    }

    private static final Map<Integer, Item> FIGHTING_ICONS = new HashMap<>();

    static {
        FIGHTING_ICONS.put(1, Items.GLOW_INK_SAC);
        FIGHTING_ICONS.put(2, Items.SPIDER_EYE);
        FIGHTING_ICONS.put(3, Items.SPIDER_EYE);
        FIGHTING_ICONS.put(4, Items.FERMENTED_SPIDER_EYE);
        FIGHTING_ICONS.put(5, Items.IRON_NUGGET);
        FIGHTING_ICONS.put(6, Items.GOLD_NUGGET);
        FIGHTING_ICONS.put(7, Items.EMERALD);
        FIGHTING_ICONS.put(8, Items.BLAZE_POWDER);
        FIGHTING_ICONS.put(9, Items.STICK);
        FIGHTING_ICONS.put(10, Items.IRON_SWORD);
        FIGHTING_ICONS.put(11, Items.NETHERITE_SWORD);
        FIGHTING_ICONS.put(12, Items.CLOCK);
        FIGHTING_ICONS.put(13, Items.GOLDEN_SWORD);
        FIGHTING_ICONS.put(14, Items.DIAMOND_SWORD);
        FIGHTING_ICONS.put(15, Items.ROTTEN_FLESH);
        FIGHTING_ICONS.put(16, Items.SPIDER_EYE);
        FIGHTING_ICONS.put(17, Items.EXPERIENCE_BOTTLE);
        FIGHTING_ICONS.put(18, Items.SHIELD);
        FIGHTING_ICONS.put(19, Items.NETHERITE_CHESTPLATE);
        FIGHTING_ICONS.put(20, Items.NETHER_STAR);
    }

    private static final Map<Integer, Item> WOODCUTTING_ICONS = new HashMap<>();

    static {
        WOODCUTTING_ICONS.put(1, ModBlocks.ENCHANTED_WOOD.asItem());
        WOODCUTTING_ICONS.put(2, Items.OAK_LOG);
        WOODCUTTING_ICONS.put(3, Items.SPRUCE_LOG);
        WOODCUTTING_ICONS.put(4, Items.DARK_OAK_LOG);
        WOODCUTTING_ICONS.put(5, ModBlocks.ENCHANTED_WOOD.asItem());
        WOODCUTTING_ICONS.put(6, ModBlocks.ENCHANTED_WOOD.asItem());
        WOODCUTTING_ICONS.put(7, ModBlocks.ENCHANTED_WOOD.asItem());
        WOODCUTTING_ICONS.put(8, Items.ENCHANTED_BOOK);
        WOODCUTTING_ICONS.put(9, Items.OAK_SAPLING);
        WOODCUTTING_ICONS.put(10, Items.EXPERIENCE_BOTTLE);
        WOODCUTTING_ICONS.put(11, Items.BIRCH_SAPLING);
        WOODCUTTING_ICONS.put(12, Items.IRON_AXE);
        WOODCUTTING_ICONS.put(13, Items.APPLE);
        WOODCUTTING_ICONS.put(14, Items.GOLDEN_APPLE);
        WOODCUTTING_ICONS.put(15, ModItems.CHEST_MODULE_I);
        WOODCUTTING_ICONS.put(16, ModItems.CHEST_MODULE_II);
        WOODCUTTING_ICONS.put(17, ModItems.CHEST_MODULE_III);
        WOODCUTTING_ICONS.put(18, ModItems.ENCHANTED_AXE);
        WOODCUTTING_ICONS.put(19, Items.GOLDEN_CARROT);
        WOODCUTTING_ICONS.put(20, Items.NETHERITE_AXE);
    }

    private static final Map<Integer, Item> FARMING_ICONS = new HashMap<>();

    static {
        FARMING_ICONS.put(1, Items.WHEAT_SEEDS);
        FARMING_ICONS.put(2, Items.WHEAT);
        FARMING_ICONS.put(3, Items.CARROT);
        FARMING_ICONS.put(4, Items.POTATO);
        FARMING_ICONS.put(5, Items.COMPOSTER);
        FARMING_ICONS.put(6, Items.BONE_MEAL);
        FARMING_ICONS.put(7, Items.BONE_BLOCK);
        FARMING_ICONS.put(8, Items.BUNDLE);
        FARMING_ICONS.put(9, Items.EXPERIENCE_BOTTLE);
        FARMING_ICONS.put(10, Items.PUMPKIN_SEEDS);
        FARMING_ICONS.put(11, Items.MELON_SEEDS);
        FARMING_ICONS.put(12, Items.BONE_MEAL);
        FARMING_ICONS.put(13, Items.WATER_BUCKET);
        FARMING_ICONS.put(14, Items.MELON);
        FARMING_ICONS.put(15, Items.WOODEN_HOE);
        FARMING_ICONS.put(16, Items.IRON_HOE);
        FARMING_ICONS.put(17, Items.DIAMOND_HOE);
        FARMING_ICONS.put(18, ModItems.ENCHANTED_FLOWER);
        FARMING_ICONS.put(19, Items.TOTEM_OF_UNDYING);
        FARMING_ICONS.put(20, Items.FLOWERING_AZALEA);
    }

    private static final Map<Integer, Item> CRAFTING_ICONS = new HashMap<>();

    static {
        CRAFTING_ICONS.put(1, Items.CRAFTING_TABLE);
        CRAFTING_ICONS.put(2, Items.STICK);
        CRAFTING_ICONS.put(3, Items.STRING);
        CRAFTING_ICONS.put(4, Items.IRON_NUGGET);
        CRAFTING_ICONS.put(5, Items.LAPIS_BLOCK);
        CRAFTING_ICONS.put(6, Items.LAPIS_LAZULI);
        CRAFTING_ICONS.put(7, Items.EXPERIENCE_BOTTLE);
        CRAFTING_ICONS.put(8, Items.GOLD_BLOCK);
        CRAFTING_ICONS.put(9, Items.NETHER_STAR);
        CRAFTING_ICONS.put(10, Items.AMETHYST_SHARD);
        CRAFTING_ICONS.put(11, Items.REDSTONE);
        CRAFTING_ICONS.put(12, Items.EXPERIENCE_BOTTLE);
        CRAFTING_ICONS.put(13, Items.NETHER_STAR);
        CRAFTING_ICONS.put(14, Items.WRITABLE_BOOK);
        CRAFTING_ICONS.put(15, Items.HOPPER);
        CRAFTING_ICONS.put(16, Items.CLOCK);
        CRAFTING_ICONS.put(17, Items.ENCHANTED_BOOK);
        CRAFTING_ICONS.put(18, Items.EMERALD_BLOCK);
        CRAFTING_ICONS.put(19, Items.COPPER_BLOCK);
        CRAFTING_ICONS.put(20, Items.KNOWLEDGE_BOOK);
    }

    private static final Map<Integer, Item> TRAVELING_ICONS = new HashMap<>();

    static {
        TRAVELING_ICONS.put(1, Items.FEATHER);
        TRAVELING_ICONS.put(2, Items.SOUL_SAND);
        TRAVELING_ICONS.put(3, Items.HEART_OF_THE_SEA);
        TRAVELING_ICONS.put(4, Items.LEATHER_BOOTS);
        TRAVELING_ICONS.put(5, Items.EXPERIENCE_BOTTLE);
        TRAVELING_ICONS.put(6, Items.EXPERIENCE_BOTTLE);
        TRAVELING_ICONS.put(7, Items.SPYGLASS);
        TRAVELING_ICONS.put(8, Items.AMETHYST_SHARD);
        TRAVELING_ICONS.put(9, Items.COMPASS);
        TRAVELING_ICONS.put(10, Items.MAP);
        TRAVELING_ICONS.put(11, Items.ENDER_EYE);
        TRAVELING_ICONS.put(12, Items.ECHO_SHARD);
        TRAVELING_ICONS.put(13, Items.DIAMOND_BOOTS);
        TRAVELING_ICONS.put(14, Items.SUGAR);
        TRAVELING_ICONS.put(15, Items.MINECART);
        TRAVELING_ICONS.put(16, Items.OAK_BOAT);
        TRAVELING_ICONS.put(17, Items.SADDLE);
        TRAVELING_ICONS.put(18, Items.FILLED_MAP);
        TRAVELING_ICONS.put(19, Items.PHANTOM_MEMBRANE);
        TRAVELING_ICONS.put(20, ModItems.GRAPPLING_HOOK);
    }


    private static final Map<Integer, Item> BUILDING_ICONS = new HashMap<>();

    static {
        BUILDING_ICONS.put(1, Items.IRON_PICKAXE);
        BUILDING_ICONS.put(2, Items.MAP);
        BUILDING_ICONS.put(3, Items.FILLED_MAP);
        BUILDING_ICONS.put(4, Items.WRITABLE_BOOK);
        BUILDING_ICONS.put(5, Items.HOPPER);
        BUILDING_ICONS.put(6, Items.ENDER_PEARL);
        BUILDING_ICONS.put(7, Items.DIAMOND_PICKAXE);
        BUILDING_ICONS.put(8, Items.STONE_SLAB);
        BUILDING_ICONS.put(9, Items.STICK);
        BUILDING_ICONS.put(10, Items.BAMBOO);
        BUILDING_ICONS.put(11, Items.BLAZE_ROD);
        BUILDING_ICONS.put(12, Items.SCAFFOLDING);
        BUILDING_ICONS.put(13, Items.WHITE_CONCRETE);
        BUILDING_ICONS.put(14, Items.COMPASS);
        BUILDING_ICONS.put(15, Items.CHEST);
        BUILDING_ICONS.put(16, Items.BARREL);
        BUILDING_ICONS.put(17, Items.ENDER_CHEST);
        BUILDING_ICONS.put(18, Items.ARMOR_STAND);
        BUILDING_ICONS.put(19, Items.FIREWORK_STAR);
        BUILDING_ICONS.put(20, Items.BLAZE_ROD);
    }


    private static final Map<Integer, Item> FISHING_ICONS = new HashMap<>();

    static {
        FISHING_ICONS.put(1, ModItems.MYTHIC_FISHING_ROD);
        FISHING_ICONS.put(2, ModItems.WEATHER_WAND);
        FISHING_ICONS.put(3, ModItems.WEATHER_WAND);
        FISHING_ICONS.put(4, ModItems.WEATHER_WAND);
        FISHING_ICONS.put(5, ModItems.BAIT_I);
        FISHING_ICONS.put(6, ModItems.BAIT_II);
        FISHING_ICONS.put(7, ModItems.BAIT_III);
        FISHING_ICONS.put(8, Items.ANVIL);
        FISHING_ICONS.put(9, ModItems.RUNE_RARITY);
        FISHING_ICONS.put(10, ModItems.RUNE_SPEED);
        FISHING_ICONS.put(11, ModItems.RUNE_MASTERY);
        FISHING_ICONS.put(12, ModBlocks.FISH_NET.asItem());
        FISHING_ICONS.put(13, ModBlocks.FISH_NET.asItem());
        FISHING_ICONS.put(14, ModBlocks.FISH_NET.asItem());
        FISHING_ICONS.put(15, ModBlocks.FISHERY_TABLE.asItem());
        FISHING_ICONS.put(16, Items.DIAMOND_CHESTPLATE);
        FISHING_ICONS.put(17, ModItems.FISHING_BOAT);
        FISHING_ICONS.put(18, ModItems.BAIT_LEGENDARY);
        FISHING_ICONS.put(19, ModItems.BASALT_FISHING_ROD);
        FISHING_ICONS.put(20, ModItems.VOID_FISHING_ROD);
    }

    private static final Map<Integer, Item> EATING_ICONS = new HashMap<>();

    static {
        EATING_ICONS.put(1, Items.CAULDRON);
        EATING_ICONS.put(2, Items.CARROT);
        EATING_ICONS.put(3, Items.POTATO);
        EATING_ICONS.put(4, Items.COOKED_BEEF);
        EATING_ICONS.put(5, Items.BOWL);
        EATING_ICONS.put(6, Items.RABBIT_STEW);
        EATING_ICONS.put(7, Items.CAKE);
        EATING_ICONS.put(8, Items.BLUE_ICE);
        EATING_ICONS.put(9, Items.GOLDEN_CARROT);
        EATING_ICONS.put(10, Items.EMERALD);
        EATING_ICONS.put(11, Items.CAKE);
        EATING_ICONS.put(12, Items.SPIDER_EYE);
        EATING_ICONS.put(13, Items.CHEST_MINECART);
        EATING_ICONS.put(14, Items.COMPASS);
        EATING_ICONS.put(15, Items.COMPOSTER);
        EATING_ICONS.put(16, Items.IRON_SWORD);
        EATING_ICONS.put(17, Items.NETHER_STAR);
        EATING_ICONS.put(18, Items.NAME_TAG);
        EATING_ICONS.put(19, Items.ENCHANTED_BOOK);
        EATING_ICONS.put(20, Items.COOKED_CHICKEN);
    }

    private static final Map<SkillType, Map<Integer, Item>> ICONS_BY_SKILL = new HashMap<>();

    static {
        ICONS_BY_SKILL.put(SkillType.MINING, MINING_ICONS);
        ICONS_BY_SKILL.put(SkillType.FIGHTING, FIGHTING_ICONS);
        ICONS_BY_SKILL.put(SkillType.WOODCUTTING, WOODCUTTING_ICONS);
        ICONS_BY_SKILL.put(SkillType.FARMING, FARMING_ICONS);
        ICONS_BY_SKILL.put(SkillType.CRAFTING, CRAFTING_ICONS);
        ICONS_BY_SKILL.put(SkillType.TRAVELING, TRAVELING_ICONS);
        ICONS_BY_SKILL.put(SkillType.BUILDING, BUILDING_ICONS);
        ICONS_BY_SKILL.put(SkillType.FISHING, FISHING_ICONS);
        ICONS_BY_SKILL.put(SkillType.EATING, EATING_ICONS);
    }


    private SkillType selectedSkill;

    private Map<Integer, SkillTreeNode> tree;
    private Map<Integer, Item> icons;

    private int scrollOffset = 0;
    private int offsetX;


    public SkillTreeScreen() {
        super(Text.translatable("screen.mythicrpg.skill_tree"));

        selectedSkill = MythicClientPreferences.getLastOpenedSkill();
        updateTree();
    }

    private static class SkillTabButton extends ButtonWidget {
        private final Item icon;

        SkillTabButton(int x, int y, int size, Item icon, PressAction onPress, Text tooltipText) {
            super(x, y, size, size, Text.empty(), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
            this.icon = icon;
            setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(tooltipText));
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);
            int iconX = getX() + (getWidth() - 16) / 2;
            int iconY = getY() + (getHeight() - 16) / 2;
            context.drawItem(new ItemStack(icon), iconX, iconY);
        }
    }

    private void updateTree() {
        tree = SkillTreeRegistry.getTree(selectedSkill);
        icons = ICONS_BY_SKILL.getOrDefault(selectedSkill, Map.of());
    }

    @Override
    protected void init() {
        lastKnownUnlocked = new HashSet<>(ClientSkillTreeState.getUnlockedIds(selectedSkill));

        offsetX = (width - CONTENT_WIDTH) / 2;

        // Onglets centrés en haut de l'écran.
        int tabX = firstTabX();
        for (SkillType type : SkillType.values()) {
            addDrawableChild(
                    new SkillTabButton(
                            tabX, TAB_Y, TAB_SIZE,
                            TAB_ICONS.getOrDefault(type, Items.BARRIER),
                            button -> {
                                selectedSkill = type;
                                MythicClientPreferences.rememberLastOpenedSkill(type);
                                scrollOffset = 0;
                                updateTree();
                                clearAndInit();
                            },
                            type.displayName()
                    )
            );
            tabX += TAB_SIZE + TAB_GAP;
        }

        int unlockedCount =
                ClientSkillTreeState
                        .getUnlockedIds(selectedSkill)
                        .size();

        boolean waitingResetConfirm =
                resetConfirmSkill == selectedSkill
                        && System.currentTimeMillis() < resetConfirmUntilMs;

        Text resetButtonText = waitingResetConfirm
                ? Text.translatable("screen.mythicrpg.skill_tree.reset.confirm")
                : Text.translatable("screen.mythicrpg.skill_tree.reset", unlockedCount);

        int resetX = width - RESET_BUTTON_WIDTH - 10;
        int resetY = HEADER_TOP + 26;

        ButtonWidget resetButton =
                ButtonWidget.builder(
                                resetButtonText,
                                btn -> {
                                    long now = System.currentTimeMillis();

                                    boolean confirm =
                                            resetConfirmSkill == selectedSkill
                                                    && now < resetConfirmUntilMs;

                                    if (confirm) {
                                        ClientPlayNetworking.send(
                                                new ResetTreePayload(selectedSkill.name())
                                        );

                                        resetConfirmSkill = null;
                                        resetConfirmUntilMs = 0L;
                                    } else {
                                        resetConfirmSkill = selectedSkill;
                                        resetConfirmUntilMs = now + RESET_CONFIRM_DURATION_MS;
                                    }

                                    clearAndInit();
                                }
                        )
                        .dimensions(resetX, resetY, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT)
                        .build();

        resetButton.active = unlockedCount > 0;

        addDrawableChild(resetButton);
    }


    private int maxScroll() {
        int viewportHeight = height - VIEWPORT_TOP - VIEWPORT_MARGIN_BOTTOM;
        return Math.max(0, CONTENT_HEIGHT - viewportHeight);
    }


    private int nodeScreenX(SkillTreeNode node) {
        return offsetX + node.getX();
    }


    private int nodeScreenY(SkillTreeNode node) {
        return VIEWPORT_TOP + node.getY() - scrollOffset;
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        // IMPORTANT : super.render peut dessiner le fond flouté de Minecraft.
        // On l'appelle donc AVANT l'arbre, sinon le tree se retrouve visuellement en arrière-plan.
        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        // L'arbre est dessiné après le fond pour rester net et au premier plan.
        // Il commence sous le header, donc il ne recouvre pas les onglets ni le bouton reset.
        drawSkillTree(context);

        // Les informations du header sont dessinées en dernier pour rester parfaitement lisibles.
        drawHeaderInfo(context);
        drawSelectedTabHighlight(context);
        drawNodeTooltips(context, mouseX, mouseY);
    }

    private void drawHeaderBackground(DrawContext context) {
        context.fill(0, 0, width, VIEWPORT_TOP, 0xAA000000);
        context.fill(0, VIEWPORT_TOP - 2, width, VIEWPORT_TOP, 0xFF333333);
    }

    private void drawHeaderInfo(DrawContext context) {
        int level = ClientSkillTreeState.getLevel(selectedSkill);
        int xp = ClientSkillTreeState.getCurrentXp(selectedSkill);
        int xpForNext = ClientSkillTreeState.getXpForNext(selectedSkill);
        int availablePoints = ClientSkillTreeState.getSkillPoints(selectedSkill);

        int globalLevel = ClientSkillTreeState.getGlobalLevel();
        int maxGlobalLevel = ClientSkillTreeState.getMaxGlobalLevel();

        Text xpText = xpForNext > 0
                ? Text.translatable("screen.mythicrpg.skill_tree.level_xp", level, xp, xpForNext)
                : Text.translatable("screen.mythicrpg.skill_tree.level_max", level);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.mythicrpg.skill_tree.header", selectedSkill.displayName()),
                width / 2,
                HEADER_TOP,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.mythicrpg.skill_tree.global_level", globalLevel, maxGlobalLevel),
                width / 2,
                HEADER_TOP + 14,
                0x55FFFF
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                xpText,
                width / 2,
                HEADER_TOP + 32,
                0xAAAAAA
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.mythicrpg.skill_tree.available_points", availablePoints),
                width / 2,
                HEADER_TOP + 48,
                0xFFFF55
        );
    }

    private void drawSkillTree(DrawContext context) {
        context.enableScissor(
                0,
                VIEWPORT_TOP,
                this.width,
                this.height - VIEWPORT_MARGIN_BOTTOM
        );

        var unlocked = ClientSkillTreeState.getUnlockedIds(selectedSkill);

        // Lignes entre nodes
        for (SkillTreeNode node : tree.values()) {
            for (int parentId : node.getParentIds()) {
                SkillTreeNode parent = tree.get(parentId);
                if (parent != null) {
                    drawConnector(context, parent, node);
                }
            }
        }

        // Nodes
        for (SkillTreeNode node : tree.values()) {
            drawNode(context, node, unlocked);
        }

        context.disableScissor();
    }

    private void drawNodeTooltips(DrawContext context, int mouseX, int mouseY) {
        var unlocked = ClientSkillTreeState.getUnlockedIds(selectedSkill);

        for (SkillTreeNode node : tree.values()) {
            if (isMouseOverNode(node, mouseX, mouseY)) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.translatable(node.getNameTranslationKey()).formatted(Formatting.GREEN));
                tooltip.addAll(wrapDescription(Text.translatable(node.getDescriptionTranslationKey()).getString(), 30));
                tooltip.add(Text.translatable("screen.mythicrpg.skill_tree.cost", SkillTreeManager.NODE_UNLOCK_COST).formatted(Formatting.GRAY));
                tooltip.add(buildStatusLine(node, unlocked));

                context.drawTooltip(
                        textRenderer,
                        tooltip,
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private void drawSelectedTabHighlight(DrawContext context) {
        int x = tabXFor(selectedSkill);
        int y = TAB_Y;
        int color = 0xFFFFD700;

        context.fill(x - 1, y - 1, x + TAB_SIZE + 1, y, color);
        context.fill(x - 1, y + TAB_SIZE, x + TAB_SIZE + 1, y + TAB_SIZE + 1, color);
        context.fill(x - 1, y - 1, x, y + TAB_SIZE + 1, color);
        context.fill(x + TAB_SIZE, y - 1, x + TAB_SIZE + 1, y + TAB_SIZE + 1, color);
    }

    private int firstTabX() {
        int tabCount = SkillType.values().length;
        int totalWidth = tabCount * TAB_SIZE + (tabCount - 1) * TAB_GAP;
        return Math.max(5, (width - totalWidth) / 2);
    }

    private int tabXFor(SkillType type) {
        return firstTabX() + type.ordinal() * (TAB_SIZE + TAB_GAP);
    }

    private void drawConnector(
            DrawContext context,
            SkillTreeNode from,
            SkillTreeNode to
    ) {

        int color = 0xFF555555;


        int x1 =
                nodeScreenX(from)
                        + NODE_SIZE / 2;

        int y1 =
                nodeScreenY(from)
                        + NODE_SIZE;


        int x2 =
                nodeScreenX(to)
                        + NODE_SIZE / 2;

        int y2 =
                nodeScreenY(to);



        int midY =
                (y1 + y2) / 2;



        context.fill(
                x1 - 1,
                y1,
                x1 + 1,
                midY,
                color
        );


        context.fill(
                Math.min(x1, x2) - 1,
                midY - 1,
                Math.max(x1, x2) + 1,
                midY + 1,
                color
        );


        context.fill(
                x2 - 1,
                midY,
                x2 + 1,
                y2,
                color
        );
    }





    private void drawNode(
            DrawContext context,
            SkillTreeNode node,
            java.util.List<Integer> unlocked
    ) {


        int x =
                nodeScreenX(node);


        int y =
                nodeScreenY(node);



        if (y + NODE_SIZE < VIEWPORT_TOP
                || y > height) {

            return;
        }



        boolean isUnlocked =
                unlocked.contains(node.getId());



        boolean prerequisiteMet =
                node.isRoot()
                        ||
                        node.getParentIds()
                                .stream()
                                .anyMatch(unlocked::contains);



        int borderColor;
        int fillColor;



        if (isUnlocked) {

            borderColor = 0xFFFFD700;
            fillColor = 0xFF5C4A1E;

        } else if (prerequisiteMet) {

            borderColor = 0xFFAAAAAA;
            fillColor = 0xFF3A3A3A;

        } else {

            borderColor = 0xFF555555;
            fillColor = 0xFF1E1E1E;
        }




        context.fill(
                x - 2,
                y - 2,
                x + NODE_SIZE + 2,
                y + NODE_SIZE + 2,
                borderColor
        );



        context.fill(
                x,
                y,
                x + NODE_SIZE,
                y + NODE_SIZE,
                fillColor
        );



        Item icon =
                icons.getOrDefault(
                        node.getId(),
                        Items.STONE
                );



        context.drawItem(
                new ItemStack(icon),
                x + (NODE_SIZE - 16) / 2,
                y + (NODE_SIZE - 16) / 2
        );

        drawPurchaseAnimation(context, node, x, y, System.currentTimeMillis());
    }

    private boolean isMouseOverNode(
            SkillTreeNode node,
            int mouseX,
            int mouseY
    ) {
        int x = nodeScreenX(node);
        int y = nodeScreenY(node);

        return mouseX >= x
                && mouseX < x + NODE_SIZE
                && mouseY >= VIEWPORT_TOP
                && mouseY < height - VIEWPORT_MARGIN_BOTTOM
                && mouseY >= y
                && mouseY < y + NODE_SIZE;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {


        var unlocked =
                ClientSkillTreeState
                        .getUnlockedIds(selectedSkill);



        for (SkillTreeNode node : tree.values()) {


            if (isMouseOverNode(
                    node,
                    (int) mouseX,
                    (int) mouseY
            )) {


                boolean isUnlocked =
                        unlocked.contains(node.getId());



                boolean prerequisiteMet =
                        node.isRoot()
                                ||
                                node.getParentIds()
                                        .stream()
                                        .anyMatch(unlocked::contains);



                if (!isUnlocked
                        && prerequisiteMet) {


                    ClientPlayNetworking.send(
                            new UnlockRequestPayload(
                                    selectedSkill.name(),
                                    node.getId()
                            )
                    );
                }


                return true;
            }
        }


        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }





    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {


        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                maxScroll(),
                                scrollOffset
                                        - (int)
                                        (verticalAmount * 15)
                        )
                );


        return true;
    }

    @Override
    public boolean shouldPause() {

        return false;
    }

    private static List<Text> wrapDescription(String text, int maxCharsPerLine) {
        List<Text> lines = new ArrayList<>();

        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split(" ")) {

            if (currentLine.isEmpty()) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxCharsPerLine) {
                currentLine.append(" ").append(word);
            } else {
                lines.add(Text.literal(currentLine.toString()).formatted(Formatting.GRAY));
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(Text.literal(currentLine.toString()).formatted(Formatting.GRAY));
        }

        return lines;
    }

    private Text buildStatusLine(SkillTreeNode node, List<Integer> unlocked) {
        if (unlocked.contains(node.getId())) {
            return Text.translatable("screen.mythicrpg.skill_tree.status.owned").formatted(Formatting.GOLD);
        }

        boolean prerequisiteMet = node.isRoot() || node.getParentIds().stream().anyMatch(unlocked::contains);
        if (prerequisiteMet) {
            return Text.translatable("screen.mythicrpg.skill_tree.status.available").formatted(Formatting.GREEN);
        }

        String requiredNames = node.getParentIds().stream()
                .map(id -> tree.get(id))
                .filter(Objects::nonNull)
                .map(parentNode -> Text.translatable(parentNode.getNameTranslationKey()).getString())
                .collect(Collectors.joining(", "));

        return Text.translatable("screen.mythicrpg.skill_tree.status.locked", requiredNames).formatted(Formatting.RED);
    }

    public void refresh() {
        Set<Integer> newUnlocked = new HashSet<>(ClientSkillTreeState.getUnlockedIds(selectedSkill));
        Set<Integer> newlyUnlocked = new HashSet<>(newUnlocked);
        newlyUnlocked.removeAll(lastKnownUnlocked);

        long now = System.currentTimeMillis();
        for (int nodeId : newlyUnlocked) {
            purchaseAnimStart.put(nodeId, now);
        }
        if (!newlyUnlocked.isEmpty()) {
            playPurchaseSound();
        }

        lastKnownUnlocked = newUnlocked;
        updateTree();
    }

    private void playPurchaseSound() {
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f)
        );
    }

    private void drawPurchaseAnimation(DrawContext context, SkillTreeNode node, int x, int y, long now) {
        Long start = purchaseAnimStart.get(node.getId());
        if (start == null) {
            return;
        }

        long elapsed = now - start;
        if (elapsed >= PURCHASE_ANIM_DURATION_MS) {
            purchaseAnimStart.remove(node.getId());
            return;
        }

        float progress = elapsed / (float) PURCHASE_ANIM_DURATION_MS;
        int expand = (int) (progress * 10);
        int alpha = (int) ((1f - progress) * 255);
        int color = 0xFFD700 | (alpha << 24);

        int left = x - expand;
        int top = y - expand;
        int right = x + NODE_SIZE + expand;
        int bottom = y + NODE_SIZE + expand;

        context.fill(left, top, right, top + 1, color);
        context.fill(left, bottom - 1, right, bottom, color);
        context.fill(left, top, left + 1, bottom, color);
        context.fill(right - 1, top, right, bottom, color);
    }
}
