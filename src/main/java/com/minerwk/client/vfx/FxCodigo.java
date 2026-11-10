package com.minerwk.client.vfx;

import com.lowdragmc.lowdraglib2.math.HDRColor;
import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.BlendMode;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.CustomShaderMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.IMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.model.ObjModelSource;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.RandomConstant;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Circle;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Dot;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.IShape;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.MeshData;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleRendererSetting;
import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta efeitos do Photon2 em código (sem o editor e sem arquivo .fx) e toca no mundo. Só no cliente.
 * Velocidades em blocos por segundo e tempos em ticks, como no editor do Photon.
 */
public final class FxCodigo {
    public static final BlendMode ADITIVO = new BlendMode(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE, BlendMode.BlendFuc.ADD);
    public static final BlendMode TRANSLUCIDO = new BlendMode(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, BlendMode.BlendFuc.ADD);
    public static final ResourceLocation CIRCULO = ResourceLocation.parse("photon:textures/particle/circle.png");
    public static final ResourceLocation FUMACA = ResourceLocation.parse("photon:textures/particle/smoke.png");
    public static final ResourceLocation ESFERA = ResourceLocation.parse("photon:models/sphere.obj");

    private final ParticleConfig c = new ParticleConfig();

    private FxCodigo(int duracao) {
        c.setDuration(duracao);
        c.setLooping(false);
        c.emission.setEmissionRate(new Constant(0));
        c.shape.setShape(new Dot());
    }

    /** Um emissor que dura {@code duracao} ticks. */
    public static FxCodigo emissor(int duracao) {
        return new FxCodigo(duracao);
    }

    /** Menos partículas no VFX reduzido. */
    private static int qtd(int n) {
        return ConfigCliente.vfxReduzido() ? Math.max(1, n / 2) : n;
    }

    public FxCodigo vida(int min, int max) {
        c.setStartLifetime(min == max ? new Constant(min) : new RandomConstant(min, max));
        return this;
    }

    public FxCodigo velocidade(float min, float max) {
        c.setStartSpeed(min == max ? new Constant(min) : new RandomConstant(min, max));
        return this;
    }

    public FxCodigo tamanho(float t) {
        c.setStartSize(new NumberFunction3(t, t, t));
        return this;
    }

    public FxCodigo cor(int argb) {
        c.setStartColor(new Constant(argb));
        return this;
    }

    /** Uma rajada de {@code n} partículas no tick {@code tick}. */
    public FxCodigo rajada(int tick, int n) {
        EmissionSetting.Burst b = new EmissionSetting.Burst();
        b.time = tick;
        b.cycles = 1;
        b.probability = 1;
        b.setCount(new Constant(qtd(n)));
        List<EmissionSetting.Burst> lista = new ArrayList<>(c.emission.getBursts());
        lista.add(b);
        c.emission.setBursts(lista);
        return this;
    }

    /** Emissão contínua (partículas por segundo). */
    public FxCodigo taxa(float porSegundo) {
        c.emission.setEmissionRate(new Constant(ConfigCliente.vfxReduzido() ? porSegundo / 2 : porSegundo));
        return this;
    }

    public FxCodigo esfera(float raio, float espessura) {
        Sphere s = new Sphere();
        s.setRadius(raio);
        s.setRadiusThickness(espessura);
        return forma(s);
    }

    /** Círculo deitado no chão (plano horizontal). */
    public FxCodigo circulo(float raio, float espessura) {
        Circle s = new Circle();
        s.setRadius(raio);
        s.setRadiusThickness(espessura);
        c.shape.setRotation(new NumberFunction3(90, 0, 0));
        return forma(s);
    }

    private FxCodigo forma(IShape s) {
        c.shape.setShape(s);
        return this;
    }

    public FxCodigo gravidade(float g, boolean colide) {
        c.physics.setEnable(true);
        c.physics.setGravity(new Constant(g));
        c.physics.setHasCollision(colide);
        return this;
    }

    /** Rastro atrás de cada partícula, com a cor dela; {@code duracao} relativa à vida da partícula. */
    public FxCodigo rastro(float duracao) {
        c.trails.setEnable(true);
        c.trails.setLifetime(new Constant(duracao));
        c.trails.setInheritParticleColor(true);
        return this;
    }

    /** Textura com cor HDR somada (componentes acima de 1 brilham com o bloom do Photon). */
    public FxCodigo textura(ResourceLocation tex, float r, float g, float b, BlendMode blend) {
        TextureMaterial m = new TextureMaterial(tex);
        m.setHdr(new HDRColor(r, g, b, 1f, 1f));
        m.setHdrMode(TextureMaterial.HDRMode.ADDITIVE);
        return material(m, blend);
    }

    /** Malha esférica com um shader nosso ({@code assets/minerwk/shaders/core/<shader>.json}). */
    public FxCodigo esferaComShader(String shader) {
        c.renderer.setRenderMode(ParticleRendererSetting.Mode.Model);
        c.renderer.setUseBlockUV(false);
        c.renderer.setModel(new MeshData(new ObjModelSource(ESFERA)));
        c.renderer.setModelPivot(new Vector3f(.5f, .5f, .5f));
        return material(new CustomShaderMaterial(MineRWK.id(shader)), TRANSLUCIDO);
    }

    private FxCodigo material(IMaterial m, BlendMode blend) {
        MaterialSetting s = new MaterialSetting(m);
        s.setBlend(blend);
        c.renderer.setMaterials(new ArrayList<>(List.of(s)));
        return this;
    }

    /** Toca o efeito parado em {@code onde} (o centro do bloco mais o deslocamento até o ponto exato). */
    public void tocar(Level nivel, Vec3 onde) {
        FX fx = new FX();
        fx.getFxData().objects().add(new ParticleEmitter(c));
        BlockPos bloco = BlockPos.containing(onde);
        BlockEffectExecutor e = new BlockEffectExecutor(fx, nivel, bloco);
        e.setOffset(new Vector3f((float) (onde.x - bloco.getX() - .5), (float) (onde.y - bloco.getY() - .5),
                (float) (onde.z - bloco.getZ() - .5)));
        e.setAllowMulti(true);
        e.start();
    }
}
