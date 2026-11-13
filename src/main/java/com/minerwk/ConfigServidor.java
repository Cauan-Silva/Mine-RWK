package com.minerwk;

import com.minerwk.core.kit.Poder;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Opções do servidor ({@code saves/<mundo>/serverconfig/minerwk-server.toml}): o balanceamento dos
 * personagens e dos bosses. Valem para todos os jogadores do mundo e podem ser mudadas sem recompilar.
 */
public final class ConfigServidor {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue VIDA_EXTRA;
    public static final ModConfigSpec.DoubleValue REDUCAO_DE_DANO;
    public static final ModConfigSpec.DoubleValue MULTIPLICADOR_DOS_GOLPES;
    public static final ModConfigSpec.DoubleValue VIDA_DOS_BOSSES;
    public static final ModConfigSpec.DoubleValue DANO_DOS_BOSSES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Balanceamento: vale para quem escolheu um personagem e para os bosses do mod").push("poder");
        VIDA_EXTRA = b.comment("Vida somada à vida máxima de quem tem personagem (20 = 10 corações a mais)")
                .translation("config.minerwk.vidaExtra").defineInRange("vidaExtra", Poder.PADRAO.vidaExtra(), 0, 200);
        REDUCAO_DE_DANO = b.comment("Parte do dano recebido que é cortada (0,5 = metade). Não vale para /kill e o vazio")
                .translation("config.minerwk.reducaoDeDano").defineInRange("reducaoDeDano", Poder.PADRAO.reducaoDeDano(), 0, .95);
        MULTIPLICADOR_DOS_GOLPES = b.comment("Multiplica o dano de todos os golpes dos personagens (não o do clique)")
                .translation("config.minerwk.multiplicadorDosGolpes")
                .defineInRange("multiplicadorDosGolpes", Poder.PADRAO.multiplicadorDosGolpes(), .1, 20);
        VIDA_DOS_BOSSES = b.comment("Multiplica a vida dos bosses (Toji, Zangetsu interior, Ragnarok)")
                .translation("config.minerwk.vidaDosBosses").defineInRange("vidaDosBosses", Poder.PADRAO.vidaDosBosses(), .1, 20);
        DANO_DOS_BOSSES = b.comment("Multiplica o dano dos bosses (a redução de dano do jogador vale por cima)")
                .translation("config.minerwk.danoDosBosses").defineInRange("danoDosBosses", Poder.PADRAO.danoDosBosses(), .1, 20);
        b.pop();
        SPEC = b.build();
    }

    private ConfigServidor() {
    }

    /** Os valores atuais; antes de a config do mundo carregar (ou num teste sem mundo), o padrão do spec. */
    public static Poder poder() {
        if (!SPEC.isLoaded()) return Poder.PADRAO;
        return new Poder(VIDA_EXTRA.get(), REDUCAO_DE_DANO.get(), MULTIPLICADOR_DOS_GOLPES.get(), VIDA_DOS_BOSSES.get(),
                DANO_DOS_BOSSES.get());
    }
}
