package com.minerwk.core.camera;

import com.minerwk.core.kit.Vetor;
import java.util.List;

/**
 * Movimento de câmera de uma cutscene: quadros-chave na base de quem usa o golpe (lado, cima, frente,
 * a partir dos olhos), interpolados suavemente. Uma cutscene nova é só uma lista nova de quadros.
 */
public final class Coreografia {
    /**
     * Um quadro-chave. {@code posicao} e {@code alvo} na base do golpe; {@code fov} multiplica o FOV do
     * jogador; {@code inclinacao} em graus. Com {@code corte}, a câmera não desliza até ele: segura o
     * quadro anterior e pula no tick dele (corte seco de cinema).
     */
    public record Quadro(int tick, Vetor posicao, Vetor alvo, double fov, double inclinacao, boolean corte) {
        public Quadro(int tick, Vetor posicao, Vetor alvo, double fov, double inclinacao) {
            this(tick, posicao, alvo, fov, inclinacao, false);
        }
    }

    private final List<Quadro> quadros;

    public Coreografia(List<Quadro> quadros) {
        if (quadros.isEmpty()) throw new IllegalArgumentException("coreografia sem quadros");
        for (int i = 1; i < quadros.size(); i++) {
            if (quadros.get(i).tick() <= quadros.get(i - 1).tick()) {
                throw new IllegalArgumentException("quadros fora de ordem no tick " + quadros.get(i).tick());
            }
        }
        this.quadros = List.copyOf(quadros);
    }

    public int duracao() {
        return quadros.get(quadros.size() - 1).tick();
    }

    /** O quadro no tick {@code t} (fracionário), entre os dois quadros-chave vizinhos. */
    public Quadro quadro(double t) {
        Quadro primeiro = quadros.get(0);
        if (t <= primeiro.tick()) return primeiro;
        for (int i = 1; i < quadros.size(); i++) {
            Quadro b = quadros.get(i);
            if (t > b.tick()) continue;
            Quadro a = quadros.get(i - 1);
            if (b.corte() && t < b.tick()) return a;
            double k = suave((t - a.tick()) / (b.tick() - a.tick()));
            return new Quadro((int) t, mistura(a.posicao(), b.posicao(), k), mistura(a.alvo(), b.alvo(), k),
                    a.fov() + (b.fov() - a.fov()) * k, a.inclinacao() + (b.inclinacao() - a.inclinacao()) * k);
        }
        return quadros.get(quadros.size() - 1);
    }

    /** Ponto do mundo para {@code local} (lado, cima, frente) em volta de {@code origem}, virado para {@code direcao}. */
    public static Vetor naBase(Vetor local, Vetor origem, Vetor direcao) {
        Vetor frente = direcao.comprimento() < 1e-9 ? new Vetor(0, 0, 1) : direcao.normalizado();
        Vetor lado = cruz(frente, new Vetor(0, 1, 0));
        if (lado.comprimento() < 1e-6) lado = new Vetor(1, 0, 0);
        lado = lado.normalizado();
        Vetor cima = cruz(lado, frente).normalizado();
        return origem.mais(lado.vezes(local.x())).mais(cima.vezes(local.y())).mais(frente.vezes(local.z()));
    }

    private static Vetor cruz(Vetor a, Vetor b) {
        return new Vetor(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(), a.x() * b.y() - a.y() * b.x());
    }

    private static Vetor mistura(Vetor a, Vetor b, double k) {
        return a.mais(b.menos(a).vezes(k));
    }

    private static double suave(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
