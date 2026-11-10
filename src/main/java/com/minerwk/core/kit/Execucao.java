package com.minerwk.core.kit;

/**
 * Uma habilidade em uso: CARGA → DISPARO → FIM → ACABOU, um passo por {@link #tick()}.
 * Interromper não é papel desta classe: quem interrompe descarta a execução ({@link KitDoJogador}).
 */
public final class Execucao {
    public enum Fase { CARGA, DISPARO, FIM, ACABOU }

    /** O que aconteceu num tick. */
    public sealed interface Evento permits Simples, Disparou {
        Evento NADA = Simples.NADA;
        Evento TERMINOU = Simples.TERMINOU;
    }

    enum Simples implements Evento { NADA, TERMINOU }

    public record Disparou(float forca) implements Evento {
    }

    private final Habilidade habilidade;
    private final Numeros n;
    private Fase fase = Fase.CARGA;
    private int t;
    private boolean solta;

    public Execucao(Habilidade habilidade) {
        this(habilidade, habilidade.numeros());
    }

    /** Com os números já ajustados pelas melhorias do jogador. */
    public Execucao(Habilidade habilidade, Numeros numeros) {
        this.habilidade = habilidade;
        this.n = numeros;
    }

    public Habilidade habilidade() {
        return habilidade;
    }

    public Fase fase() {
        return fase;
    }

    /** A tecla foi solta. Só importa para habilidades de segurar. */
    public void soltar() {
        solta = true;
    }

    public Evento tick() {
        t++;
        switch (fase) {
            case CARGA -> {
                boolean disparar = t >= n.cargaMax() || (n.segurar() && solta && t >= n.cargaMin());
                if (disparar) {
                    float forca = forca();
                    proxima(Fase.DISPARO);
                    return new Disparou(forca);
                }
            }
            case DISPARO -> {
                if (t >= n.disparo()) proxima(Fase.FIM);
            }
            case FIM -> {
                if (t >= n.fim()) {
                    proxima(Fase.ACABOU);
                    return Evento.TERMINOU;
                }
            }
            case ACABOU -> {
            }
        }
        return Evento.NADA;
    }

    private float forca() {
        if (!n.segurar() || n.cargaMax() == n.cargaMin()) return 1f;
        float f = (t - n.cargaMin()) / (float) (n.cargaMax() - n.cargaMin());
        return Math.max(0f, Math.min(1f, f));
    }

    private void proxima(Fase nova) {
        fase = nova;
        t = 0;
    }
}
