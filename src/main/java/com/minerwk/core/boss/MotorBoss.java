package com.minerwk.core.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A máquina de golpes de um boss, sem classes do Minecraft: escolhe o próximo golpe pela distância do alvo,
 * pelas recargas e pelo peso, e conta as fases dele. Quem usa chama {@link #tick} uma vez por tick e reage
 * aos eventos; o que cada golpe faz no mundo fica com a entidade.
 */
public final class MotorBoss {
    /** O que aconteceu num tick. */
    public enum Evento { NADA, COMECOU, ABRIU_JANELA, FECHOU_JANELA, TERMINOU }

    private List<Golpe> golpes;
    private final int intervalo;
    /** Tick a partir do qual cada golpe pode ser repetido. */
    private final Map<String, Long> prontoEm = new HashMap<>();
    private double ritmo = 1;
    private Golpe atual;
    private int t;
    /** Tick a partir do qual o boss pode começar outro golpe (a folga entre dois golpes). */
    private long livreEm;

    /** @param intervalo ticks de folga entre o fim de um golpe e o começo do próximo */
    public MotorBoss(List<Golpe> golpes, int intervalo) {
        this.golpes = List.copyOf(golpes);
        this.intervalo = Math.max(0, intervalo);
    }

    /** Troca os golpes que o boss pode escolher (mudou de fase). O golpe em andamento e as recargas continuam. */
    public void trocarGolpes(List<Golpe> novos) {
        golpes = List.copyOf(novos);
    }

    /** O golpe em andamento, ou null. */
    public Golpe atual() {
        return atual;
    }

    public boolean noAviso() {
        return atual != null && t < atual.aviso();
    }

    public boolean naJanela() {
        return atual != null && t >= atual.aviso() && t < atual.aviso() + atual.janela();
    }

    /** Ticks desde que a janela abriu (0 no tick em que abre); só vale {@link #naJanela}. */
    public int tempoNaJanela() {
        return atual == null ? 0 : t - atual.aviso();
    }

    /** Ticks desde o começo do golpe em andamento. */
    public int tempoNoGolpe() {
        return t;
    }

    /** Encurta (fator menor que 1) as recargas e a folga dali em diante: o boss fica mais agressivo. */
    public void acelerar(double fator) {
        ritmo = Math.max(.1, fator);
    }

    /** Larga o golpe em andamento sem terminar (o boss foi atordoado ou a luta parou). */
    public void parar(long agora) {
        atual = null;
        t = 0;
        livreEm = agora + Math.round(intervalo * ritmo);
    }

    /** Começa {@code golpe} agora, largando o que estiver em andamento e sem olhar alcance nem recarga. */
    public void forcar(Golpe golpe, long agora) {
        comecar(golpe, agora);
        // O tick em que foi forçado já conta como o primeiro do aviso.
        t = 0;
    }

    private void comecar(Golpe golpe, long agora) {
        atual = golpe;
        t = 0;
        prontoEm.put(golpe.id(), agora + Math.round(golpe.recarga() * ritmo));
    }

    /**
     * Um tick. {@code distancia} é até o alvo; {@code sorteio}, um número em [0, 1) que decide entre os golpes
     * possíveis (quem chama fornece, para o motor ser previsível nos testes).
     */
    public Evento tick(double distancia, long agora, double sorteio) {
        if (atual == null) {
            if (agora < livreEm) return Evento.NADA;
            Golpe escolhido = escolher(distancia, agora, sorteio);
            if (escolhido == null) return Evento.NADA;
            comecar(escolhido, agora);
            return Evento.COMECOU;
        }
        t++;
        if (t == atual.aviso()) return Evento.ABRIU_JANELA;
        if (t == atual.aviso() + atual.janela()) {
            if (atual.pausa() == 0) return terminar(agora);
            return Evento.FECHOU_JANELA;
        }
        if (t >= atual.duracao()) return terminar(agora);
        return Evento.NADA;
    }

    private Evento terminar(long agora) {
        parar(agora);
        return Evento.TERMINOU;
    }

    private Golpe escolher(double distancia, long agora, double sorteio) {
        List<Golpe> possiveis = new ArrayList<>();
        int total = 0;
        for (Golpe g : golpes) {
            if (g.alcanca(distancia) && agora >= prontoEm.getOrDefault(g.id(), 0L)) {
                possiveis.add(g);
                total += g.peso();
            }
        }
        if (possiveis.isEmpty()) return null;
        double ponto = Math.max(0, Math.min(.999999, sorteio)) * total;
        for (Golpe g : possiveis) {
            ponto -= g.peso();
            if (ponto < 0) return g;
        }
        return possiveis.get(possiveis.size() - 1);
    }
}
