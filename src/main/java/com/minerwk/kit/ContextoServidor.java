package com.minerwk.kit;

import com.minerwk.core.kit.Contexto;
import net.minecraft.server.level.ServerPlayer;

/** O {@link Contexto} do núcleo, executado no servidor em nome de um jogador. */
public final class ContextoServidor implements Contexto {
    private final ServerPlayer jogador;

    public ContextoServidor(ServerPlayer jogador) {
        this.jogador = jogador;
    }

    public ServerPlayer jogador() {
        return jogador;
    }
}
