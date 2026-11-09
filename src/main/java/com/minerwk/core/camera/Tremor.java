package com.minerwk.core.camera;

import java.util.ArrayList;
import java.util.List;

/**
 * Tremor de tela: soma de tremores ativos que decaem suavemente até zero.
 * Sem aleatório (senos de frequências diferentes) para ser testável.
 */
public final class Tremor {
    /** Força máxima da soma. */
    public static final float LIMITE = 1.5f;
    /** Graus de câmera por unidade de força. */
    public static final float GRAUS = 2f;

    private static final float HZ_GUINADA = 23f;
    private static final float HZ_INCLINACAO = 31f;
    private static final float HZ_ROTACAO = 17f;

    public record Deslocamento(float guinada, float inclinacao, float rotacao) {
        public static final Deslocamento NENHUM = new Deslocamento(0, 0, 0);
    }

    private static final class Ativo {
        final float forca;
        final int duracao;
        int t;

        Ativo(float forca, int duracao) {
            this.forca = forca;
            this.duracao = duracao;
        }

        float agora() {
            float resto = 1f - t / (float) duracao;
            return forca * resto * resto;
        }
    }

    private final List<Ativo> ativos = new ArrayList<>();

    public void adicionar(float forca, int duracao) {
        if (forca > 0 && duracao > 0) ativos.add(new Ativo(forca, duracao));
    }

    public void tick() {
        for (Ativo a : ativos) a.t++;
        ativos.removeIf(a -> a.t >= a.duracao);
    }

    public int ativos() {
        return ativos.size();
    }

    public float forcaAtual() {
        float soma = 0;
        for (Ativo a : ativos) soma += a.agora();
        return Math.min(LIMITE, soma);
    }

    /** @param tempo segundos (contínuo, com a fração do tick) @param intensidade da configuração, 0 a 1 */
    public Deslocamento deslocamento(float tempo, float intensidade) {
        float f = forcaAtual() * intensidade * GRAUS;
        if (f == 0) return Deslocamento.NENHUM;
        double doisPi = 2 * Math.PI;
        return new Deslocamento(
                (float) (f * Math.sin(doisPi * HZ_GUINADA * tempo)),
                (float) (f * Math.sin(doisPi * HZ_INCLINACAO * tempo + 1.3)),
                (float) (0.5f * f * Math.sin(doisPi * HZ_ROTACAO * tempo + 2.1)));
    }

    public void limpar() {
        ativos.clear();
    }
}
