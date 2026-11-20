package io.github.mrbuggi.waterfallmist;

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
    private static final int SEARCH_RADIUS_XZ = 24;
    private static final int SEARCH_RADIUS_Y = 12;
    private static final int SAMPLES_PER_TICK = 28;
    private static final int MIN_FALL_HEIGHT = 3;
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

        scanForWaterfalls(world, playerPos);
        emitParticlesAndSound(world, playerPos);
    }

    private void scanForWaterfalls(ClientWorld world, BlockPos center) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int i = 0; i < SAMPLES_PER_TICK; i++) {
            int dx = random.nextInt(SEARCH_RADIUS_XZ * 2 + 1) - SEARCH_RADIUS_XZ;
            int dy = random.nextInt(SEARCH_RADIUS_Y * 2 + 1) - SEARCH_RADIUS_Y;
            int dz = random.nextInt(SEARCH_RADIUS_XZ * 2 + 1) - SEARCH_RADIUS_XZ;

            mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

            FluidState currentFluid = world.getFluidState(mutable);
            if (!currentFluid.isIn(FluidTags.WATER)) {
                continue;
            }

            mutable.move(Direction.UP);
            FluidState aboveFluid = world.getFluidState(mutable);
            if (aboveFluid.isEmpty() || !aboveFluid.isIn(FluidTags.WATER) || aboveFluid.isStill()) {
                continue;
            }

            int height = 1;
            while (height < 24) {
                mutable.move(Direction.UP);
                FluidState stepFluid = world.getFluidState(mutable);
                if (stepFluid.isEmpty() || !stepFluid.isIn(FluidTags.WATER)) {
                    break;
                }
                height++;
            }

            if (height >= MIN_FALL_HEIGHT) {
                BlockPos impactPos = new BlockPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
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

    private void emitParticlesAndSound(ClientWorld world, BlockPos playerPos) {
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
            if (distSq > (SEARCH_RADIUS_XZ + 8) * (SEARCH_RADIUS_XZ + 8) || data.lifetime <= 0) {
                if (!isStillWaterfall(world, pos)) {
                    it.remove();
                    continue;
                }
                data.lifetime = MAX_TRACKED_LIFETIME;
            }

            spawnWaterfallParticles(world, pos, data.height);

            if (distSq < 144.0 && (tickCounter % 25 == 0) && random.nextFloat() < 0.45F) {
                float pitch = 0.35F + random.nextFloat() * 0.15F;
                float volume = 0.12F + Math.min(data.height * 0.015F, 0.18F);
                world.playSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_WATER_AMBIENT,
                    SoundCategory.AMBIENT,
                    volume, pitch, false
                );
            }
        }
    }

    private boolean isStillWaterfall(ClientWorld world, BlockPos pos) {
        if (!world.getFluidState(pos).isIn(FluidTags.WATER)) {
            return false;
        }
        FluidState above = world.getFluidState(pos.up());
        return !above.isEmpty() && above.isIn(FluidTags.WATER) && !above.isStill();
    }

    private void spawnWaterfallParticles(ClientWorld world, BlockPos pos, int height) {
        double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
        double py = pos.getY() + 0.85 + random.nextDouble() * 0.15;
        double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;

        int splashCount = 1 + (height >= 6 ? 2 : 1);
        for (int i = 0; i < splashCount; i++) {
            double sx = (random.nextDouble() - 0.5) * 0.7;
            double sz = (random.nextDouble() - 0.5) * 0.7;
            double vx = (random.nextDouble() - 0.5) * 0.18;
            double vy = 0.12 + random.nextDouble() * 0.14;
            double vz = (random.nextDouble() - 0.5) * 0.18;
            world.addParticle(ParticleTypes.SPLASH, pos.getX() + 0.5 + sx, py, pos.getZ() + 0.5 + sz, vx, vy, vz);
        }

        if (random.nextFloat() < 0.65F) {
            double bx = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.9;
            double bz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.9;
            world.addParticle(ParticleTypes.BUBBLE_POP, bx, py, bz, 0.0, 0.02, 0.0);
        }

        if (random.nextFloat() < (height >= 8 ? 0.45F : 0.25F)) {
            double mx = (random.nextDouble() - 0.5) * 0.05;
            double my = 0.03 + random.nextDouble() * 0.04;
            double mz = (random.nextDouble() - 0.5) * 0.05;
            world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py + 0.1, pz, mx, my, mz);
        }

        if (random.nextFloat() < 0.30F) {
            double cx = (random.nextDouble() - 0.5) * 0.03;
            double cy = 0.01 + random.nextDouble() * 0.02;
            double cz = (random.nextDouble() - 0.5) * 0.03;
            world.addParticle(ParticleTypes.CLOUD, px, py + 0.05, pz, cx, cy, cz);
        }
    }
}
