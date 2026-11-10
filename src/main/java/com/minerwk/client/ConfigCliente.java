package com.minerwk.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Opções do cliente ({@code config/minerwk-client.toml}, editáveis no menu de Mods). */
public final class ConfigCliente {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue INTENSIDADE_TREMOR;
    public static final ModConfigSpec.BooleanValue VFX_REDUZIDO;
    public static final ModConfigSpec.BooleanValue SFX_ANIME;
    public static final ModConfigSpec.BooleanValue MOSTRAR_HUD;
    public static final ModConfigSpec.BooleanValue CUTSCENES;
    public static final ModConfigSpec.BooleanValue ARMADURA_SOBRE_TRAJE;
    public static final ModConfigSpec.BooleanValue OLHOS_BRILHANDO;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        INTENSIDADE_TREMOR = b.comment("Intensidade do tremor de tela (0 desliga)")
                .translation("config.minerwk.intensidadeTremor").defineInRange("intensidadeTremor", 1.0, 0.0, 1.0);
        VFX_REDUZIDO = b.comment("Efeitos visuais reduzidos (vale quando os efeitos dos personagens existirem)")
                .translation("config.minerwk.vfxReduzido").define("vfxReduzido", false);
        SFX_ANIME = b.comment("Sons das habilidades")
                .translation("config.minerwk.sfxAnime").define("sfxAnime", true);
        MOSTRAR_HUD = b.comment("Mostrar o HUD de recurso e recargas")
                .translation("config.minerwk.mostrarHud").define("mostrarHud", true);
        CUTSCENES = b.comment("Cutscenes das ultimates (desligadas, aparece só o encantamento na tela)")
                .translation("config.minerwk.cutscenes").define("cutscenes", true);
        ARMADURA_SOBRE_TRAJE = b.comment("Desenhar a armadura vestida por cima do traje do personagem")
                .translation("config.minerwk.armaduraSobreTraje").define("armaduraSobreTraje", false);
        OLHOS_BRILHANDO = b.comment("Olhos brilhando quando a venda do Gojo sai (desligue se a sua skin tiver o rosto em outra posição)")
                .translation("config.minerwk.olhosBrilhando").define("olhosBrilhando", true);
        SPEC = b.build();
    }

    private ConfigCliente() {
    }

    /** Lê com segurança: antes de a configuração carregar, devolve o padrão. */
    public static double intensidadeTremor() {
        return SPEC.isLoaded() ? INTENSIDADE_TREMOR.get() : INTENSIDADE_TREMOR.getDefault();
    }

    public static boolean vfxReduzido() {
        return SPEC.isLoaded() ? VFX_REDUZIDO.get() : VFX_REDUZIDO.getDefault();
    }

    public static boolean sfxAnime() {
        return SPEC.isLoaded() ? SFX_ANIME.get() : SFX_ANIME.getDefault();
    }

    public static boolean armaduraSobreTraje() {
        return SPEC.isLoaded() ? ARMADURA_SOBRE_TRAJE.get() : ARMADURA_SOBRE_TRAJE.getDefault();
    }

    public static boolean olhosBrilhando() {
        return SPEC.isLoaded() ? OLHOS_BRILHANDO.get() : OLHOS_BRILHANDO.getDefault();
    }

    public static boolean cutscenes() {
        return SPEC.isLoaded() ? CUTSCENES.get() : CUTSCENES.getDefault();
    }

    public static boolean mostrarHud() {
        return SPEC.isLoaded() ? MOSTRAR_HUD.get() : MOSTRAR_HUD.getDefault();
    }
}
