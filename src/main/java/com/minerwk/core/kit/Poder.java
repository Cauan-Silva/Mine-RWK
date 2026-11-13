package com.minerwk.core.kit;

/**
 * A camada de poder comum a todo personagem (spec "Balanceamento"): quanto de vida ele ganha, quanto do
 * dano recebido é cortado, e os multiplicadores dos golpes do kit e dos bosses. Os valores vêm da config
 * do servidor; aqui ficam só as contas.
 *
 * @param vidaExtra              somada à vida máxima de quem tem personagem
 * @param reducaoDeDano          parte do dano recebido que é cortada (0,5 = metade)
 * @param multiplicadorDosGolpes multiplica o dano de todos os golpes do kit (não o do clique)
 * @param vidaDosBosses          multiplica a vida dos bosses
 * @param danoDosBosses          multiplica o dano dos bosses
 */
public record Poder(double vidaExtra, double reducaoDeDano, double multiplicadorDosGolpes, double vidaDosBosses,
                    double danoDosBosses) {
    public static final Poder PADRAO = new Poder(20, .5, 3, 2, 2);

    /** O dano que chega a quem tem personagem. */
    public float recebido(float dano) {
        return (float) Math.max(0, dano * (1 - reducaoDeDano));
    }

    /** O dano de um golpe do kit. */
    public float golpe(float dano) {
        return (float) Math.max(0, dano * multiplicadorDosGolpes);
    }

    public float vidaDoBoss(float vida) {
        return (float) (vida * vidaDosBosses);
    }

    public float danoDoBoss(float dano) {
        return (float) Math.max(0, dano * danoDosBosses);
    }
}
