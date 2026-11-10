package com.minerwk.core.personagens.gojo;

import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.maestria.No;
import java.util.List;

/** A árvore de habilidades do Gojo. Os números vêm do spec "Maestria e árvore de habilidades". */
public final class ArvoreGojo {
    public static final String AZUL_ALCANCE = "gojo_azul_alcance";
    public static final String AZUL_RECARGA = "gojo_azul_recarga";
    public static final String INFINITO_DURACAO = "gojo_infinito_duracao";
    public static final String INFINITO_RECARGA = "gojo_infinito_recarga";
    public static final String ENERGIA_RESERVA = "gojo_energia_reserva";
    public static final String ENERGIA_FLUXO = "gojo_energia_fluxo";
    public static final String DESPERTAR = "gojo_despertar";
    public static final String VERMELHO_FORCA = "gojo_vermelho_forca";
    public static final String VERMELHO_CARGA = "gojo_vermelho_carga_rapida";
    public static final String ROXO_FORCA = "gojo_roxo_forca";
    public static final String ROXO_RECARGA = "gojo_roxo_recarga";
    public static final String TECNICA_REVERSA = "gojo_tecnica_reversa";
    public static final String DOMINIO_DURACAO = "gojo_dominio_duracao";
    public static final String ROXO_MAXIMO_RECARGA = "gojo_roxo_maximo_recarga";

    public static final Arvore ARVORE = new Arvore(List.of(
            No.melhoria(AZUL_ALCANCE, false, .10),
            No.recarga(AZUL_RECARGA, false, "gojo_azul", .08),
            No.melhoria(INFINITO_DURACAO, false, .15),
            No.recarga(INFINITO_RECARGA, false, "gojo_infinito", .08),
            No.melhoria(ENERGIA_RESERVA, false, .10),
            No.melhoria(ENERGIA_FLUXO, false, .10),
            No.despertar(DESPERTAR, 5, 20),
            No.melhoria(VERMELHO_FORCA, true, .08),
            No.carga(VERMELHO_CARGA, true, "gojo_vermelho", .10),
            No.melhoria(ROXO_FORCA, true, .08),
            No.recarga(ROXO_RECARGA, true, "gojo_roxo", .08),
            No.melhoria(TECNICA_REVERSA, true, .10),
            No.melhoria(DOMINIO_DURACAO, true, .10),
            No.recarga(ROXO_MAXIMO_RECARGA, true, "gojo_roxo_200", .08)));

    private ArvoreGojo() {
    }
}
