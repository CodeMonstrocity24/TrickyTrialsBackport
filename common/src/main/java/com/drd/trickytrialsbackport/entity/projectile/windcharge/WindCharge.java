package com.drd.trickytrialsbackport.entity.projectile.windcharge;

import com.drd.trickytrialsbackport.registry.ModEntities;
import com.drd.trickytrialsbackport.registry.ModParticles;
import com.drd.trickytrialsbackport.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;

public class WindCharge extends ThrowableProjectile {
    private static final float EXPLOSION_RADIUS = 1.2F;
    private static final int NODEFLECT_TICKS = 5;

    private int noDeflectTicks = NODEFLECT_TICKS;
    private int noFallTicks = 0;
    private boolean appliedRecoil = false;
    private boolean exploded = false; // NEW

    public WindCharge(EntityType<? extends WindCharge> type, Level level) {
        super(type, level);
    }

    public WindCharge(Level level, LivingEntity owner) {
        super(ModEntities.WIND_CHARGE.get(), owner, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (noDeflectTicks > 0) {
            noDeflectTicks--;
        }

        if (noFallTicks > 0 && getOwner() instanceof LivingEntity living) {
            noFallTicks--;
            living.fallDistance = 0.0F;
        }

        if (exploded && getOwner() instanceof LivingEntity living) {
            living.fallDistance = 0.0F;

            if (living.onGround() || living.isInWater() || living.isInLava()) {
                discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode();
    }

    private void explode() {
        if (!level().isClientSide) {
            ServerLevel server = (ServerLevel) level();

            server.sendParticles(ModParticles.GUST_EMITTER_SMALL.get(),
                    getX(), getY(), getZ(),
                    1, 0, 0, 0, 0);

            if (random.nextFloat() < 0.25F) {
                server.sendParticles(ModParticles.GUST_EMITTER_LARGE.get(),
                        getX(), getY(), getZ(),
                        1, 0, 0, 0, 0);
            }

            server.playSound(
                    null,
                    getX(), getY(), getZ(),
                    ModSounds.WIND_CHARGE_BURST.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            
            WindChargeBlockInteraction.burst(server, position(), 2.4D, getOwner(), false);

            exploded = true;
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
        }
    }

    @Override
    protected float getGravity() {
        return 0.01F;
    }
}
