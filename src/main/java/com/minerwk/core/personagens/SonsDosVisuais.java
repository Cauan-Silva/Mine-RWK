package com.minerwk.core.personagens;

import com.minerwk.core.personagens.gojo.SonsGojo;
import com.minerwk.core.personagens.ichigo.SonsIchigo;
import java.util.Optional;

/**
 * Qual som acompanha um visual, de qualquer personagem. O cliente pergunta aqui; cada personagem responde
 * pelos seus visuais (os tipos começam com o id dele, então nunca se sobrepõem).
 */
public final class SonsDosVisuais {
    private SonsDosVisuais() {
    }

    /** O som que toca quando o visual {@code tipo} começa. */
    public static Optional<String> doVisual(String tipo) {
        return SonsGojo.doVisual(tipo).or(() -> SonsIchigo.doVisual(tipo));
    }

    /** Se o som do visual {@code tipo} para quando o visual é encerrado antes da hora. */
    public static boolean cortaComOVisual(String tipo) {
        return SonsGojo.cortaComOVisual(tipo) || SonsIchigo.cortaComOVisual(tipo);
    }

    /** O som que toca um pouco antes de o visual {@code tipo} acabar. */
    public static Optional<SonsGojo.NoFim> antesDoFim(String tipo) {
        return SonsGojo.antesDoFim(tipo).or(() -> SonsIchigo.antesDoFim(tipo));
    }
}
