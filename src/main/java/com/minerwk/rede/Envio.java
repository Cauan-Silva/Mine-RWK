package com.minerwk.rede;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

/** Envio para clientes que aceitam o canal (cliente sem o mod ou jogador falso de GameTest não recebem). */
public final class Envio {
    private Envio() {
    }

    public static void para(ServerPlayer jogador, CustomPacketPayload pacote) {
        if (jogador.connection != null && jogador.connection.hasChannel(pacote)) {
            jogador.connection.send(pacote);
        }
    }

    /** Para o próprio jogador e para quem está vendo ele. */
    public static void paraQuemVe(ServerPlayer jogador, CustomPacketPayload pacote) {
        para(jogador, pacote);
        if (jogador.level() instanceof ServerLevel nivel) {
            for (ServerPlayer outro : nivel.getChunkSource().chunkMap.getPlayers(jogador.chunkPosition(), false)) {
                if (outro != jogador) para(outro, pacote);
            }
        }
    }
}
