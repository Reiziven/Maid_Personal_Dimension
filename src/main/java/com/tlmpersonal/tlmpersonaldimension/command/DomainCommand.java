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
import net.minecraft.world.phys.AABB;

import java.util.List;

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
                            StringArgumentType.getString(ctx, "structure"))))));
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

        // If executed by a player, set them as owner and find their nearest maid
        try {
            ServerPlayer player = src.getPlayerOrException();
            domain.setOwnerId(player.getUUID());

            // Find the nearest maid owned by this player
            EntityMaid nearestMaid = level.getEntitiesOfClass(
                            EntityMaid.class,
                            AABB.ofSize(player.position(), 128, 128, 128))
                    .stream()
                    .filter(m -> m.isAlive() && player.getUUID().equals(m.getOwnerUUID()))
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
}
