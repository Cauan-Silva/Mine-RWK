package com.minerwk.client.arena;

import com.minerwk.rede.CenarioArena;

/** Em qual cenário de arena o jogador está, segundo o servidor ({@link CenarioArena}). */
public final class CenarioCliente {
    private static volatile String atual = CenarioArena.NENHUM;

    private CenarioCliente() {
    }

    public static void receber(CenarioArena pacote) {
        atual = pacote.cenario();
    }

    public static String atual() {
        return atual;
    }

    public static void limpar() {
        atual = CenarioArena.NENHUM;
    }
}
