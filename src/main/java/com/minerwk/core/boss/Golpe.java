package com.minerwk.core.boss;

/**
 * Um golpe de boss, com todos os tempos em ticks. O golpe tem três fases: o aviso (a pose e a marca que
 * deixam o jogador reagir), a janela (quando o golpe acerta) e a pausa (o boss fica aberto).
 *
 * @param id         id único no boss
 * @param animacao   animação do modelo que cobre o golpe inteiro
 * @param aviso      ticks de aviso antes de a janela abrir
 * @param janela     ticks em que o golpe pode acertar (pelo menos 1)
 * @param pausa      ticks de recuperação depois da janela
 * @param recarga    ticks até poder repetir este golpe, contados do começo dele
 * @param alcanceMin distância mínima do alvo para o boss escolher este golpe
 * @param alcanceMax distância máxima
 * @param peso       chance relativa de ser escolhido entre os que alcançam (pelo menos 1)
 */
public record Golpe(String id, String animacao, int aviso, int janela, int pausa, int recarga, double alcanceMin,
                    double alcanceMax, int peso) {
    public Golpe {
        if (id.isEmpty() || aviso < 0 || janela < 1 || pausa < 0 || recarga < 0 || alcanceMin < 0
                || alcanceMax < alcanceMin || peso < 1) {
            throw new IllegalArgumentException("golpe inválido: " + id);
        }
    }

    public int duracao() {
        return aviso + janela + pausa;
    }

    public boolean alcanca(double distancia) {
        return distancia >= alcanceMin && distancia <= alcanceMax;
    }
}
