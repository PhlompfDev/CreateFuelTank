package com.createvehiclesurplus.client;

import com.createvehiclesurplus.content.link.SidedLinkBehaviour;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Client tick: outlines the built-in link slots on the face the player is looking at, and shows
 * the "click to set" tip over the hovered one, the way Create does for a Redstone Link.
 */
public final class SidedLinkOutliner {
    private SidedLinkOutliner() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit))
            return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        Vec3 faceNormal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Component firstName = CreateLang.translateDirect("logistics.firstFrequency");
        Component secondName = CreateLang.translateDirect("logistics.secondFrequency");

        for (BehaviourType<SidedLinkBehaviour> type : SidedLinkBehaviour.types()) {
            SidedLinkBehaviour link = BlockEntityBehaviour.get(mc.level, pos, type);
            if (link == null)
                continue;
            for (boolean first : Iterate.trueAndFalse) {
                Vec3 offset = link.getSlot(first).getLocalOffset(mc.level, pos, state);
                if (offset == null || offset.subtract(0.5, 0.5, 0.5).dot(faceNormal) < 0.4)
                    continue; // not on the face being looked at
                Component slotName = first ? firstName : secondName;
                boolean hovered = link.testHit(first, hit.getLocation());
                boolean empty = link.getFrequency(first).getStack().isEmpty();
                ValueBox box = new ValueBox(slotName, new AABB(Vec3.ZERO, Vec3.ZERO).inflate(0.25), pos).passive(!hovered);
                if (!empty)
                    box.wideOutline();
                Outliner.getInstance()
                        .showOutline(Pair.of(type.getName() + first, pos), box.transform(link.getSlot(first)))
                        .highlightFace(hit.getDirection());
                if (hovered) {
                    List<MutableComponent> tip = new ArrayList<>();
                    tip.add(link.label().copy().append(": ").append(slotName));
                    tip.add(CreateLang.translateDirect(empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
                    CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
                }
            }
        }
    }
}
