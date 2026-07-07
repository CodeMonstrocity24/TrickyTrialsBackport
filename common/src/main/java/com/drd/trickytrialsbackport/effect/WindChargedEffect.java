package com.drd.trickytrialsbackport.effect;

import com.drd.trickytrialsbackport.registry.ModParticles;
import com.drd.trickytrialsbackport.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class WindChargedEffect extends MobEffect {
    public WindChargedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public static void explodeWindCharge(LivingEntity entity) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() / 2.0F;
        double z = entity.getZ();

        double radius = 3.5;
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);

        for (Entity target : level.getEntities(entity, box)) {
            if (target instanceof LivingEntity living) {
                double dx = living.getX() - x;
                double dz = living.getZ() - z;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > radius) continue;

                double falloff = 1.0 - dist / radius;
                double strength = falloff * 1.2;
                double upward = 0.4 * falloff + 0.15;

                if (dist < 0.001) {
                    // Standing on/at the burst center: launch straight up.
                    living.push(0.0, Math.max(upward, 0.5), 0.0);
                } else {
                    living.push(dx / dist * strength, upward, dz / dist * strength);
                }
                living.hurtMarked = true;
            }
        }

        serverLevel.sendParticles(ModParticles.GUST_EMITTER_SMALL.get(), x, y, z, 20, 0.5, 0.5, 0.5, 0.1);
        serverLevel.sendParticles(ModParticles.GUST_EMITTER_LARGE.get(), x, y, z, 5, 0.5, 0.5, 0.5, 0.1);
        serverLevel.playSound(null, x, y, z, ModSounds.BREEZE_WIND_CHARGE_BURST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}