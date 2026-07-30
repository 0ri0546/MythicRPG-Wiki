package com.mythicrpg.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SkillProgress {
    public static final int MAX_LEVEL = 100;
    public static final int MAX_SKILL_POINTS = 10_000;

    private static final double XP_CURVE_COEFFICIENT = 19.5D;
    private static final double XP_CURVE_EXPONENT = 0.04D;

    public static final Codec<SkillProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(SkillProgress::getLevel),
            Codec.INT.fieldOf("xp").forGetter(SkillProgress::getXp),
            Codec.INT.fieldOf("skillPoints").forGetter(SkillProgress::getSkillPoints)
    ).apply(instance, SkillProgress::new));

    private int level;
    private int xp;
    private int skillPoints;

    public SkillProgress() {
        this(1, 0, 1);
    }

    public SkillProgress(int level, int xp, int skillPoints) {
        this.level = Math.clamp(level, 1, MAX_LEVEL);
        this.xp = this.level >= MAX_LEVEL
                ? 0
                : Math.clamp(xp, 0, Math.max(0, xpRequiredForLevel(this.level) - 1));
        this.skillPoints = Math.clamp(skillPoints, 0, MAX_SKILL_POINTS);
    }

    public void addXp(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) {
            return;
        }
        long accumulatedXp = Math.min(Integer.MAX_VALUE, (long) xp + amount);
        xp = (int) accumulatedXp;
        while (level < MAX_LEVEL && xp >= xpRequiredForLevel(level)) {
            xp -= xpRequiredForLevel(level);
            level++;
            addSkillPoints(pointsGrantedForLevel(level));
        }
        if (level >= MAX_LEVEL) {
            xp = 0;
        }
    }

    public static int xpRequiredForLevel(int level) {
        return Math.max(1, (int) Math.round(
                XP_CURVE_COEFFICIENT * Math.pow(level, XP_CURVE_EXPONENT)
        ));
    }

    public static int pointsGrantedForLevel(int level) {
        return 1;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public boolean spendPoints(int amount) {
        if (amount <= 0 || skillPoints < amount) {
            return false;
        }
        skillPoints -= amount;
        return true;
    }

    public void addSkillPoints(int amount) {
        long updated = (long) this.skillPoints + amount;
        this.skillPoints = (int) Math.clamp(updated, 0L, (long) MAX_SKILL_POINTS);
    }

    public SkillProgress normalizedCopy() {
        return new SkillProgress(level, xp, skillPoints);
    }

    public boolean isNormalized() {
        SkillProgress normalized = normalizedCopy();
        return level == normalized.level && xp == normalized.xp && skillPoints == normalized.skillPoints;
    }
}
