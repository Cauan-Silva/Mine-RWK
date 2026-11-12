package com.minerwk.core.personagens.cid;

import java.util.List;
import java.util.Optional;

/**
 * Ids dos sons do Cid (gerados por {@code ferramentas/gerar_sfx_cid.py}) e qual som acompanha cada visual.
 * Todo som do Cid sai do começo de um visual: assim ele soa junto com o que se vê, do lugar certo, e para
 * com o visual quando é o caso. Sem classes do Minecraft; o registro fica em {@code registro/Sons}.
 */
public final class SonsCid {
    public static final String REDEMOINHO = "cid_redemoinho";
    public static final String POS_IMAGEM = "cid_pos_imagem";
    public static final String SLIME = "cid_slime";
    public static final String CORRENTES = "cid_correntes";
    public static final String OVERDRIVE = "cid_overdrive";
    public static final String TRAJE = "cid_traje";
    /** A Espada Atômica e o Recovery Atomic: a carga é um som e o golpe é outro. */
    public static final String ESPADA_CARGA = "cid_espada_carga", ESPADA_ATOMICA = "cid_espada_atomica";
    public static final String RECOVERY_CARGA = "cid_recovery_carga", RECOVERY = "cid_recovery";

    public static final List<String> TODOS = List.of(REDEMOINHO, POS_IMAGEM, SLIME, CORRENTES, OVERDRIVE, TRAJE,
            ESPADA_CARGA, ESPADA_ATOMICA, RECOVERY_CARGA, RECOVERY);

    private SonsCid() {
    }

    /** O som que toca quando o visual {@code tipo} começa, no ponto do visual. */
    public static Optional<String> doVisual(String tipo) {
        // Cada visual do Cid tem um som com o mesmo id.
        return TODOS.contains(tipo) ? Optional.of(tipo) : Optional.empty();
    }

    /**
     * Se o som do visual {@code tipo} para quando o visual é encerrado antes da hora: o zumbido do Overdrive
     * (desligado com G, ou quem o usa morreu) e as duas cargas atômicas (interrompidas; no disparo elas já
     * acabaram). O slime que bate cedo ainda soa o arremesso, que é curto.
     */
    public static boolean cortaComOVisual(String tipo) {
        return tipo.equals(OVERDRIVE) || tipo.equals(ESPADA_CARGA) || tipo.equals(RECOVERY_CARGA);
    }
}
