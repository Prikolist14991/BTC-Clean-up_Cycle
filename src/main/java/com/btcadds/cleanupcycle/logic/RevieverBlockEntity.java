package com.btcadds.cleanupcycle.logic;

import com.btcadds.cleanupcycle.ExampleMod;
import com.btcadds.cleanupcycle.client.RevieverMenu;


import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class RevieverBlockEntity extends BlockEntity implements MenuProvider, net.minecraft.world.inventory.ContainerData {

    private static final String ONE_MOD_ID = "fluid";
    private static final String FLUID_BALLS = "fluid_ball";
    private static final String TWO_MOD_ID = "corner_progress";
    private static final String BATTAREY = "battery";
    private static final String FLUID_BOTTLE = "fluid_bottle";
    private boolean loading = false;
    private boolean crafting = false;
    //public static final int MAX_CHARGES = 4;
    private int charges = 0;

    // Кэш предметов из зависимых модов (поиск по реестру — один раз)
    private static final Map<String, Item> ITEM_CACHE = new HashMap<>();
    

    private final SimpleContainer container = new SimpleContainer(2); 

    public RevieverBlockEntity(BlockPos pos, BlockState state) {
    super(ExampleMod.REVIEVER_BE.get(), pos, state);
    this.container.addListener(this::containerChanged); 
}

// рецепт \ конт

private void containerChanged(Container inv) {
    if (loading || crafting) return;
    Level level = getLevel();
    if (level == null || level.isClientSide) return;
    tryUseRecipe();
}
public void tryUseRecipe() {
    this.crafting = true;
    try {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        ItemStack slot0 = container.getItem(0);
        ItemStack slot1 = container.getItem(1);

        boolean matches = false;
        int remove0 = 0;

        // Предметы из зависимых модов (кэшируются после первого поиска)
        Item fluidBall = getCachedItem(ONE_MOD_ID, FLUID_BALLS);
        Item fluidBottle = getCachedItem(ONE_MOD_ID, FLUID_BOTTLE);
        Item battery = getCachedItem(TWO_MOD_ID, BATTAREY);
        if (fluidBall == null || battery == null) return; // Зависимые моды не подключены — пропуск

        if (slot0.is(fluidBall) && slot0.getCount() >= 64) { matches = true; remove0 = 64; }
        else if (slot0.is(fluidBottle) && slot0.is(fluidBottle) && slot0.getCount() >= 16) { matches = true; remove0 = 16; }

        if (matches && slot1.is(battery) && slot1.getCount() >= 4) {
            if (charges >= 4) {
            return;}

            container.removeItem(0, remove0);
            container.removeItem(1, 4);
            charges++;
            setChanged();
        } else {
            matches = false;
        }
    } finally {
        this.crafting = false;
    }
}

 // заряды и возрождение

    public int getCharges() { return charges; }

public boolean consumeCharge() {
    if (charges > 0) {
        charges--;
        setChanged();
        return true;
    }
    return false;
}


// конты б\и

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", container.createTag());
        tag.putInt("charges", charges);
    }

    @Override
    public void load(CompoundTag tag) {
        this.loading = true;
        super.load(tag);
        container.fromTag(tag.getList("inventory", 10));
        charges = Math.max(0, Math.min(4, tag.getInt("charges")));
        loading = false;
    }

    public SimpleContainer getContainer() {
        return container;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cleanupcycle.reviever_block");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new RevieverMenu(windowId, playerInventory, this, this);
    }

    // передача зарядов на клиент для отображения в GUI
    @Override
    public int get(int id) {
        return id == 0 ? charges : 0;
    }

    @Override
    public void set(int id, int value) {
        if (id == 0) charges = value;   // клиентская копия 
    }

    @Override
    public int getCount() { return 1; } 

    private static Item getCachedItem(String modId, String itemName) {
        String key = modId + ":" + itemName;
        if (ITEM_CACHE.containsKey(key)) return ITEM_CACHE.get(key);

        Item item = null;
        if (ModList.get().isLoaded(modId)) {
            item = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath(modId, itemName));
        }
        ITEM_CACHE.put(key, item);
        return item;
    }


}