package com.createfueltank.content.fuel_tank;

import com.createfueltank.FuelTankBlockEntities;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Create's FluidTankItem with the block entity type swapped for ours. Its multi-place
 * helper (placing one tank on top of a wide multiblock fills the whole layer) is private
 * and hard-wired to Create's own tank type, so the logic is carried over rather than inherited.
 */
public class FuelTankItem extends BlockItem {

    public FuelTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult initialResult = super.place(context);
        if (!initialResult.consumesAction())
            return initialResult;
        tryMultiPlace(context);
        return initialResult;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        MinecraftServer server = level.getServer();
        if (server == null)
            return false;

        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTag();
            nbt.remove("Luminosity");
            nbt.remove("Size");
            nbt.remove("Height");
            nbt.remove("Controller");
            nbt.remove("LastKnownPos");
            nbt.remove("RedstoneDisabled");
            nbt.remove("EngineCount");

            if (nbt.contains("TankContent")) {
                FluidStack fluid = FluidStack.parseOptional(server.registryAccess(), nbt.getCompound("TankContent"));
                if (!fluid.isEmpty()) {
                    fluid.setAmount(Math.min(FluidTankBlockEntity.getCapacityMultiplier(), fluid.getAmount()));
                    nbt.put("TankContent", fluid.saveOptional(server.registryAccess()));
                }
            }

            BlockEntity.addEntityType(nbt, ((IBE<?>) getBlock()).getBlockEntityType());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt));
        }

        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    private void tryMultiPlace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown())
            return;
        Direction face = context.getClickedFace();
        if (!face.getAxis().isVertical())
            return;

        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = level.getBlockState(placedOnPos);

        if (!FluidTankBlock.isTank(placedOnState))
            return;
        if (SymmetryWandItem.presentInHotbar(player))
            return;

        FluidTankBlockEntity tankAt = ConnectivityHandler.partAt(FuelTankBlockEntities.FUEL_TANK.get(), level, placedOnPos);
        if (tankAt == null)
            return;
        FluidTankBlockEntity controllerBE = tankAt.getControllerBE();
        if (controllerBE == null)
            return;

        int width = controllerBE.getWidth();
        if (width == 1)
            return;

        int tanksToPlace = 0;
        BlockPos startPos = face == Direction.DOWN
                ? controllerBE.getBlockPos().below()
                : controllerBE.getBlockPos().above(controllerBE.getHeight());

        if (startPos.getY() != pos.getY())
            return;

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (FluidTankBlock.isTank(blockState))
                    continue;
                if (!blockState.canBeReplaced())
                    return;
                tanksToPlace++;
            }
        }

        if (!player.isCreative() && stack.getCount() < tanksToPlace)
            return;

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (FluidTankBlock.isTank(blockState))
                    continue;
                BlockPlaceContext offsetContext = BlockPlaceContext.at(context, offsetPos, face);
                player.getPersistentData().putBoolean("SilenceTankSound", true);
                super.place(offsetContext);
                player.getPersistentData().remove("SilenceTankSound");
            }
        }
    }
}
