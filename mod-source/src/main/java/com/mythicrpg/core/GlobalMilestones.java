package com.mythicrpg.core;

import java.util.List;

public final class GlobalMilestones {

    private GlobalMilestones() {
    }

    public record Milestone(int level, String title) {
    }

    private static final List<Milestone> MILESTONES = List.of(
            new Milestone(12, "Nouveau venu"),
            new Milestone(25, "Farmer du dimanche"),
            new Milestone(50, "Apprenti aventurier"),
            new Milestone(100, "Aventurier confirmé"),
            new Milestone(150, "Habitué du monde"),
            new Milestone(250, "Vétéran"),
            new Milestone(400, "Maître polyvalent"),
            new Milestone(600, "Légende locale"),
            new Milestone(800, "Mythique")
    );

    public static List<Milestone> getMilestones() {
        return MILESTONES;
    }

    public static String getTitleForLevel(int globalLevel) {
        String currentTitle = "Nouveau venu";

        for (Milestone milestone : MILESTONES) {
            if (globalLevel >= milestone.level()) {
                currentTitle = milestone.title();
            } else {
                break;
            }
        }

        return currentTitle;
    }

    public static Milestone getNextMilestone(int globalLevel) {
        for (Milestone milestone : MILESTONES) {
            if (globalLevel < milestone.level()) {
                return milestone;
            }
        }

        return null;
    }
}