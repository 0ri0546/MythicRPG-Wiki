package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;


/**
 * Maintient le slot de Palette actuellement exposé comme main principale.
 *
 * <p>L'état est volontairement indexé par instance de joueur, et non par UUID.
 * Dans un serveur intégré, le joueur client et le joueur serveur ont le même UUID
 * mais sont deux objets différents. Partager leur cache faisait appliquer la
 * consommation prédite côté client puis la consommation autoritaire côté serveur
 * sur le même ItemStack, soit deux blocs consommés pour un seul placement.</p>
 */
public final class PaletteSelectionManager {
    private PaletteSelectionManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
                flush(player);
            }
        });
    }

    /**
     * Change la sélection sans toucher au slot vanilla du joueur.
     */
    public static void select(PlayerEntity player, int index) {
        PaletteSelectionState state = state(player);
        flush(player, state);

        ItemStack palette = player.getOffHandStack();
        int capacity = palette.isOf(ModItems.FOSSIL_PALETTE)
                ? FossilPaletteItem.slots(palette)
                : 0;

        if (index < 0 || index >= capacity) {
            state.reset();
            return;
        }

        DefaultedList<ItemStack> contents = FossilPaletteItem.read(palette);
        state.selectedIndex = index;
        state.cachedStack = contents.get(index).copy();
        state.loadedComponent = componentOf(palette);
    }

    public static int selected(PlayerEntity player) {
        return state(player).selectedIndex;
    }

    /**
     * Retourne le vrai stack virtuel utilisé par la main principale.
     * Le même objet est conservé pendant le placement afin que la décrémentation
     * vanilla soit observée puis sauvegardée une seule fois côté serveur.
     */
    public static ItemStack selectedStack(PlayerEntity player) {
        PaletteSelectionState state = state(player);
        if (state.selectedIndex < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack palette = player.getOffHandStack();
        if (!palette.isOf(ModItems.FOSSIL_PALETTE)
                || state.selectedIndex >= FossilPaletteItem.slots(palette)) {
            state.reset();
            return ItemStack.EMPTY;
        }

        ContainerComponent currentComponent = componentOf(palette);

        // Une synchronisation d'inventaire ou une édition des slots a remplacé
        // le composant de la Palette : on recharge alors la valeur autoritaire.
        if (!currentComponent.equals(state.loadedComponent)) {
            DefaultedList<ItemStack> contents = FossilPaletteItem.read(palette);
            state.cachedStack = contents.get(state.selectedIndex).copy();
            state.loadedComponent = currentComponent;
        }

        return state.cachedStack;
    }

    /**
     * Stack exposé comme main principale.
     *
     * <p>Côté client, Minecraft exécute une prédiction locale du placement avant
     * que le serveur traite le paquet. Renvoyer le cache persistant ici ferait
     * décrémenter la même représentation une première fois lors de la prédiction,
     * puis une seconde fois lors de la synchronisation autoritaire. Une copie
     * jetable est donc utilisée côté client ; seul le cache serveur est persistant
     * et peut être écrit dans la Palette.</p>
     */
    public static ItemStack handStack(PlayerEntity player) {
        ItemStack stack = selectedStack(player);
        return player.getWorld().isClient() ? stack.copy() : stack;
    }

    /**
     * Sauvegarde le stack virtuel dans la Palette sans modifier la sélection.
     */
    public static void flush(PlayerEntity player) {
        PaletteSelectionState state = state(player);
        if (state.selectedIndex >= 0) {
            flush(player, state);
        }
    }

    /**
     * Force le prochain accès à relire le contenu réel tout en gardant l'index.
     * Utile après une modification directe des slots de l'inventaire.
     */
    public static void invalidateCache(PlayerEntity player) {
        PaletteSelectionState state = state(player);
        if (state.selectedIndex >= 0) {
            state.loadedComponent = null;
        }
    }

    private static void flush(PlayerEntity player, PaletteSelectionState state) {
        if (state.selectedIndex < 0) {
            return;
        }

        // Le client ne doit jamais écrire sa prédiction dans le composant
        // CONTAINER. Le serveur est l'unique source autoritaire du contenu.
        if (player.getWorld().isClient()) {
            return;
        }

        ItemStack palette = player.getOffHandStack();
        if (!palette.isOf(ModItems.FOSSIL_PALETTE)
                || state.selectedIndex >= FossilPaletteItem.slots(palette)) {
            state.reset();
            return;
        }

        DefaultedList<ItemStack> contents = FossilPaletteItem.read(palette);
        ItemStack stored = contents.get(state.selectedIndex);

        if (stored.getCount() != state.cachedStack.getCount()
                || !ItemStack.areItemsAndComponentsEqual(stored, state.cachedStack)) {
            contents.set(state.selectedIndex, state.cachedStack.copy());
            FossilPaletteItem.write(palette, contents);
        }

        state.loadedComponent = componentOf(palette);
    }

    private static PaletteSelectionState state(PlayerEntity player) {
        if (player.getInventory() instanceof PaletteSelectionHolder holder) {
            return holder.mythicrpg$getPaletteSelectionState();
        }
        throw new IllegalStateException("PlayerInventory palette mixin is unavailable");
    }

    private static ContainerComponent componentOf(ItemStack palette) {
        return palette.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
    }
}
