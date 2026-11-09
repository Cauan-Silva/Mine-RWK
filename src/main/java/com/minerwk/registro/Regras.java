package com.minerwk.registro;

import net.minecraft.world.level.GameRules;

/** Gamerules do mod. Registradas ao carregar a classe ({@link #registrar()} no construtor do mod). */
public final class Regras {
    /** Se golpes como o Roxo apagam blocos. Padrão: ligada. */
    public static final GameRules.Key<GameRules.BooleanValue> APAGAR_BLOCOS =
            GameRules.register("minerwkApagarBlocos", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    private Regras() {
    }

    /** Só força o carregamento da classe. */
    public static void registrar() {
    }
}
