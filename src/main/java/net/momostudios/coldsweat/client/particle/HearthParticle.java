package net.momostudios.coldsweat.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class HearthParticle extends SpriteTexturedParticle
{
    private IAnimatedSprite ageSprite;
    protected HearthParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, IAnimatedSprite spriteSet)
    {
        super(world, x, y, z);
        float size = 0.5f;
        this.ageSprite = spriteSet;

        this.particleAlpha = 0.0f;
        this.setSize(size, size);
        this.particleScale = 0.4f + (float) (Math.random() / 2.5f);
        this.maxAge = 40;
        this.particleGravity = -0.01f;
        this.canCollide = true;
        this.motionX = vx * 1;
        this.motionY = vy * 1;
        this.motionZ = vz * 1;
        this.selectSpriteWithAge(spriteSet);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick()
    {
        super.tick();
        this.selectSpriteWithAge(this.ageSprite);

        if (this.age < 10)
            this.particleAlpha += 0.05f;
        else if (this.age > 35)
            this.particleAlpha -= 0.06f;

        if (this.particleAlpha <= 0.06  && this.age > 10)
            this.setExpired();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<BasicParticleType>
    {
        public final IAnimatedSprite sprite;

        public Factory(IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new HearthParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite);
        }
    }
}