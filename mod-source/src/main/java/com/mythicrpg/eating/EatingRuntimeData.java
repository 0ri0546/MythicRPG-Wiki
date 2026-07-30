package com.mythicrpg.eating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EatingRuntimeData(
        long chefAuraReadyAt,
        long riskTasteReadyAt,
        int mealStage,
        long mealDeadline,
        long signatureReadyAt,
        String activeSignatureBonus,
        long activeSignatureExpiresAt
) {
    public static final Codec<EatingRuntimeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("chef_aura_ready_at", 0L).forGetter(EatingRuntimeData::chefAuraReadyAt),
            Codec.LONG.optionalFieldOf("risk_taste_ready_at", 0L).forGetter(EatingRuntimeData::riskTasteReadyAt),
            Codec.INT.optionalFieldOf("meal_stage", 0).forGetter(EatingRuntimeData::mealStage),
            Codec.LONG.optionalFieldOf("meal_deadline", 0L).forGetter(EatingRuntimeData::mealDeadline),
            Codec.LONG.optionalFieldOf("signature_ready_at", 0L).forGetter(EatingRuntimeData::signatureReadyAt),
            Codec.STRING.optionalFieldOf("active_signature_bonus", "").forGetter(EatingRuntimeData::activeSignatureBonus),
            Codec.LONG.optionalFieldOf("active_signature_expires_at", 0L).forGetter(EatingRuntimeData::activeSignatureExpiresAt)
    ).apply(instance, EatingRuntimeData::new));

    public EatingRuntimeData {
        activeSignatureBonus = activeSignatureBonus == null ? "" : activeSignatureBonus;
    }

    public static EatingRuntimeData defaults() {
        return new EatingRuntimeData(0L, 0L, 0, 0L, 0L, "", 0L);
    }

    public EatingRuntimeData withChefAuraReadyAt(long value) {
        return new EatingRuntimeData(value, riskTasteReadyAt, mealStage, mealDeadline, signatureReadyAt,
                activeSignatureBonus, activeSignatureExpiresAt);
    }

    public EatingRuntimeData withRiskTasteReadyAt(long value) {
        return new EatingRuntimeData(chefAuraReadyAt, value, mealStage, mealDeadline, signatureReadyAt,
                activeSignatureBonus, activeSignatureExpiresAt);
    }

    public EatingRuntimeData withMeal(int stage, long deadline) {
        return new EatingRuntimeData(chefAuraReadyAt, riskTasteReadyAt, stage, deadline, signatureReadyAt,
                activeSignatureBonus, activeSignatureExpiresAt);
    }

    public EatingRuntimeData withSignatureReadyAt(long value) {
        return new EatingRuntimeData(chefAuraReadyAt, riskTasteReadyAt, mealStage, mealDeadline, value,
                activeSignatureBonus, activeSignatureExpiresAt);
    }

    public EatingRuntimeData withActiveSignature(SignatureBonus bonus, long expiresAt) {
        return new EatingRuntimeData(chefAuraReadyAt, riskTasteReadyAt, mealStage, mealDeadline, signatureReadyAt,
                bonus == null ? "" : bonus.id(), Math.max(0L, expiresAt));
    }
}
