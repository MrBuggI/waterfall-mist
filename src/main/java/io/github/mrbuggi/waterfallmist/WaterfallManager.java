package io.github.mrbuggi.waterfallmist;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public final class WaterfallManager {
    private static final int SEARCH_RADIUS_Y = 14;
    private static final int SAMPLES_PER_TICK = 36;
    private static final int MAX_TRACKED_LIFETIME = 50;

    private final Map<BlockPos, WaterfallData> activeWaterfalls = new HashMap<>();
    private final Random random = new Random();
    private int tickCounter = 0;

    private static class WaterfallData {
        int height;
        int lifetime;

        WaterfallData(int height, int lifetime) {
            this.height = height;
            this.lifetime = lifetime;
        }
    }

    public void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            activeWaterfalls.clear();
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        tickCounter++;

        WaterfallConfig config = WaterfallConfig.get();
        scanForWaterfalls(world, playerPos, config);
        emitParticlesAndSound(world, playerPos, config);
    }

    private void scanForWaterfalls(ClientWorld world, BlockPos center, WaterfallConfig config) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int radiusXZ = config.searchRadius;

        for (int i = 0; i < SAMPLES_PER_TICK; i++) {
            int dx = random.nextInt(radiusXZ * 2 + 1) - radiusXZ;
            int dy = random.nextInt(SEARCH_RADIUS_Y * 2 + 1) - SEARCH_RADIUS_Y;
            int dz = random.nextInt(radiusXZ * 2 + 1) - radiusXZ;

            mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

            if (!isWaterImpactBase(world, mutable)) {
                continue;
            }

            BlockPos.Mutable tracer = mutable.mutableCopy();
            int height = 0;
            while (height < 24) {
                tracer.move(Direction.UP);
                FluidState stepFluid = world.getFluidState(tracer);
                if (stepFluid.isEmpty() || !stepFluid.isIn(FluidTags.WATER)) {
                    break;
                }
                height++;
            }

            if (height >= config.minFallHeight) {
                BlockPos impactPos = mutable.toImmutable();
                WaterfallData existing = activeWaterfalls.get(impactPos);
                if (existing != null) {
                    existing.lifetime = MAX_TRACKED_LIFETIME;
                    existing.height = Math.max(existing.height, height);
                } else {
                    activeWaterfalls.put(impactPos, new WaterfallData(height, MAX_TRACKED_LIFETIME));
                }
            }
        }
    }

    private boolean isWaterImpactBase(ClientWorld world, BlockPos pos) {
        FluidState currentFluid = world.getFluidState(pos);
        if (!currentFluid.isIn(FluidTags.WATER)) {
            return false;
        }

        FluidState aboveFluid = world.getFluidState(pos.up());
        if (aboveFluid.isEmpty() || !aboveFluid.isIn(FluidTags.WATER) || aboveFluid.isStill()) {
            return false;
        }

        BlockPos downPos = pos.down();
        BlockState belowState = world.getBlockState(downPos);

        if (!belowState.getCollisionShape(world, downPos).isEmpty() || belowState.isSolid()) {
            return true;
        }

        if (belowState.isAir()) {
            return false;
        }

        FluidState belowFluid = world.getFluidState(downPos);
        if (belowFluid.isIn(FluidTags.WATER)) {
            if (belowFluid.isStill() || currentFluid.isStill()) {
                return true;
            }
            BlockState floorState = world.getBlockState(downPos.down());
            if (!floorState.getCollisionShape(world, downPos.down()).isEmpty() || floorState.isSolid()) {
                return true;
            }
        }

        return false;
    }

    private void emitParticlesAndSound(ClientWorld world, BlockPos playerPos, WaterfallConfig config) {
        if (activeWaterfalls.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<BlockPos, WaterfallData>> it = activeWaterfalls.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, WaterfallData> entry = it.next();
            BlockPos pos = entry.getKey();
            WaterfallData data = entry.getValue();

            data.lifetime--;

            double distSq = pos.getSquaredDistance(playerPos);
            int maxDist = config.searchRadius + 8;
            if (distSq > (double) maxDist * maxDist || data.lifetime <= 0) {
                if (!isWaterImpactBase(world, pos)) {
                    it.remove();
                    continue;
                }
                data.lifetime = MAX_TRACKED_LIFETIME;
            }

            spawnWaterfallParticles(world, pos, data.height, config);

            if (config.enableSounds && distSq < 144.0 && (tickCounter % 25 == 0) && random.nextFloat() < 0.45F) {
                float pitch = 0.35F + random.nextFloat() * 0.15F;
                float volume = (0.12F + Math.min(data.height * 0.015F, 0.18F)) * config.soundVolume;
                world.playSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_WATER_AMBIENT,
                    SoundCategory.AMBIENT,
                    volume, pitch, false
                );
            }
        }
    }

    private void spawnWaterfallParticles(ClientWorld world, BlockPos pos, int height, WaterfallConfig config) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double mistDist = 0.45 + random.nextDouble() * 0.55;
        double px = pos.getX() + 0.5 + Math.cos(angle) * mistDist;
        double pz = pos.getZ() + 0.5 + Math.sin(angle) * mistDist;
        double py = pos.getY() + 0.95 + random.nextDouble() * 0.15;

        double mvx = Math.cos(angle) * (0.015 + random.nextDouble() * 0.025);
        double mvy = 0.025 + random.nextDouble() * 0.035;
        double mvz = Math.sin(angle) * (0.015 + random.nextDouble() * 0.025);

        float density = config.mistDensity;

        // 1. Pure white mist
        if (config.enableMist && random.nextFloat() < (height >= 8 ? 0.70F : 0.45F) * density) {
            world.addParticle(WaterfallMistClient.MIST, px, py, pz, mvx, mvy, mvz);
        }

        // 2. Soft white cloud puff
        if (config.enableMist && random.nextFloat() < 0.30F * density) {
            double cx = pos.getX() + 0.5 + Math.cos(angle) * (0.40 + random.nextDouble() * 0.40);
            double cz = pos.getZ() + 0.5 + Math.sin(angle) * (0.40 + random.nextDouble() * 0.40);
            world.addParticle(ParticleTypes.CLOUD, cx, py + 0.05, cz, mvx * 0.5, 0.015, mvz * 0.5);
        }

        // 3. Water splashes jumping outward from impact
        if (config.enableSplashes) {
            int splashCount = Math.max(1, Math.round((1 + (height >= 6 ? 2 : 1)) * density));
            for (int i = 0; i < splashCount; i++) {
                double sAngle = random.nextDouble() * Math.PI * 2.0;
                double sDist = 0.30 + random.nextDouble() * 0.35;
                double sx = pos.getX() + 0.5 + Math.cos(sAngle) * sDist;
                double sz = pos.getZ() + 0.5 + Math.sin(sAngle) * sDist;
                double svx = Math.cos(sAngle) * (0.04 + random.nextDouble() * 0.08);
                double svy = 0.12 + random.nextDouble() * 0.14;
                double svz = Math.sin(sAngle) * (0.04 + random.nextDouble() * 0.08);
                world.addParticle(ParticleTypes.SPLASH, sx, py, sz, svx, svy, svz);
            }
        }

        // 4. Surface foam bubbles
        if (config.enableBubbles && random.nextFloat() < 0.55F * density) {
            double fAngle = random.nextDouble() * Math.PI * 2.0;
            double fDist = 0.35 + random.nextDouble() * 0.45;
            double bx = pos.getX() + 0.5 + Math.cos(fAngle) * fDist;
            double bz = pos.getZ() + 0.5 + Math.sin(fAngle) * fDist;
            world.addParticle(ParticleTypes.BUBBLE_POP, bx, py, bz, 0.0, 0.015, 0.0);
        }
    }
}
