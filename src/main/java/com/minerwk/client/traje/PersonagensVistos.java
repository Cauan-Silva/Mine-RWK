package com.minerwk.client.traje;

import com.minerwk.rede.PersonagemDe;
import java.util.HashMap;
import java.util.Map;

/** O personagem de cada jogador que este cliente vê (id da entidade → id do personagem), avisado pelo servidor. */
public final class PersonagensVistos {
    private static final Map<Integer, String> VISTOS = new HashMap<>();

    private PersonagensVistos() {
    }

    public static void receber(PersonagemDe pacote) {
        if (pacote.personagem().isEmpty()) VISTOS.remove(pacote.entidade());
        else VISTOS.put(pacote.entidade(), pacote.personagem());
    }

    /** O personagem do jogador com esse id de entidade, ou vazio se não escolheu ou não foi avisado. */
    public static String de(int entidade) {
        return VISTOS.getOrDefault(entidade, "");
    }

    public static void limpar() {
        VISTOS.clear();
    }
}
