package com.minerwk.client.arena;

import com.minerwk.arena.ArenasServidor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;

/**
 * O céu da dimensão das arenas: o mesmo do mundo normal (o pôr do sol da Escola e do mundo interior), menos as
 * nuvens em Oriana, que ficariam brancas por cima da noite de {@link NoiteOriana}.
 */
public final class CeuArena extends DimensionSpecialEffects.OverworldEffects {
    public static void registrar(RegisterDimensionSpecialEffectsEvent evento) {
        evento.register(ArenasServidor.DIMENSAO.location(), new CeuArena());
    }

    /** Verdadeiro quer dizer "já cuidei": em Oriana, nenhuma nuvem. */
    @Override
    public boolean renderClouds(ClientLevel nivel, int ticks, float parcial, PoseStack pose, double camX, double camY, double camZ,
                                Matrix4f modelo, Matrix4f projecao) {
        return NoiteOriana.ativa();
    }
}
