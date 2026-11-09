package com.minerwk.client.kit;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.core.hud.LogicaHud;
import com.minerwk.rede.SincronizarKit;
import com.minerwk.rede.SincronizarKit.HabilidadeHud;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** HUD de recurso e recargas, acima dos corações e da fome. */
public final class HudKit {
    public static final ResourceLocation CAMADA = MineRWK.id("hud_kit");

    private static final int LADO = 16;
    private static final int ESPACO = 2;
    private static final int ALTURA_BARRA = 4;
    /** Folga acima da pilha vanilla (vida, armadura, fome, montaria, ar). */
    private static final int FOLGA = 2;

    private static final int FUNDO = 0xAA000000;
    private static final int SOMBRA_RECARGA = 0xB0000000;
    private static final int BORDA = 0xFF555555;
    private static final int BORDA_EM_USO = 0xFFFFFFFF;
    private static final int TEXTO = 0xFFFFFFFF;
    private static final int TEXTO_SEM_RECURSO = 0xFFFF5555;

    /** Valor mostrado da barra, que desliza até o recebido; -1 = pular direto para o valor. */
    private static double recursoMostrado = -1;
    private static String personagemMostrado = "";

    private HudKit() {
    }

    public static void registrar(RegisterGuiLayersEvent evento) {
        evento.registerAbove(VanillaGuiLayers.AIR_LEVEL, CAMADA, HudKit::desenhar);
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        SincronizarKit s = RecepcaoKit.estado();
        if (s.personagem().isEmpty() || !s.personagem().equals(personagemMostrado)) {
            // Saiu do mundo, entrou ou trocou de personagem: a barra não desliza a partir de um valor velho.
            personagemMostrado = s.personagem();
            recursoMostrado = -1;
            if (s.personagem().isEmpty()) return;
        }
        double alvo = s.recurso();
        if (recursoMostrado < 0 || Math.abs(alvo - recursoMostrado) < 0.05) recursoMostrado = alvo;
        else recursoMostrado += (alvo - recursoMostrado) * 0.35;
    }

    private static void desenhar(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SincronizarKit s = RecepcaoKit.estado();
        if (!ConfigCliente.mostrarHud() || mc.options.hideGui || mc.level == null || mc.player == null
                || s.personagem().isEmpty() || mc.player.isSpectator()) {
            return;
        }
        List<HabilidadeHud> habilidades = s.habilidades();
        if (habilidades.isEmpty()) return;

        Font fonte = mc.font;
        long agora = mc.level.getGameTime();
        int larguraTotal = habilidades.size() * (LADO + ESPACO) - ESPACO;
        int x0 = (g.guiWidth() - larguraTotal) / 2;
        // A pilha vanilla (vida, armadura, fome, montaria, ar) já desenhou e diz até onde subiu.
        int pilha = Math.max(mc.gui.leftHeight, mc.gui.rightHeight);
        int yBarra = g.guiHeight() - pilha - FOLGA - ALTURA_BARRA;
        int yQuadros = yBarra - 2 - LADO;

        for (int i = 0; i < habilidades.size(); i++) {
            HabilidadeHud h = habilidades.get(i);
            int x = x0 + i * (LADO + ESPACO);
            boolean emUso = h.id().equals(s.emUso());
            g.fill(x - 1, yQuadros - 1, x + LADO + 1, yQuadros + LADO + 1, emUso ? BORDA_EM_USO : BORDA);
            g.fill(x, yQuadros, x + LADO, yQuadros + LADO, FUNDO);

            long restante = h.prontaNoTick() - agora;
            // Relógio do cliente atrasado não mostra mais tempo que a recarga tem.
            if (h.duracao() > 0) restante = Math.min(restante, h.duracao());
            float fracao = LogicaHud.fracao(h.prontaNoTick(), h.duracao(), agora);
            if (restante > 0) {
                int alturaSombra = Math.round(LADO * (fracao > 0 ? fracao : 1f));
                g.fill(x, yQuadros + LADO - alturaSombra, x + LADO, yQuadros + LADO, SOMBRA_RECARGA);
                String seg = LogicaHud.segundos(restante);
                g.drawString(fonte, seg, x + (LADO - fonte.width(seg)) / 2 + 1, yQuadros + 4, TEXTO, true);
            } else {
                String letra = h.tecla().name();
                int cor = LogicaHud.semRecurso(s.recurso(), h.custo()) ? TEXTO_SEM_RECURSO : TEXTO;
                g.drawString(fonte, letra, x + (LADO - fonte.width(letra)) / 2 + 1, yQuadros + 4, cor, true);
            }
        }

        double maximo = Math.max(1, s.maximo());
        int larguraBarra = Math.max(larguraTotal, 80);
        int xb = (g.guiWidth() - larguraBarra) / 2;
        int cheio = (int) Math.round(larguraBarra * Math.min(1, Math.max(0, recursoMostrado / maximo)));
        g.fill(xb - 1, yBarra - 1, xb + larguraBarra + 1, yBarra + ALTURA_BARRA + 1, FUNDO);
        g.fill(xb, yBarra, xb + cheio, yBarra + ALTURA_BARRA, 0xFF000000 | s.corRecurso());
        String texto = s.nomeRecurso() + " " + (int) Math.floor(s.recurso()) + "/" + (int) maximo;
        g.drawString(fonte, texto, xb - fonte.width(texto) - 4, yBarra - 2, TEXTO, true);
    }
}
