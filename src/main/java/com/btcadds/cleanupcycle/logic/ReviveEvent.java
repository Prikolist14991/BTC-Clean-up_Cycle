package com.btcadds.cleanupcycle.logic;

import com.btcadds.cleanupcycle.ExampleMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ReviveEvent {
    private static final String TAG_POS = "reviver_pos";
    private static final String TAG_DIM = "reviver_dim";

    private static final int TELEPORT_DELAY_TICKS = 20;

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(TAG_POS)) return;

        BlockPos boundPos = BlockPos.of(persisted.getLong(TAG_POS));
        ResourceLocation boundDim = ResourceLocation.tryParse(persisted.getString(TAG_DIM));
        if (boundDim == null) return;
        ResourceKey<Level> boundKey = ResourceKey.create(Registries.DIMENSION, boundDim);
        if (!boundKey.equals(level.dimension())) return;

        boolean revived = level.getBlockEntity(boundPos) instanceof RevieverBlockEntity be
                && be.consumeCharge();

        if (revived) {
            teleportToReviever(player, boundPos);
            if (level instanceof ServerLevel serverLevel) {
                PENDING_TELEPORTS.put(player.getUUID(),
                        new PendingTeleport(boundPos, serverLevel.getGameTime() + TELEPORT_DELAY_TICKS));
            }
        } else {
            persisted.remove(TAG_POS);
            persisted.remove(TAG_DIM);
            player.setRespawnPosition(null, null, 0.0F, false, false);
            player.displayClientMessage(
                    Component.translatable("message.cleanupcycle.no_charges_revive"), true);
        }
    }

    private record PendingTeleport(BlockPos pos, long executeAtTick) {}
    private static final Map<UUID, PendingTeleport> PENDING_TELEPORTS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_TELEPORTS.isEmpty()) return;

        MinecraftServer server = event.getServer();
        long gameTime = server.overworld().getGameTime();

        PENDING_TELEPORTS.entrySet().removeIf(entry -> {
            PendingTeleport pending = entry.getValue();
            if (gameTime < pending.executeAtTick()) return false;

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isAlive()) {
                teleportToReviever(player, pending.pos());
            }
            return true; 
        });
    }


    private static void teleportToReviever(ServerPlayer player, BlockPos boundPos) {
        double x = boundPos.getX() + 0.5;
        double y = boundPos.getY() + 1.0;
        double z = boundPos.getZ() + 0.5;
        player.teleportTo(x, y, z);
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        player.displayClientMessage(
                Component.translatable("message.cleanupcycle.revived"), true);
    }
}