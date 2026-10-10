package com.tlmpersonal.tlmpersonaldimension.command;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

public class DomainCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("domain")
            .requires(src -> src.hasPermission(2))

            // /domain summon <structure> [duration] [cost]
            .then(Commands.literal("summon")
                .then(Commands.argument("structure", StringArgumentType.word())
                    .executes(ctx -> summon(ctx,
                            StringArgumentType.getString(ctx, "structure"),
                            -1, true))
                    .then(Commands.argument("duration", IntegerArgumentType.integer(-1))
                        .executes(ctx -> summon(ctx,
                                StringArgumentType.getString(ctx, "structure"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                true))
                        .then(Commands.argument("cost", BoolArgumentType.bool())
                            .executes(ctx -> summon(ctx,
                                    StringArgumentType.getString(ctx, "structure"),
                                    IntegerArgumentType.getInteger(ctx, "duration"),
                                    BoolArgumentType.getBool(ctx, "cost")))))))

            // /domain destroy [all | <structure>]
            .then(Commands.literal("destroy")
                .executes(ctx -> destroy(ctx, null))                          // /domain destroy  → all
                .then(Commands.literal("all")
                    .executes(ctx -> destroy(ctx, null)))                     // /domain destroy all
                .then(Commands.argument("structure", StringArgumentType.word())
                    .executes(ctx -> destroy(ctx,
                            StringArgumentType.getString(ctx, "structure")))))

            // Read-only check for legacy island templates. It never changes a world.
            .then(Commands.literal("inspect-island-template")
                .executes(DomainCommand::inspectIslandTemplate))
            .then(Commands.literal("inspect-island-entities")
                .executes(DomainCommand::inspectIslandEntities)));
    }

    // ── summon ───────────────────────────────────────────────────────────────

    private static int summon(CommandContext<CommandSourceStack> ctx,
                              String structure, int duration, boolean cost) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level;
        double x, y, z;

        try {
            ServerPlayer player = src.getPlayerOrException();
            level = (ServerLevel) player.level();
            x = player.getX();
            y = player.getY() - 1;
            z = player.getZ();
        } catch (Exception e) {
            // Executed from console / command block — use source position
            level = src.getLevel();
            x = src.getPosition().x;
            y = src.getPosition().y - 1;
            z = src.getPosition().z;
        }

        DomainExpansionEntity domain = Touhoulittlemaidpersonaldimension.DOMAIN_EXPANSION_ENTITY.get().create(level);
        if (domain == null) {
            src.sendFailure(Component.literal("Failed to create Domain Expansion entity."));
            return 0;
        }

        domain.moveTo(x, y, z, 0, 0);
        domain.setStructureOverride(structure);
        domain.setDurationOverride(duration);
        domain.setCostOverride(cost);

        // If executed by a player, set them as owner so effects/cost work,
        // and find their nearest maid to set as the domain maid
        try {
            ServerPlayer player = src.getPlayerOrException();
            domain.setOwnerId(player.getUUID());

            // Find the nearest maid owned by this player
            EntityMaid nearestMaid = level.getEntitiesOfClass(EntityMaid.class,
                            new net.minecraft.world.phys.AABB(x - 64, y - 64, z - 64, x + 64, y + 64, z + 64))
                    .stream()
                    .filter(m -> player.getUUID().equals(m.getOwnerUUID()))
                    .min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(player)))
                    .orElse(null);
            if (nearestMaid != null) {
                domain.setMaidId(nearestMaid.getUUID());
            }
        } catch (Exception ignored) {}

        level.addFreshEntity(domain);

        String durationMsg = duration < 0 ? "infinite" : duration + "s";
        src.sendSuccess(() -> Component.literal(
                "§aDomain Expansion summoned: §f" + structure +
                " §7| duration: §f" + durationMsg +
                " §7| cost: §f" + cost), true);
        return 1;
    }

    // ── destroy ──────────────────────────────────────────────────────────────

    private static int destroy(CommandContext<CommandSourceStack> ctx, String structureFilter) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();

        List<DomainExpansionEntity> domains = level.getEntitiesOfClass(
                DomainExpansionEntity.class,
                net.minecraft.world.phys.AABB.ofSize(src.getPosition(), 60000, 60000, 60000));

        if (domains.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7No active Domain Expansions found."), false);
            return 0;
        }

        int count = 0;
        for (DomainExpansionEntity domain : domains) {
            if (structureFilter == null || matchesStructure(domain, structureFilter)) {
                domain.callRestoreAndDiscard();
                count++;
            }
        }

        if (count == 0) {
            src.sendSuccess(() -> Component.literal("§7No Domain Expansions matching '§f" + structureFilter + "§7' found."), false);
            return 0;
        }

        final int finalCount = count;
        String target = structureFilter != null ? " matching '§f" + structureFilter + "§7'" : "";
        src.sendSuccess(() -> Component.literal("§aDestroyed §f" + finalCount + "§a Domain Expansion(s)" + target + "."), true);
        return finalCount;
    }

    private static boolean matchesStructure(DomainExpansionEntity domain, String filter) {
        // DomainExpansionEntity exposes structure name via NBT tag "StructureOverride"
        // We can read it by delegating to the entity
        String name = domain.getActiveStructureName();
        return name != null && name.equalsIgnoreCase(filter);
    }

    private static int inspectIslandTemplate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(
                Touhoulittlemaidpersonaldimension.MODID, "my_island");
        Optional<StructureTemplate> template = level.getServer().getStructureManager().get(id);
        if (template.isEmpty()) {
            source.sendFailure(Component.literal("Could not load the my_island structure template."));
            return 0;
        }

        net.minecraft.nbt.CompoundTag serialized = template.get().save(new net.minecraft.nbt.CompoundTag());
        net.minecraft.nbt.ListTag entities = serialized.getList(StructureTemplate.ENTITIES_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND);
        int passengers = 0;
        for (int i = 0; i < entities.size(); i++) {
            net.minecraft.nbt.CompoundTag entity = entities.getCompound(i).getCompound(StructureTemplate.ENTITY_TAG_NBT);
            if (entity.contains("Passengers", net.minecraft.nbt.Tag.TAG_LIST)) passengers++;
        }

        int count = entities.size();
        int passengerCount = passengers;
        source.sendSuccess(() -> Component.literal("my_island template: " + count
                + " saved entities, " + passengerCount + " with passengers. No world data was changed."), false);
        return count;
    }

    private static int inspectIslandEntities(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        List<Entity> entities = level.getEntities((Entity) null,
                new net.minecraft.world.phys.AABB(-38, 150, -36, 38, 221, 36));
        int passengerRoots = 0;
        int mountedEntities = 0;
        for (Entity entity : entities) {
            if (entity.isPassenger()) mountedEntities++;
            if (!entity.getPassengers().isEmpty()) passengerRoots++;
        }

        int entityCount = entities.size();
        int rootCount = passengerRoots;
        int mountedCount = mountedEntities;
        source.sendSuccess(() -> Component.literal("Island range in " + level.dimension().location()
                + ": " + entityCount + " loaded entities, " + rootCount + " passenger roots, "
                + mountedCount + " passengers. No world data was changed."), false);
        return entityCount;
    }
}
