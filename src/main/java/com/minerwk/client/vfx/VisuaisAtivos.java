package com.minerwk.client.vfx;

import com.minerwk.core.kit.Vetor;
import com.minerwk.core.vfx.PerfilVfxGojo;
import com.minerwk.core.vfx.TrajetoriaGojo;
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
        /** Se já deixou o rastro (túnel do Roxo, marca do Azul). */
        boolean deixouRastro;

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
        deixarRastros(nivel, agora);
        ATIVOS.removeIf(v -> v.acabou(agora));
        ImpactoCamera.tick();
        DetritosGojo.tick();
    }

    /** Passo da massa do Roxo pelo tipo do visual (0 se não for Roxo). */
    static double passoRoxo(String tipo) {
        return switch (tipo) {
            case "gojo_roxo", "gojo_roxo_rastro" -> TrajetoriaGojo.PASSO_ROXO;
            case "gojo_roxo_200", "gojo_roxo_200_rastro" -> TrajetoriaGojo.PASSO_ROXO_200;
            default -> 0;
        };
    }

    /** O golpe chegou ao fim natural: o que ele deixa no mundo vira um visual novo. */
    private static void deixarRastros(ClientLevel nivel, long agora) {
        List<Visual> novos = new ArrayList<>(0);
        for (Visual v : ATIVOS) {
            if (v.deixouRastro || v.fimEm >= 0 || agora - v.inicio < v.duracao) continue;
            switch (v.tipo) {
                case "gojo_roxo", "gojo_roxo_200" -> {
                    v.deixouRastro = true;
                    // forca guarda o comprimento do túnel.
                    float comprimento = (float) (passoRoxo(v.tipo) * v.duracao);
                    novos.add(new Visual(v.dono, v.tipo + "_rastro", v.origem, v.direcao, comprimento,
                            PerfilVfxGojo.RASTRO_ROXO, agora));
                }
                case "gojo_azul" -> {
                    v.deixouRastro = true;
                    Vetor chao = chao(nivel, v.origem);
                    if (chao != null) {
                        novos.add(new Visual(v.dono, "gojo_azul_marca", chao, v.direcao, 1, PerfilVfxGojo.MARCA_AZUL, agora));
                    }
                }
                default -> {
                }
            }
        }
        for (Visual n : novos) {
            if (ATIVOS.size() >= LIMITE) ATIVOS.remove(0);
            ATIVOS.add(n);
        }
    }

    /** Topo do primeiro bloco sólido até 8 abaixo do ponto. */
    private static Vetor chao(ClientLevel nivel, Vetor p) {
        net.minecraft.core.BlockPos.MutableBlockPos b = net.minecraft.core.BlockPos.containing(p.x(), p.y(), p.z()).mutable();
        for (int i = 0; i < 9; i++, b.move(0, -1, 0)) {
            if (!nivel.getBlockState(b).getCollisionShape(nivel, b).isEmpty()) return new Vetor(p.x(), b.getY() + 1.02, p.z());
        }
        return null;
    }

    public static void limpar() {
        ATIVOS.clear();
        ImpactoCamera.limpar();
        mundo = null;
    }
}
