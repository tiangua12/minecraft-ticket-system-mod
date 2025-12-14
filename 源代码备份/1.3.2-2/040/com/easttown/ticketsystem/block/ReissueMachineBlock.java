package com.easttown.ticketsystem.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import com.easttown.ticketsystem.util.DebugLogger;

import javax.annotation.Nullable;

public class ReissueMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // 下半部分碰撞箱（完整高度）
    private static final VoxelShape SHAPE_LOWER = Block.box(0, 0, 0, 16, 32, 16);
    // 上半部分碰撞箱（相对于下半部分的位置）
    private static final VoxelShape SHAPE_UPPER = Block.box(0, -16, 0, 16, 16, 16);

    public ReissueMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HALF);
    }

    // 放置逻辑
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();

        // 检查上方空间是否可用
        if (clickedPos.getY() < level.getMaxBuildHeight() - 1 &&
            level.getBlockState(clickedPos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
        } else {
            return null; // 放置失败
        }
    }

    // 自动创建上半部分
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                           LivingEntity entity, ItemStack stack) {
        // 在下方位置的上方放置上半部分
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    // 生存检查机制
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            // 下半部分：需要下方有坚固的支撑面
            return belowState.isFaceSturdy(level, belowPos, Direction.UP);
        } else {
            // 上半部分：需要下方是同一方块的下半部分
            return belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
    }

    // 方块更新同步
    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                 LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf blockHalf = state.getValue(HALF);

        // 处理垂直方向的更新（上下部分连接处）
        if (facing.getAxis() == Direction.Axis.Y &&
            blockHalf == DoubleBlockHalf.LOWER == (facing == Direction.UP)) {

            boolean condition = facingState.is(this) && facingState.getValue(HALF) != blockHalf;
            return condition ?
                state.setValue(FACING, facingState.getValue(FACING)) :
                Blocks.AIR.defaultBlockState(); // 如果另一半丢失，破坏当前部分
        } else {
            // 下半部分检查下方支撑
            boolean condition = blockHalf == DoubleBlockHalf.LOWER &&
                               facing == Direction.DOWN &&
                               !state.canSurvive(level, currentPos);
            return condition ? Blocks.AIR.defaultBlockState() :
                   super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        }
    }

    // 破坏连锁反应
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf value = state.getValue(HALF);
        BlockPos otherPos = value == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(otherPos);

        if (otherState.is(this) && otherState.getValue(HALF) != value) {
            if (!level.isClientSide) {
                if (player.isCreative()) {
                    // 创造模式：直接破坏另一部分，不产生掉落
                    level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                } else {
                    // 生存模式：只在下半部分产生掉落，避免重复掉落
                    if (value == DoubleBlockHalf.LOWER) {
                        // 破坏下半部分时，上半部分自动破坏但不产生掉落
                        level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                    } else {
                        // 破坏上半部分时，下半部分保留，不产生额外掉落
                        level.setBlock(otherPos, otherState, Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                    }
                }
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    // 碰撞箱管理
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return SHAPE_LOWER; // 完整高度的碰撞箱
        } else {
            return SHAPE_UPPER; // 上半部分较矮的碰撞箱
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    // 交互逻辑
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {

        // 统一使用下半部分的位置进行方块实体操作
        BlockPos tePos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockEntity blockEntity = level.getBlockEntity(tePos);

        if (blockEntity instanceof ReissueMachineBlockEntity terminal) {
            DebugLogger.info("方块使用 - 位置: {}, 客户端: {}", tePos, level.isClientSide());

            if (level.isClientSide()) {
                // 客户端直接打开界面
                terminal.openScreen(player);
            } else if (player instanceof ServerPlayer serverPlayer) {
                // 服务端打开界面
                NetworkHooks.openScreen(serverPlayer, terminal, tePos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // 方块实体集成
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 只在下半部分创建方块实体
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new ReissueMachineBlockEntity(pos, state);
        }
        return null;
    }
}