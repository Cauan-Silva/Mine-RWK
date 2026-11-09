package com.minerwk.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Opções do cliente ({@code config/minerwk-client.toml}, editáveis no menu de Mods). */
public final class ConfigCliente {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue INTENSIDADE_TREMOR;
    public static final ModConfigSpec.BooleanValue VFX_REDUZIDO;
    public static final ModConfigSpec.BooleanValue SFX_ANIME;
    public static final ModConfigSpec.BooleanValue MOSTRAR_HUD;

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
        SPEC = b.build();
    }

    private ConfigCliente() {
    }

    /** Lê com segurança: antes de a configuração carregar, devolve o padrão. */
    public static double intensidadeTremor() {
        return SPEC.isLoaded() ? INTENSIDADE_TREMOR.get() : INTENSIDADE_TREMOR.getDefault();
    }

    public static boolean sfxAnime() {
        return SPEC.isLoaded() ? SFX_ANIME.get() : SFX_ANIME.getDefault();
    }

    public static boolean mostrarHud() {
        return SPEC.isLoaded() ? MOSTRAR_HUD.get() : MOSTRAR_HUD.getDefault();
    }
}
