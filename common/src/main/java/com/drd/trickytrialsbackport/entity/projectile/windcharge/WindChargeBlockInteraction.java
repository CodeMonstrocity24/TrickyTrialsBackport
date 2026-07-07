package com.drd.trickytrialsbackport.entity.projectile.windcharge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Vanilla-accurate wind charge block interactions: flips non-iron doors,
 * trapdoors, and fence gates; toggles levers; presses buttons; rings bells;
 * extinguishes candles; breaks decorated pots and chorus flowers.
 *
 * Bells always ring regardless of mobGriefing. All other interactions are
 * suppressed when caused by a breeze and mobGriefing is disabled; player- and
 * dispenser-fired charges always interact.
 */
public final class WindChargeBlockInteraction {
    private WindChargeBlockInteraction() {}

    public static void apply(ServerLevel level, BlockPos pos, Entity source, boolean fromBreeze) {
        BlockState state = level.getBlockState(pos);

        boolean griefingAllowed = !fromBreeze
                || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);

        // Iron doors/trapdoors never react.
        if (state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_TRAPDOOR)) {
            return;
        }

        // Bells ring regardless of mobGriefing.
        if (state.getBlock() instanceof BellBlock bell) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BellBlockEntity) {
                bell.attemptToRing(source, level, pos, (Direction) null);
                level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
            }
            return;
        }

        if (!griefingAllowed) {
            return;
        }

        if (state.getBlock() instanceof DoorBlock) {
            boolean open = state.getValue(BlockStateProperties.OPEN);
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, !open), 10);
            level.levelEvent(null, open ? 1006 : 1005, pos, 0);
            level.gameEvent(source, open ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
        } else if (state.getBlock() instanceof TrapDoorBlock) {
            boolean open = state.getValue(BlockStateProperties.OPEN);
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, !open), 10);
            level.levelEvent(null, open ? 1007 : 1037, pos, 0);
            level.gameEvent(source, open ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
        } else if (state.getBlock() instanceof FenceGateBlock) {
            boolean open = state.getValue(BlockStateProperties.OPEN);
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, !open), 10);
            level.levelEvent(null, open ? 1014 : 1008, pos, 0);
            level.gameEvent(source, open ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
        } else if (state.getBlock() instanceof LeverBlock) {
            BlockState newState = state.cycle(BlockStateProperties.POWERED);
            level.setBlock(pos, newState, 3);
            updateLeverNeighbors(level, pos, newState);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F,
                    newState.getValue(BlockStateProperties.POWERED) ? 0.6F : 0.5F);
            level.gameEvent(source, GameEvent.BLOCK_ACTIVATE, pos);
        } else if (state.getBlock() instanceof ButtonBlock button) {
            if (!state.getValue(BlockStateProperties.POWERED)) {
                button.press(state, level, pos);
                level.gameEvent(source, GameEvent.BLOCK_ACTIVATE, pos);
            }
        } else if (state.getBlock() instanceof CandleBlock && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
        } else if (state.getBlock() instanceof CandleCakeBlock && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
        } else if (state.getBlock() instanceof ChorusFlowerBlock || state.getBlock() instanceof net.minecraft.world.level.block.DecoratedPotBlock) {
            level.destroyBlock(pos, true, source);
        }
    }

    /**
     * Full wind-charge burst: knock back nearby entities and interact with nearby
     * interactable blocks within the given radius. Entities are pushed away from
     * center (including the owner if they're in range).
     */
    public static void burst(ServerLevel level, Vec3 center, double radius, Entity source, boolean fromBreeze) {
        // Entity knockback
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (net.minecraft.world.entity.LivingEntity e :
                level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
            double dist = e.position().distanceTo(center);
            if (dist > radius) continue;
            double strength = (1.0 - dist / radius) * 1.2;
            Vec3 push = e.position().subtract(center);
            if (push.lengthSqr() < 1.0E-4) {
                push = new Vec3(0.0, 1.0, 0.0);
            }
            push = push.normalize().scale(strength);
            e.push(push.x, Math.max(push.y, 0.35), push.z);
            e.hurtMarked = true;
        }

        // Block interactions within the burst radius
        int r = (int) Math.ceil(radius);
        BlockPos centerPos = BlockPos.containing(center.x, center.y, center.z);
        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset(-r, -r, -r), centerPos.offset(r, r, r))) {
            if (pos.getCenter().distanceTo(center) <= radius) {
                apply(level, pos.immutable(), source, fromBreeze);
            }
        }
    }

    private static void updateLeverNeighbors(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.FACING).getOpposite();
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.relative(facing), state.getBlock());
    }
}