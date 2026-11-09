package com.minerwk.core.kit;

/**
 * Números de uma habilidade, todos os tempos em ticks.
 *
 * @param cargaMin ticks mínimos de carga antes de poder disparar
 * @param cargaMax ticks de carga em que dispara sozinha (força máxima)
 * @param disparo  duração da fase de disparo
 * @param fim      duração da fase de fim (recuperação)
 * @param segurar  true se carrega segurando a tecla e dispara ao soltar
 * @param custo    quanto do recurso gasta, cobrado no disparo
 * @param recarga  ticks de recarga, contados a partir do disparo
 */
public record Numeros(int cargaMin, int cargaMax, int disparo, int fim, boolean segurar, double custo, int recarga) {
    public Numeros {
        if (cargaMin < 0 || disparo < 0 || fim < 0 || recarga < 0 || custo < 0 || !Double.isFinite(custo)) {
            throw new IllegalArgumentException("número negativo ou inválido na habilidade");
        }
        if (cargaMax < cargaMin) {
            throw new IllegalArgumentException("cargaMax menor que cargaMin");
        }
    }

    /** Habilidade que dispara sozinha depois de {@code carga} ticks. */
    public static Numeros instantanea(int carga, int disparo, int fim, double custo, int recarga) {
        return new Numeros(carga, carga, disparo, fim, false, custo, recarga);
    }
}
