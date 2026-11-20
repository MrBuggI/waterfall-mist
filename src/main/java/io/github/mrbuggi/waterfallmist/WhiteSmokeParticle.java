package io.github.mrbuggi.waterfallmist;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class WhiteSmokeParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    protected WhiteSmokeParticle(ClientWorld world, double x, double y, double z,
                                double vx, double vy, double vz, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.spriteProvider = spriteProvider;
        this.setSpriteForAge(spriteProvider);

        this.scale(1.4F + this.random.nextFloat() * 1.0F);
        this.maxAge = 25 + this.random.nextInt(20);
        this.gravityStrength = 0.00005F;

        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;

        // Pure white mist with slight soft blue/cyan tint
        this.setColor(0.95F, 0.98F, 1.0F);
        this.setAlpha(0.60F);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.spriteProvider);

        float lifeRatio = (float) this.age / (float) this.maxAge;
        this.setAlpha(Math.max(0.0F, (1.0F - lifeRatio) * 0.60F));
        this.scale(this.scale * 1.018F);
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new WhiteSmokeParticle(world, x, y, z, vx, vy, vz, this.spriteProvider);
        }
    }
}
