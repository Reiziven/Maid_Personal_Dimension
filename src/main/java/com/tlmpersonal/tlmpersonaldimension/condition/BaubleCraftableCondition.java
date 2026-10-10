package com.tlmpersonal.tlmpersonaldimension.condition;

import com.google.gson.JsonObject;
import com.tlmpersonal.tlmpersonaldimension.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public class BaubleCraftableCondition implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation(
            "touhoulittlemaidpersonaldimension", "bauble_craftable");

    private final String bauble;

    public BaubleCraftableCondition(String bauble) {
        this.bauble = bauble;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return switch (bauble) {
            case "domain_expansion_bauble"  -> Config.DOMAIN_EXPANSION_BAUBLE_CRAFTABLE.get();
            case "cherry_domain_bauble"     -> Config.CHERRY_DOMAIN_BAUBLE_CRAFTABLE.get();
            case "cat_familiar_bauble"      -> Config.CAT_FAMILIAR_BAUBLE_CRAFTABLE.get();
            case "tethered_teleport_bauble" -> Config.TETHERED_TELEPORT_BAUBLE_CRAFTABLE.get();
            default -> true;
        };
    }

    /** Serializer registered via CraftingHelper.register in FMLCommonSetupEvent. */
    public static class Serializer implements IConditionSerializer<BaubleCraftableCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ResourceLocation getID() {
            return ID;
        }

        @Override
        public BaubleCraftableCondition read(JsonObject json) {
            return new BaubleCraftableCondition(json.get("bauble").getAsString());
        }

        @Override
        public void write(JsonObject json, BaubleCraftableCondition value) {
            json.addProperty("bauble", value.bauble);
        }
    }
}
