package com.easttown.ticketsystem.block;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.init.BlockEntityInit;
import com.easttown.ticketsystem.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ticks.TickPriority;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Map;

public class GateBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
    // 关闭状态下的碰撞箱（H形）
    private static final Map<Direction, VoxelShape> CLOSED_SHAPES = Map.of(
        Direction.NORTH, Shapes.or(
            Block.box(0, 0, 0, 1, 32, 16),   // 左柱
            Block.box(15, 0, 0, 16, 32, 16), // 右柱
            Block.box(1, 0, 7, 15, 32, 8)    // 横梁
        ),
        Direction.SOUTH, Shapes.or(
            Block.box(15, 0, 0, 16, 32, 16),
            Block.box(0, 0, 0, 1, 32, 16),
            Block.box(1, 0, 8, 15, 32, 9)
        ),
        Direction.EAST, Shapes.or(
            Block.box(0, 0, 0, 16, 32, 1),
            Block.box(0, 0, 15, 16, 32, 16),
            Block.box(8, 0, 0, 9, 32, 16)
        ),
        Direction.WEST, Shapes.or(
            Block.box(0, 0, 15, 16, 32, 16),
            Block.box(0, 0, 0, 16, 32, 1),
            Block.box(7, 0, 0, 8, 32, 16)
        )
    );

    // 开启状态下的碰撞箱（只有两侧立柱）
    private static final Map<Direction, VoxelShape> OPEN_SHAPES = Map.of(
        Direction.NORTH, Shapes.or(
            Block.box(0, 0, 0, 1, 32, 16),
            Block.box(15, 0, 0, 16, 32, 16)
        ),
        Direction.SOUTH, Shapes.or(
            Block.box(15, 0, 0, 16, 32, 16),
            Block.box(0, 0, 0, 1, 32, 16)
        ),
        Direction.EAST, Shapes.or(
            Block.box(0, 0, 0, 16, 32, 1),
            Block.box(0, 0, 15, 16, 32, 16)
        ),
        Direction.WEST, Shapes.or(
            Block.box(0, 0, 15, 16, 32, 16),
            Block.box(0, 0, 0, 16, 32, 1)
        )
    );

    public GateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(WATERLOGGED, false));
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        boolean isOpen = state.getValue(OPEN);
        
        // 根据OPEN状态返回不同的碰撞箱
        if (isOpen) {
            return OPEN_SHAPES.get(direction);
        } else {
            return CLOSED_SHAPES.get(direction);
        }
    }

    // 获取碰撞形状（只返回闸机主体的碰撞箱）
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        boolean isOpen = state.getValue(OPEN);
        
        return isOpen ? OPEN_SHAPES.get(direction) : CLOSED_SHAPES.get(direction);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, WATERLOGGED);
    }
    
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof GateBlockEntity gate) {
                // 检查是否有管理员钥匙
                boolean isAdmin = player.getItemInHand(hand).getItem() == ItemInit.ADMIN_KEY.get();
                
                if (isAdmin) {
                    // 管理员打开配置界面
                    gate.openConfigGui(serverPlayer);
                    return InteractionResult.SUCCESS;
                } else {
                    // 普通玩家尝试通过闸机
                    gate.tryPassThrough(player);
                    return InteractionResult.SUCCESS;
                }
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
        return BlockEntityInit.GATE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, BlockEntityInit.GATE.get(), GateBlockEntity::tick);
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        
        // 确保只在服务器端执行
        if (!level.isClientSide) {
            TicketSystemMod.LOGGER.info("闸机方块已放置于位置: " + pos);
            
            // 获取方块实体并初始化
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof GateBlockEntity gate) {
                TicketSystemMod.LOGGER.info("闸机方块实体初始化完成");
            } else {
                TicketSystemMod.LOGGER.warn("闸机方块放置后未找到方块实体！位置: " + pos);
            }
        }
    }
    
    // 添加闸机关闭的tick处理
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GateBlockEntity gate) {
            gate.closeGate();
        }
    }
}
