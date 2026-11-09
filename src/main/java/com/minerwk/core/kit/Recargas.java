package com.minerwk.core.kit;

import java.util.HashMap;
import java.util.Map;

/**
 * Recargas das habilidades de um jogador, contadas em ticks do jogo.
 * Lógica pura (sem classes do Minecraft) para poder ser testada com JUnit.
 */
public final class Recargas {
    private final Map<String, Long> prontaNoTick = new HashMap<>();
    private final Map<String, Integer> duracoes = new HashMap<>();

    /** Começa a recarga de {@code habilidade}: fica indisponível por {@code duracao} ticks a partir de {@code agora}. */
    public void iniciar(String habilidade, long agora, int duracao) {
        if (duracao < 0) {
            throw new IllegalArgumentException("duração negativa: " + duracao);
        }
        prontaNoTick.put(habilidade, agora + duracao);
        duracoes.put(habilidade, duracao);
    }

    public boolean pronta(String habilidade, long agora) {
        return restante(habilidade, agora) == 0;
    }

    /** Ticks que faltam para a habilidade ficar pronta (0 se já está). */
    public long restante(String habilidade, long agora) {
        Long pronta = prontaNoTick.get(habilidade);
        return pronta == null ? 0 : Math.max(0, pronta - agora);
    }

    /** Tenta usar: se estiver pronta, inicia a recarga e devolve true. */
    public boolean usar(String habilidade, long agora, int duracao) {
        if (!pronta(habilidade, agora)) {
            return false;
        }
        iniciar(habilidade, agora, duracao);
        return true;
    }

    /** Duração total da última recarga iniciada (para a fração no HUD); 0 se nunca começou nesta sessão. */
    public int duracao(String habilidade) {
        return duracoes.getOrDefault(habilidade, 0);
    }

    /** Cópia das recargas para salvar nos dados do jogador (habilidade → tick em que fica pronta). */
    public Map<String, Long> salvar() {
        return new HashMap<>(prontaNoTick);
    }

    /** Substitui as recargas pelas salvas. */
    public void carregar(Map<String, Long> salvas) {
        prontaNoTick.clear();
        prontaNoTick.putAll(salvas);
        duracoes.clear();
    }

    public void limpar() {
        prontaNoTick.clear();
        duracoes.clear();
    }
}
