package com.minerwk.core.personagens.gojo;

import com.minerwk.core.hud.FormaArvore;
import com.minerwk.core.hud.FormaArvore.Galho;
import com.minerwk.core.hud.FormaArvore.Selo;
import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.maestria.No;
import java.util.List;
import java.util.Map;

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

    /** Como a árvore é desenhada (spec [[2026-11-12 UI dos personagens]]). */
    public static final FormaArvore FORMA = new FormaArvore(List.of(
            Galho.antes("azul", "gojo_azul", List.of(AZUL_ALCANCE, AZUL_RECARGA), Map.of()),
            Galho.antes("infinito", "gojo_infinito", List.of(INFINITO_DURACAO, INFINITO_RECARGA), Map.of(INFINITO_DURACAO, Selo.DURACAO)),
            Galho.antes("energia", "gojo_recurso", List.of(ENERGIA_RESERVA, ENERGIA_FLUXO),
                    Map.of(ENERGIA_RESERVA, Selo.RECURSO, ENERGIA_FLUXO, Selo.RECURSO)),
            Galho.depois("vermelho", "gojo_vermelho", List.of(VERMELHO_FORCA, VERMELHO_CARGA), Map.of()),
            Galho.depois("roxo", "gojo_roxo", List.of(ROXO_FORCA, ROXO_RECARGA, ROXO_MAXIMO_RECARGA), Map.of()),
            Galho.depois("dominio", "gojo_vazio_infinito", List.of(TECNICA_REVERSA, DOMINIO_DURACAO), Map.of(DOMINIO_DURACAO, Selo.DURACAO))));

    private ArvoreGojo() {
    }
}
