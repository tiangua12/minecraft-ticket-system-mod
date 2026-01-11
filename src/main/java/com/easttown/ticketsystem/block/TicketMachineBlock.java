package com.easttown.ticketsystem.block;

import com.easttown.ticketsystem.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class TicketMachineBlock extends BaseEntityBlock {
    // 1. 添加方向属性
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TicketMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // 2. 注册默认状态（朝北）
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // 3. 将方向属性添加到状态定义
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    // 4. 设置放置时的方向（根据玩家朝向）
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // 以下原有方法保持不变 ▼▼▼
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TicketMachineBlockEntity ticketMachine) {
                // 计算相对点击位置
                double hitX = hit.getLocation().x - pos.getX();
                double hitY = hit.getLocation().y - pos.getY();
                double hitZ = hit.getLocation().z - pos.getZ();

                boolean isAdmin = player.getItemInHand(hand).getItem() == ItemInit.ADMIN_KEY.get();

                NetworkHooks.openScreen(serverPlayer, ticketMachine, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeBoolean(isAdmin);
                    buf.writeDouble(hitX);
                    buf.writeDouble(hitY);
                    buf.writeDouble(hitZ);
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TicketMachineBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TicketMachineBlockEntity ticketMachine) {
                ticketMachine.dropContents(level, pos);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
