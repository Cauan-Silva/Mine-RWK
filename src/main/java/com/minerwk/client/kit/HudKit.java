package com.minerwk.client.kit;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.core.hud.LogicaHud;
import com.minerwk.core.hud.Pronto;
import com.minerwk.core.hud.TemaUi;
import com.minerwk.core.hud.Temas;
import com.minerwk.rede.SincronizarKit;
import com.minerwk.rede.SincronizarKit.HabilidadeHud;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;

/**
 * HUD de recurso e recargas, acima dos corações e da fome, no tema do personagem (spec [[2026-11-12 UI dos
 * personagens]]): o ícone de cada golpe com a moldura do tema, a recarga em varredura de relógio, um brilho quando
 * fica pronto, e a barra de recurso com o ícone e as cores do tema.
 */
public final class HudKit {
    public static final ResourceLocation CAMADA = MineRWK.id("hud_kit");

    private static final int LADO = 20;
    private static final int ESPACO = 4;
    private static final int ALTURA_BARRA = 4;
    /** Folga acima da pilha vanilla (vida, armadura, fome, montaria, ar). */
    private static final int FOLGA = 6;
    private static final int COR_XP = 0xFFE8B84A;
    private static final int COR_CADEADO = 0xFF8A8A95;

    private static final int FUNDO = 0xAA000000;
    private static final int SOMBRA_RECARGA = 0xB0000000;
    private static final int VEU_TRANCADO = 0xC0000000;
    private static final int VEU_SEM_RECURSO = 0x80404040;
    private static final int TEXTO = 0xFFFFFFFF;
    private static final int TEXTO_SEM_RECURSO = 0xFFFF5555;

    /** Valor mostrado da barra, que desliza até o recebido; -1 = pular direto para o valor. */
    private static double recursoMostrado = -1;
    private static String personagemMostrado = "";
    private static final Pronto PRONTO = new Pronto();

    private HudKit() {
    }

    public static void registrar(RegisterGuiLayersEvent evento) {
        evento.registerAbove(VanillaGuiLayers.AIR_LEVEL, CAMADA, HudKit::desenhar);
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        SincronizarKit s = RecepcaoKit.estado();
        if (s.personagem().isEmpty() || !s.personagem().equals(personagemMostrado)) {
            // Saiu do mundo, entrou ou trocou de personagem: a barra não desliza a partir de um valor velho,
            // e os golpes que já estavam prontos não piscam.
            personagemMostrado = s.personagem();
            recursoMostrado = -1;
            PRONTO.limpar();
            if (s.personagem().isEmpty()) return;
        }
        double alvo = s.recurso();
        if (recursoMostrado < 0 || Math.abs(alvo - recursoMostrado) < 0.05) recursoMostrado = alvo;
        else recursoMostrado += (alvo - recursoMostrado) * 0.35;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        for (HabilidadeHud h : s.habilidades()) {
            if (!h.trancada()) PRONTO.ver(h.id(), h.prontaNoTick() > agora, agora);
        }
    }

