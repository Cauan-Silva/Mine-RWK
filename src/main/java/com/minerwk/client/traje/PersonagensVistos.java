package com.minerwk.client.traje;

import com.minerwk.core.kit.Forma;
import com.minerwk.rede.PersonagemDe;
import java.util.HashMap;
import java.util.Map;

/**
 * A aparência de cada jogador que este cliente vê (id da entidade → personagem, forma e arma), avisada
 * pelo servidor. O próprio jogador também é avisado.
 */
public final class PersonagensVistos {
    /** O que o servidor avisou de um jogador. */
    public record Visto(String personagem, String forma, boolean semArma) {
        static final Visto NINGUEM = new Visto("", Forma.BASE, false);
    }

    private static final Map<Integer, Visto> VISTOS = new HashMap<>();

    private PersonagensVistos() {
    }

    public static void receber(PersonagemDe pacote) {
        if (pacote.personagem().isEmpty()) VISTOS.remove(pacote.entidade());
        else VISTOS.put(pacote.entidade(), new Visto(pacote.personagem(), pacote.forma(), pacote.semArma()));
    }

    /** O personagem do jogador com esse id de entidade, ou vazio se não escolheu ou não foi avisado. */
    public static String de(int entidade) {
        return visto(entidade).personagem();
    }

    /** A aparência do jogador com esse id de entidade (personagem vazio se não foi avisado). */
    public static Visto visto(int entidade) {
        return VISTOS.getOrDefault(entidade, Visto.NINGUEM);
    }

    public static void limpar() {
        VISTOS.clear();
    }
}
