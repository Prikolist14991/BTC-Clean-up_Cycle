package com.btcadds.cleanupcycle.client;

import com.btcadds.cleanupcycle.ExampleMod;
import com.btcadds.cleanupcycle.logic.RevieverBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class RevieverMenu extends AbstractContainerMenu {

    private static final int CONTAINER_SLOTS = 2;
    private final RevieverBlockEntity blockEntity;
    private final ContainerData data;
    

    public RevieverMenu(int windowId, Inventory playerInventory, RevieverBlockEntity blockEntity, ContainerData data) {
        super(ExampleMod.REVIEVER_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.addDataSlots(data);

        // Слоты 
        this.addSlot(new Slot(blockEntity.getContainer(), 0, 80, 21));
        this.addSlot(new Slot(blockEntity.getContainer(), 1, 80, 57));

        // инвентарь
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // инвентарь игорька
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public RevieverMenu(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(windowId, playerInventory, resolveBlockEntity(playerInventory, extraData), new SimpleContainerData(1));
    }

    private static RevieverBlockEntity resolveBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof RevieverBlockEntity revieverBlockEntity) {
            return revieverBlockEntity;
        }
        return new RevieverBlockEntity(pos, playerInventory.player.level().getBlockState(pos));
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (blockEntity.getLevel() != null) {
            blockEntity.setChanged();
            blockEntity.tryUseRecipe();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < CONTAINER_SLOTS) {
                if (!this.moveItemStackTo(stackInSlot, CONTAINER_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, CONTAINER_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }



    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity.getBlockPos().distToCenterSqr(player.position()) <= 64.0D;
    }
    
   public RevieverBlockEntity getBlockEntity() {
    return this.blockEntity;
}

    public int getCharges() {
        return this.data.get(0);
    }
}
