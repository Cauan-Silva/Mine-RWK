package com.minerwk.client.arena;

import com.minerwk.MineRWK;
import com.minerwk.core.arena.CurvaEscurecer;
import com.minerwk.rede.EscurecerTela;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A tela preta que esconde a ida e a volta da arena. Fica por cima de tudo, inclusive do HUD. Não segue a
 * opção {@code cutscenes} do cliente: sem ela o jogador veria o teleporte.
 */
public final class Escurecer {
    public static final ResourceLocation CAMADA = MineRWK.id("escurecer");

    private static EscurecerTela pedido;
    private static int tick;

    private Escurecer() {
    }

    public static void receber(EscurecerTela p) {
        // Já preta e o pedido novo começa preto: continua sem piscar.
        pedido = p;
        tick = 0;
    }

    public static void limpar() {
        pedido = null;
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        if (pedido == null || Minecraft.getInstance().isPaused()) return;
        if (++tick > CurvaEscurecer.duracao(pedido.escurecendo(), pedido.preto(), pedido.clareando())) pedido = null;
    }

    public static void desenhar(GuiGraphics g, DeltaTracker tempo) {
        if (pedido == null) return;
        double opacidade = CurvaEscurecer.opacidade(tick + tempo.getGameTimeDeltaPartialTick(true),
                pedido.escurecendo(), pedido.preto(), pedido.clareando());
        int alfa = (int) Math.round(255 * opacidade);
        if (alfa <= 0) return;
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), alfa << 24);
    }
}
