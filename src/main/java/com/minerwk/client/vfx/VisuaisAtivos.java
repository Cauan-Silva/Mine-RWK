package com.minerwk.client.vfx;

import com.minerwk.core.kit.Vetor;
import com.minerwk.core.vfx.PerfilVfxGojo;
import com.minerwk.rede.EncerrarVisual;
import com.minerwk.rede.MostrarVisual;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Visuais de golpe em andamento no cliente. Cada um sabe o tick em que começou (do servidor) e se
 * desenha sozinho; somem ao expirar, ao trocar de mundo e ao sair. Só na thread do cliente.
 */
public final class VisuaisAtivos {
    /** Limite para um servidor cheio de golpes não derrubar o FPS. */
    static final int LIMITE = 64;
    /** Ticks que um visual encerrado antes da hora leva para sumir. */
    static final int SAIDA = 4;

    /** Um visual em andamento. {@code fimEm} é o tick em que foi encerrado antes da hora (ou -1). */
    public static final class Visual {
        final int dono;
        final String tipo;
        final Vetor origem;
        final Vetor direcao;
        final float forca;
        final int duracao;
        final long inicio;
        long fimEm = -1;

        Visual(int dono, String tipo, Vetor origem, Vetor direcao, float forca, int duracao, long inicio) {
            this.dono = dono;
            this.tipo = tipo;
            this.origem = origem;
            this.direcao = direcao;
            this.forca = forca;
            this.duracao = duracao;
            this.inicio = inicio;
        }

        /** Ticks desde o começo, com a fração do quadro. */
        double idade(long agora, float parcial) {
            return Math.max(0, agora - inicio + parcial);
        }

        /** Opacidade geral: entrada e saída suaves, e saída rápida se foi encerrado antes. */
        float opacidade(long agora, float parcial) {
            double t = idade(agora, parcial);
            double o = PerfilVfxGojo.opacidade(t, duracao);
            if (fimEm >= 0) o *= Math.max(0, 1 - (agora - fimEm + parcial) / SAIDA);
            return (float) o;
        }

        boolean acabou(long agora) {
            return agora - inicio > duracao + 2 || (fimEm >= 0 && agora - fimEm > SAIDA);
        }
    }

    private static final List<Visual> ATIVOS = new ArrayList<>();
    private static ClientLevel mundo;

    private VisuaisAtivos() {
    }

    public static List<Visual> ativos() {
        return ATIVOS;
    }

    public static void receber(MostrarVisual p) {
        if (ATIVOS.size() >= LIMITE) ATIVOS.remove(0);
        ATIVOS.add(new Visual(p.dono(), p.tipo(), p.origem(), p.direcao(), p.forca(), p.duracao(), p.inicio()));
        ImpactoCamera.momento(p.tipo(), p.origem());
        DetritosGojo.aoComecar(p.tipo(), p.origem());
    }

    public static void encerrar(EncerrarVisual p) {
        ClientLevel nivel = Minecraft.getInstance().level;
        if (nivel == null) return;
        long agora = nivel.getGameTime();
        for (int i = ATIVOS.size() - 1; i >= 0; i--) {
            Visual v = ATIVOS.get(i);
            if (v.dono != p.dono() || !v.tipo.equals(p.tipo()) || v.fimEm >= 0) continue;
            if (p.tipo().equals("gojo_vermelho")) {
                // A esfera bateu (ou chegou ao fim): some e nasce a explosão no ponto exato.
                ATIVOS.remove(i);
                ATIVOS.add(new Visual(v.dono, "gojo_vermelho_explosao", p.ponto(), v.direcao, v.forca,
                        PerfilVfxGojo.EXPLOSAO_VERMELHO, agora));
                ImpactoCamera.momento("gojo_vermelho_explosao", p.ponto());
                DetritosGojo.explosao(p.ponto());
            } else {
                v.fimEm = agora;
            }
            return;
        }
    }

    /** Um tick do cliente: tira o que acabou e limpa tudo ao trocar de mundo. */
    public static void tick() {
        ClientLevel nivel = Minecraft.getInstance().level;
        if (nivel != mundo) {
            ATIVOS.clear();
            mundo = nivel;
        }
        if (nivel == null) return;
        long agora = nivel.getGameTime();
        ATIVOS.removeIf(v -> v.acabou(agora));
        ImpactoCamera.tick();
        DetritosGojo.tick();
    }

    public static void limpar() {
        ATIVOS.clear();
        ImpactoCamera.limpar();
        mundo = null;
    }
}
