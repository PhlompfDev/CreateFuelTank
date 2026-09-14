package com.createvehiclesurplus.content.link;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-click a built-in link slot with an item to set that frequency (empty hand clears it), like on a Redstone Link. */
public final class SidedLinkInteractionHandler {
    private SidedLinkInteractionHandler() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled())
            return;
        Player player = event.getEntity();
        if (player.isShiftKeyDown() || player.isSpectator())
            return;
        ItemStack held = player.getItemInHand(event.getHand());
        if (AllItems.WRENCH.isIn(held) || AllItems.LINKED_CONTROLLER.isIn(held))
            return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Vec3 hit = event.getHitVec().getLocation();
        for (BehaviourType<SidedLinkBehaviour> type : SidedLinkBehaviour.types()) {
            SidedLinkBehaviour link = BlockEntityBehaviour.get(level, pos, type);
            if (link == null)
                continue;
            for (boolean first : Iterate.trueAndFalse) {
                if (!link.testHit(first, hit))
                    continue;
                if (!level.isClientSide)
                    link.setFrequency(first, held);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25f, 0.1f);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
    }
}
