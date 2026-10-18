package com.btcadds.cleanupcycle;

import com.btcadds.cleanupcycle.logic.RevieverBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.network.chat.Component;



public class RevieverBlock extends HorizontalDirectionalBlock implements EntityBlock
{
    
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    public RevieverBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
        
    }
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

     @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }
     @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
     @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    // Дальше по блоку

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RevieverBlockEntity(pos, state);
    }
   @Override
public void onRemove(BlockState state, Level level, BlockPos pos,
                     BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
        for (ServerPlayer p : serverLevel.players()) {
            CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (persisted.contains("reviver_pos")) {
                BlockPos boundPos = BlockPos.of(persisted.getLong("reviver_pos"));
                if (boundPos.equals(pos)) {
                    persisted.remove("reviver_pos");
                    persisted.remove("reviver_dim");
                    p.setRespawnPosition(null, null, 0.0F, false, false);
                }
            }
        }
    }
    super.onRemove(state, level, pos, newState, isMoving);
}


   
@Override
public InteractionResult use(BlockState state, Level level, BlockPos pos,
                             Player player, InteractionHand hand, BlockHitResult hit) {
    if (level.isClientSide) {
        return InteractionResult.SUCCESS;
    }
    if (!(level.getBlockEntity(pos) instanceof RevieverBlockEntity blockEntity)) {
        return InteractionResult.PASS;
    }
    if (player instanceof ServerPlayer serverPlayer) {
        if (player.isSecondaryUseActive()) { // Shift+ПКМ — привязать точку возрождения
            if (blockEntity.getCharges() <= 0) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.cleanupcycle.no_charges"), true);
            } else {
                // bindToAnchor=true: возрождаться на позиции, даже если блок потом сломают
                // (заряд всё равно проверим в PlayerRespawnEvent)
                serverPlayer.setRespawnPosition(level.dimension(), pos,
        state.getValue(FACING).toYRot(), true, false);

                // Forge при смерти копирует новому игроку ТОЛЬКО вложенный тег
                // Player.PERSISTED_NBT_TAG ("PlayerPersisted") внутри persistent data,
                // поэтому привязку пишем именно туда — иначе она теряется после смерти.
                CompoundTag forgeData = serverPlayer.getPersistentData();
                if (!forgeData.contains(Player.PERSISTED_NBT_TAG)) {
                    forgeData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
                }
                CompoundTag persisted = forgeData.getCompound(Player.PERSISTED_NBT_TAG);
                persisted.putLong("reviver_pos", pos.asLong());
                persisted.putString("reviver_dim", level.dimension().location().toString());

                serverPlayer.displayClientMessage(
                        Component.translatable("message.cleanupcycle.bound"), true);
            }
        } else { // обычный ПКМ — GUI
            NetworkHooks.openScreen(serverPlayer, blockEntity, pos);
        }
    }
    return InteractionResult.CONSUME;
}

@Override
public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
    return SHAPE;
}

}