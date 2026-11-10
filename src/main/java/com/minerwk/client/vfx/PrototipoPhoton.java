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
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Dot;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.MeshData;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleRendererSetting;
import com.minerwk.MineRWK;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Parte 0 do spec do VFX pesado: protótipo descartável para provar três coisas no jogo
 * (efeito do Photon montado em código, bloom por HDR, shader lendo profundidade e cor da cena).
 * Sai do código depois do teste.
 */
public final class PrototipoPhoton {
    private PrototipoPhoton() {
    }

    private static final BlendMode ADITIVO = new BlendMode(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, BlendMode.BlendFuc.ADD);
    private static final BlendMode TRANSLUCIDO = new BlendMode(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, BlendMode.BlendFuc.ADD);
    private static final ResourceLocation CIRCULO = ResourceLocation.parse("photon:textures/particle/circle.png");

    /** 1: rajada de 150 partículas roxas com rastro e cor HDR (deve brilhar). */
    public static void fogos(Level nivel, BlockPos pos) {
        ParticleConfig c = new ParticleConfig();
        c.setDuration(60);
        c.setLooping(false);
        c.setStartLifetime(new Constant(30));
        c.setStartSpeed(new Constant(8f));
        c.setStartSize(new NumberFunction3(0.15f, 0.15f, 0.15f));
        c.setStartColor(new Constant(0xFF9A4DFF));
        c.emission.setEmissionRate(new Constant(0));
        EmissionSetting.Burst rajada = new EmissionSetting.Burst();
        rajada.time = 0;
        rajada.cycles = 1;
        rajada.probability = 1;
        rajada.setCount(new Constant(150));
        c.emission.setBursts(new ArrayList<>(List.of(rajada)));
        Sphere esfera = new Sphere();
        esfera.setRadius(0.3f);
        c.shape.setShape(esfera);
        c.physics.setEnable(true);
        c.physics.setGravity(new Constant(0.02f));
        c.trails.setEnable(true);
        c.trails.setLifetime(new Constant(0.4f));
        c.trails.setInheritParticleColor(true);
        c.renderer.setMaterials(new ArrayList<>(List.of(material(texturaHdr(new HDRColor(1.4f, 0.5f, 2.6f, 1f, 1f)), ADITIVO))));
        tocar(c, nivel, pos.above(3));
    }

    /** 2: um halo grande HDR parado no ar, como o que ficaria atrás do nosso núcleo (deve brilhar em volta). */
    public static void halo(Level nivel, BlockPos pos) {
        ParticleConfig c = umaParticula(100, 4f, 0xFFB070FF);
        c.renderer.setMaterials(new ArrayList<>(List.of(material(texturaHdr(new HDRColor(1.2f, 0.4f, 2.4f, 1f, 1f)), ADITIVO))));
        tocar(c, nivel, pos.above(3));
    }

    /** 3: casca esférica com o shader de teste: fresnel, linha onde corta o terreno e distorção atrás. */
    public static void casca(Level nivel, BlockPos pos) {
        ParticleConfig c = umaParticula(200, 6f, 0xFFFFFFFF);
        c.renderer.setRenderMode(ParticleRendererSetting.Mode.Model);
        c.renderer.setUseBlockUV(false);
        c.renderer.setModel(new MeshData(new ObjModelSource(ResourceLocation.parse("photon:models/sphere.obj"))));
        c.renderer.setModelPivot(new Vector3f(0.5f, 0.5f, 0.5f));
        c.renderer.setMaterials(new ArrayList<>(List.of(material(new CustomShaderMaterial(MineRWK.id("casca_teste")), TRANSLUCIDO))));
        tocar(c, nivel, pos);
    }

    private static ParticleConfig umaParticula(int vida, float tamanho, int cor) {
        ParticleConfig c = new ParticleConfig();
        c.setDuration(vida);
        c.setLooping(false);
        c.setStartLifetime(new Constant(vida));
        c.setStartSpeed(new Constant(0));
        c.setStartSize(new NumberFunction3(tamanho, tamanho, tamanho));
        c.setStartColor(new Constant(cor));
        c.emission.setEmissionRate(new Constant(0));
        EmissionSetting.Burst uma = new EmissionSetting.Burst();
        uma.time = 0;
        uma.cycles = 1;
        uma.probability = 1;
        uma.setCount(new Constant(1));
        c.emission.setBursts(new ArrayList<>(List.of(uma)));
        c.shape.setShape(new Dot());
        return c;
    }

    private static TextureMaterial texturaHdr(HDRColor hdr) {
        TextureMaterial m = new TextureMaterial(CIRCULO);
        m.setHdr(hdr);
        m.setHdrMode(TextureMaterial.HDRMode.ADDITIVE);
        return m;
    }

    private static MaterialSetting material(IMaterial m, BlendMode blend) {
        MaterialSetting s = new MaterialSetting(m);
        s.setBlend(blend);
        return s;
    }

    private static void tocar(ParticleConfig c, Level nivel, BlockPos pos) {
        FX fx = new FX();
        fx.getFxData().objects().add(new ParticleEmitter(c));
        new BlockEffectExecutor(fx, nivel, pos).start();
    }
}
