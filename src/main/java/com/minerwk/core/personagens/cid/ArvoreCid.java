package com.minerwk.core.personagens.cid;

import com.minerwk.core.hud.FormaArvore;
import com.minerwk.core.hud.FormaArvore.Galho;
import com.minerwk.core.hud.FormaArvore.Selo;
import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.maestria.No;
import java.util.List;
import java.util.Map;

/**
 * A árvore de habilidades do Cid. Antes do Despertar ela melhora os golpes base e a Magia; depois de vencer
 * o Ragnarok, a Espada Atômica, o recuo do Overdrive e o Atomic. A recarga do R não entra: ele a divide com o
 * Recovery Atomic.
 */
public final class ArvoreCid {
    public static final String REDEMOINHO_FORCA = "cid_redemoinho_forca";
    public static final String REDEMOINHO_RECARGA = "cid_redemoinho_recarga";
    public static final String POS_IMAGEM_RECARGA = "cid_pos_imagem_recarga";
    public static final String PRISAO_DURACAO = "cid_prisao_duracao";
    public static final String MAGIA_RESERVA = "cid_magia_reserva";
    public static final String MAGIA_FLUXO = "cid_magia_fluxo";
    public static final String DESPERTAR = "cid_despertar";
    public static final String ESPADA_FORCA = "cid_espada_forca";
    public static final String ESPADA_RECARGA = "cid_espada_recarga";
    public static final String OVERDRIVE_RECUO = "cid_overdrive_recuo";
    public static final String ATOMIC_FORCA = "cid_atomic_forca";

    public static final Arvore ARVORE = new Arvore(List.of(
            No.melhoria(REDEMOINHO_FORCA, false, .08),
            No.recarga(REDEMOINHO_RECARGA, false, Cid.Redemoinho.VISUAL, .08),
            No.recarga(POS_IMAGEM_RECARGA, false, Cid.PosImagem.VISUAL, .08),
            No.melhoria(PRISAO_DURACAO, false, .10),
            No.melhoria(MAGIA_RESERVA, false, .10),
            No.melhoria(MAGIA_FLUXO, false, .10),
            No.despertar(DESPERTAR, 5, 20),
            No.melhoria(ESPADA_FORCA, true, .08),
            No.recarga(ESPADA_RECARGA, true, Cid.ESPADA_ATOMICA, .08),
            No.melhoria(OVERDRIVE_RECUO, true, .15),
            No.melhoria(ATOMIC_FORCA, true, .08)));

    /** Como a árvore é desenhada (spec [[2026-11-12 UI dos personagens]]). */
    public static final FormaArvore FORMA = new FormaArvore(List.of(
            Galho.antes("redemoinho", "cid_redemoinho", List.of(REDEMOINHO_FORCA, REDEMOINHO_RECARGA), Map.of()),
            Galho.antes("pos_imagem", "cid_pos_imagem", List.of(POS_IMAGEM_RECARGA), Map.of()),
            Galho.antes("prisao", "cid_prisao", List.of(PRISAO_DURACAO), Map.of(PRISAO_DURACAO, Selo.DURACAO)),
            Galho.antes("magia", "cid_recurso", List.of(MAGIA_RESERVA, MAGIA_FLUXO),
                    Map.of(MAGIA_RESERVA, Selo.RECURSO, MAGIA_FLUXO, Selo.RECURSO)),
            Galho.depois("espada", "cid_espada_atomica", List.of(ESPADA_FORCA, ESPADA_RECARGA), Map.of()),
            Galho.depois("overdrive", "cid_overdrive", List.of(OVERDRIVE_RECUO), Map.of()),
            Galho.depois("atomic", "cid_atomic", List.of(ATOMIC_FORCA), Map.of())));

    private ArvoreCid() {
    }
}
