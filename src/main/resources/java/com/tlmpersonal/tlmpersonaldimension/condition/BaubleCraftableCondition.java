package com.tlmpersonal.tlmpersonaldimension.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tlmpersonal.tlmpersonaldimension.Config;
import net.neoforged.neoforge.common.conditions.ICondition;

public record BaubleCraftableCondition(String bauble) implements ICondition {

    public static final MapCodec<BaubleCraftableCondition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.STRING.fieldOf("bauble").forGetter(BaubleCraftableCondition::bauble)
            ).apply(instance, BaubleCraftableCondition::new)
    );

    @Override
    public boolean test(ICondition.IContext context) {
        return switch (bauble) {
            case "domain_expansion_bauble" -> Config.DOMAIN_EXPANSION_BAUBLE_CRAFTABLE.get();
            case "cherry_domain_bauble"    -> Config.CHERRY_DOMAIN_BAUBLE_CRAFTABLE.get();
            case "cat_familiar_bauble"     -> Config.CAT_FAMILIAR_BAUBLE_CRAFTABLE.get();
            case "tethered_teleport_bauble"-> Config.TETHERED_TELEPORT_BAUBLE_CRAFTABLE.get();
            default -> true;
        };
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