    /** Golpe que ainda exige o despertar: o quadro fica escuro, com um cadeado no meio. */
    private static void desenharCadeado(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + LADO, y + LADO, VEU_TRANCADO);
        int cx = x + LADO / 2, y0 = y + 2 + (LADO - 16) / 2;
        // Argola (contorno de 6 x 4) e corpo (8 x 6).
        g.fill(cx - 3, y0 + 3, cx + 3, y0 + 4, COR_CADEADO);
        g.fill(cx - 3, y0 + 4, cx - 2, y0 + 7, COR_CADEADO);
        g.fill(cx + 2, y0 + 4, cx + 3, y0 + 7, COR_CADEADO);
        g.fill(cx - 4, y0 + 7, cx + 4, y0 + 13, COR_CADEADO);
        g.fill(cx - 1, y0 + 9, cx + 1, y0 + 11, VEU_TRANCADO);
    }

    /** A parte escura da recarga, em leque a partir do centro (cada triângulo nos dois sentidos, sem depender do cull). */
    private static void desenharVarredura(GuiGraphics g, int x, int y, float restante) {
        float[] p = LogicaHud.varredura(restante, LADO);
        if (p.length < 6) return;
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        float cx = x + p[0], cy = y + p[1];
        for (int i = 2; i + 3 < p.length; i += 2) {
            float ax = x + p[i], ay = y + p[i + 1], bx = x + p[i + 2], by = y + p[i + 3];
            vc.addVertex(m, cx, cy, 0).setColor(SOMBRA_RECARGA);
            vc.addVertex(m, ax, ay, 0).setColor(SOMBRA_RECARGA);
            vc.addVertex(m, bx, by, 0).setColor(SOMBRA_RECARGA);
            vc.addVertex(m, bx, by, 0).setColor(SOMBRA_RECARGA);
            vc.addVertex(m, cx, cy, 0).setColor(SOMBRA_RECARGA);
            vc.addVertex(m, bx, by, 0).setColor(SOMBRA_RECARGA);
            vc.addVertex(m, ax, ay, 0).setColor(SOMBRA_RECARGA);
            vc.addVertex(m, ax, ay, 0).setColor(SOMBRA_RECARGA);
        }
    }

    /** Um degradê da esquerda ({@code de}) para a direita ({@code para}). */
    private static void degradeHorizontal(GuiGraphics g, int x0, int y0, int x1, int y1, int de, int para) {
        if (x1 <= x0) return;
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        vc.addVertex(m, x0, y0, 0).setColor(de);
        vc.addVertex(m, x0, y1, 0).setColor(de);
        vc.addVertex(m, x1, y1, 0).setColor(para);
        vc.addVertex(m, x1, y0, 0).setColor(para);
    }

    private static void textoPequeno(GuiGraphics g, Font fonte, String texto, int xDireita, int yBaixo, int cor) {
        g.pose().pushPose();
        g.pose().translate(xDireita - fonte.width(texto) * .5f, yBaixo - 4.5f, 0);
        g.pose().scale(.5f, .5f, 1);
        g.drawString(fonte, texto, 0, 0, cor, true);
        g.pose().popPose();
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

        TemaUi tema = Temas.tema(s.personagem(), s.forma());
        Font fonte = mc.font;
        long agora = mc.level.getGameTime();
        int larguraTotal = habilidades.size() * (LADO + ESPACO) - ESPACO;
        int x0 = (g.guiWidth() - larguraTotal) / 2;
        // A pilha vanilla (vida, armadura, fome, montaria, ar) já desenhou e diz até onde subiu.
        int pilha = Math.max(mc.gui.leftHeight, mc.gui.rightHeight);
        int yBarra = g.guiHeight() - pilha - FOLGA - ALTURA_BARRA;
        int yQuadros = yBarra - 4 - LADO;

        for (int i = 0; i < habilidades.size(); i++) {
            HabilidadeHud h = habilidades.get(i);
            int x = x0 + i * (LADO + ESPACO);
            boolean emUso = h.id().equals(s.emUso());
            Molduras.desenhar(g, x, yQuadros, LADO, tema, emUso ? tema.corPrincipal() : tema.corBorda());
            g.fill(x, yQuadros, x + LADO, yQuadros + LADO, FUNDO);
            boolean comIcone = Molduras.temIcone(h.id());
            if (comIcone) Molduras.icone(g, h.id(), x, yQuadros, LADO);
            String letra = h.tecla().name();

            if (h.trancada()) {
                desenharCadeado(g, x, yQuadros);
                continue;
            }
            long restante = h.prontaNoTick() - agora;
            // Relógio do cliente atrasado não mostra mais tempo que a recarga tem.
            if (h.duracao() > 0) restante = Math.min(restante, h.duracao());
            boolean semRecurso = LogicaHud.semRecurso(s.recurso(), h.custo());
            int yTexto = yQuadros + (LADO - 8) / 2;
            if (restante > 0) {
                float fracao = LogicaHud.fracao(h.prontaNoTick(), h.duracao(), agora);
                desenharVarredura(g, x, yQuadros, fracao > 0 ? fracao : 1f);
                String seg = LogicaHud.segundos(restante);
                g.drawString(fonte, seg, x + (LADO - fonte.width(seg)) / 2 + 1, yTexto, TEXTO, true);
            } else if (semRecurso) {
                g.fill(x, yQuadros, x + LADO, yQuadros + LADO, VEU_SEM_RECURSO);
                String custo = String.valueOf((int) Math.ceil(h.custo()));
                g.drawString(fonte, custo, x + (LADO - fonte.width(custo)) / 2 + 1, yTexto, TEXTO_SEM_RECURSO, true);
            } else if (!comIcone) {
                g.drawString(fonte, letra, x + (LADO - fonte.width(letra)) / 2 + 1, yTexto, TEXTO, true);
            }
            if (comIcone) textoPequeno(g, fonte, letra, x + LADO - 1, yQuadros + LADO - 1, TEXTO);

            long pronta = PRONTO.prontaEm(h.id());
            float brilho = pronta < 0 ? 0 : LogicaHud.brilho(agora - pronta);
            if (brilho > 0) {
                int alfa = Math.round(brilho * 0xA0);
                g.fill(x, yQuadros, x + LADO, yQuadros + LADO, (alfa << 24) | (tema.corPrincipal() & 0xFFFFFF));
            }
        }

        // Sem poderes (depois do Mugetsu): o aviso em cima dos quadros, com o tempo que falta.
        List<Long> restantes = new java.util.ArrayList<>();
        for (HabilidadeHud h : habilidades) {
            if (!h.trancada()) restantes.add(h.prontaNoTick() - agora);
        }
        long semPoderes = LogicaHud.semPoderes(s.recurso(), restantes);
        if (semPoderes > 0) {
            var aviso = Component.translatable("hud.minerwk.sem_poderes", LogicaHud.segundos(semPoderes));
            g.drawString(fonte, aviso, (g.guiWidth() - fonte.width(aviso)) / 2, yQuadros - 12, TEXTO_SEM_RECURSO, true);
        }

        double maximo = Math.max(1, s.maximo());
        int larguraBarra = Math.max(larguraTotal, 80);
        int xb = (g.guiWidth() - larguraBarra) / 2;
        int cheio = (int) Math.round(larguraBarra * Math.min(1, Math.max(0, recursoMostrado / maximo)));
        g.fill(xb - 1, yBarra - 1, xb + larguraBarra + 1, yBarra + ALTURA_BARRA + 1, FUNDO);
        if (Temas.temTema(s.personagem())) {
            degradeHorizontal(g, xb, yBarra, xb + cheio, yBarra + ALTURA_BARRA, tema.corPrincipal(), tema.corSecundaria());
        } else {
            g.fill(xb, yBarra, xb + cheio, yBarra + ALTURA_BARRA, 0xFF000000 | s.corRecurso());
        }
        String recursoIcone = s.personagem() + "_recurso";
        String texto = (int) Math.floor(s.recurso()) + "/" + (int) maximo;
        int xTexto = xb - fonte.width(texto) - 4;
        if (Molduras.temIcone(recursoIcone)) {
            Molduras.icone(g, recursoIcone, xb - 11, yBarra - 3, 9);
            xTexto -= 11;
        } else {
            texto = s.nomeRecurso() + " " + texto;
            xTexto = xb - fonte.width(texto) - 4;
        }
        g.drawString(fonte, texto, xTexto, yBarra - 2, TEXTO, true);

        // Maestria: o nível à direita da barra e uma linha fina de XP embaixo dela.
        if (s.nivel() > 0) {
            int yXp = yBarra + ALTURA_BARRA + 2;
            int cheioXp = (int) Math.round(larguraBarra * LogicaHud.fracaoXp(s.xpNoNivel(), s.xpProximo()));
            g.fill(xb - 1, yXp, xb + larguraBarra + 1, yXp + 2, FUNDO);
            g.fill(xb, yXp, xb + cheioXp, yXp + 1, COR_XP);
            g.drawString(fonte, Component.translatable("hud.minerwk.nivel", s.nivel()),
                    xb + larguraBarra + 4, yBarra - 2, COR_XP, true);
        }
    }
}
