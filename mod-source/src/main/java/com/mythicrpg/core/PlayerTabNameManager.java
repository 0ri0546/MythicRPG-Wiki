package com.mythicrpg.core;

import com.mythicrpg.titles.TitleManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class PlayerTabNameManager {

    private static final String TEAM_PREFIX = "mythic_";

    private PlayerTabNameManager() {
    }

    public static void refresh(ServerPlayerEntity player) {
        if (player.getServer() == null) {
            return;
        }

        Scoreboard scoreboard = player.getServer().getScoreboard();
        String playerName = player.getNameForScoreboard();
        String teamName = getTeamName(player);
        Team ownTitleTeam = scoreboard.getTeam(teamName);
        Team currentTeam = scoreboard.getScoreHolderTeam(playerName);
        MutableText titlePrefix = TitleManager.buildEquippedPrefix(player);

        // A player without an equipped title does not need a synthetic team.
        // This also cleans the per-player teams created by the former automatic
        // level/title prefix system when an existing world is upgraded.
        if (titlePrefix.getString().isEmpty()) {
            if (ownTitleTeam != null) {
                if (currentTeam == ownTitleTeam) {
                    scoreboard.removeScoreHolderFromTeam(playerName, ownTitleTeam);
                }
                if (ownTitleTeam.getPlayerList().isEmpty()) {
                    scoreboard.removeTeam(ownTitleTeam);
                }
            }
            return;
        }

        if (ownTitleTeam == null) {
            ownTitleTeam = scoreboard.addTeam(teamName);
        }

        // Team decoration is shared by the player list and vanilla display-name
        // pipeline. Only the prefix is styled; the player name and message retain
        // their vanilla style because the separator appended by TitleManager has
        // an explicit empty style.
        ownTitleTeam.setPrefix(titlePrefix);
        ownTitleTeam.setSuffix(Text.empty());

        if (currentTeam != ownTitleTeam) {
            scoreboard.addScoreHolderToTeam(playerName, ownTitleTeam);
        }
    }

    private static String getTeamName(ServerPlayerEntity player) {
        String uuidPart = player.getUuidAsString().replace("-", "").substring(0, 8);
        return TEAM_PREFIX + uuidPart;
    }
}
