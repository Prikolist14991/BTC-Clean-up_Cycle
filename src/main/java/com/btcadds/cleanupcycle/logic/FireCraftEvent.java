package com.btcadds.cleanupcycle.logic;

import com.btcadds.cleanupcycle.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "cleanupcycle")
public class FireCraftEvent {

    private static final String DEP_MOD_ID = "fluid"; //corner_progress
    private static final String FLUID_BALLS = "fluid_ball";
    private static final double SEARCH_RADIUS = 32.0;

    private static Item cachedStone1 = null;
    private static boolean lookupDone = false;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side != net.minecraftforge.fml.LogicalSide.SERVER || event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        // тест на зависимость
        Item fluidball = getAnotherBlock();
        if (fluidball == null) return; 

        for (ServerPlayer player : serverLevel.players()) {
            AABB searchArea = player.getBoundingBox().inflate(SEARCH_RADIUS);

            for (ItemEntity itemEntity : serverLevel.getEntitiesOfClass(ItemEntity.class, searchArea, ItemEntity::isOnFire)) {
                ItemStack stack = itemEntity.getItem();
                if (stack.is(fluidball) && stack.getCount() >= 64) {
                    itemEntity.setItem(new ItemStack(ExampleMod.JIJA_HEART.get()));
                    itemEntity.clearFire();
                }
            }
        }
    }


    private static Item getAnotherBlock() {
        if (lookupDone) return cachedStone1;

        // Проверяем наличие зависимого мода
        if (!ModList.get().isLoaded(DEP_MOD_ID)) {
            return null; // две проверки чтоб наверняка
        }

        ResourceLocation stone1Loc = ResourceLocation.fromNamespaceAndPath(DEP_MOD_ID, FLUID_BALLS);

        cachedStone1 = ForgeRegistries.ITEMS.getValue(stone1Loc);
        lookupDone = true;

        return cachedStone1;
    }
}