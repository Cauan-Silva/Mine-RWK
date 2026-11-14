package com.minerwk.client.arena;

import com.minerwk.arena.ArenasServidor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;

/**
 * O céu da dimensão das arenas: o mesmo do mundo normal (o pôr do sol da Escola), menos as nuvens em Oriana e no
 * mundo interior, que ficariam com a cor do pôr do sol por cima dos céus próprios ({@link NoiteOriana},
 * {@link CeuInterior}), e com a chuva da máscara desenhada pelo {@link CeuInterior}.
 */
public final class CeuArena extends DimensionSpecialEffects.OverworldEffects {
    public static void registrar(RegisterDimensionSpecialEffectsEvent evento) {
        evento.register(ArenasServidor.DIMENSAO.location(), new CeuArena());
    }

    /** Verdadeiro quer dizer "já cuidei": em Oriana, nenhuma nuvem. */
    @Override
    public boolean renderClouds(ClientLevel nivel, int ticks, float parcial, PoseStack pose, double camX, double camY, double camZ,
                                Matrix4f modelo, Matrix4f projecao) {
        return NoiteOriana.ativa() || CeuInterior.ativa();
    }

    /** No mundo interior, a chuva da máscara é desenhada aqui; nos outros cenários, a do jogo (que no vazio não cai). */
    @Override
    public boolean renderSnowAndRain(ClientLevel nivel, int ticks, float parcial, LightTexture luz, double camX, double camY, double camZ) {
        if (!CeuInterior.ativa()) return false;
        CeuInterior.desenharChuva(nivel, ticks, parcial, camX, camY, camZ);
        return true;
    }
}
