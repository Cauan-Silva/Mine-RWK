package com.minerwk.core.kit;

/** Acesso dos testes de outros pacotes à limpeza do registro. */
public final class PersonagensTestes {
    private PersonagensTestes() {
    }

    public static void limpar() {
        Personagens.limparParaTestes();
    }
}
