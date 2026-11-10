package com.minerwk.core.maestria;

/**
 * Um nó da árvore de habilidades de um personagem.
 *
 * @param id                id único na árvore; é o que se salva e o que o golpe consulta
 * @param niveis            quantos níveis o nó tem
 * @param custoFixo         custo de cada nível; 0 = o nível {@code k} custa {@code k} pontos (1, 2, 3...)
 * @param nivelMinimo       maestria mínima para comprar
 * @param depoisDoDespertar só pode ser comprado por quem já despertou
 * @param tipo              o que o nó muda
 * @param alvo              id da habilidade, para {@link Tipo#RECARGA} e {@link Tipo#CARGA}; vazio nos outros
 * @param porNivel          fração que cada nível soma ou tira (0,10 = 10%)
 */
public record No(String id, int niveis, int custoFixo, int nivelMinimo, boolean depoisDoDespertar, Tipo tipo,
                 String alvo, double porNivel) {
    public enum Tipo {
        /** O golpe (ou o recurso, ou a passiva) lê o nível e aplica o efeito. */
        MELHORIA,
        /** Diminui a recarga da habilidade {@code alvo}. */
        RECARGA,
        /** Diminui o tempo de carga da habilidade {@code alvo}. */
        CARGA,
        /** O nó que libera o desafio do despertar. */
        DESPERTAR
    }

    public No {
        if (id.isEmpty() || niveis < 1 || custoFixo < 0 || porNivel < 0 || porNivel * niveis >= 1) {
            throw new IllegalArgumentException("nó inválido: " + id);
        }
    }

    public static No melhoria(String id, boolean depoisDoDespertar, double porNivel) {
        return new No(id, 3, 0, 1, depoisDoDespertar, Tipo.MELHORIA, "", porNivel);
    }

    public static No recarga(String id, boolean depoisDoDespertar, String habilidade, double porNivel) {
        return new No(id, 3, 0, 1, depoisDoDespertar, Tipo.RECARGA, habilidade, porNivel);
    }

    public static No carga(String id, boolean depoisDoDespertar, String habilidade, double porNivel) {
        return new No(id, 3, 0, 1, depoisDoDespertar, Tipo.CARGA, habilidade, porNivel);
    }

    public static No despertar(String id, int custo, int nivelMinimo) {
        return new No(id, 1, custo, nivelMinimo, false, Tipo.DESPERTAR, "", 0);
    }

    /** Custo de subir do {@code nivelAtual} para o seguinte; 0 se já está no máximo. */
    public int custoDoProximo(int nivelAtual) {
        if (nivelAtual >= niveis) return 0;
        return custoFixo > 0 ? custoFixo : Math.max(0, nivelAtual) + 1;
    }

    /** Pontos gastos para ter o nó no {@code nivel}. */
    public int custoAte(int nivel) {
        int soma = 0;
        for (int k = 0; k < Math.min(nivel, niveis); k++) soma += custoDoProximo(k);
        return soma;
    }
}
