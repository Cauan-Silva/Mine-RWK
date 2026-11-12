package com.minerwk.core.traje;

/**
 * O traje do Cid, sem classes do Minecraft. O modelo sai de {@code ferramentas/gerar_traje_cid.py}: o sobretudo
 * do Shadow com o capuz, os olhos vermelhos (que brilham pela máscara de brilho da textura) e a espada de slime
 * no osso {@link TrajeIchigo#OSSO_ARMA}, preso ao braço direito. A barra do sobretudo usa os ossos de pano do
 * casaco do Bankai ({@code bk_capa_*}), que o {@code TecidoIchigo} balança.
 */
public final class TrajeCid {
    /** O sobretudo do Shadow. */
    public static final String SHADOW = "cid";

    private TrajeCid() {
    }

    /** Se o traje tem ossos de pano para o tecido balançar: os do Ichigo e o do Shadow. */
    public static boolean temPano(String traje) {
        return SHADOW.equals(traje) || TrajeIchigo.eDoIchigo(traje);
    }
}
