package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: a tela escurece por {@code escurecendo} ticks, fica preta por {@code preto} e clareia
 * por {@code clareando}. Um pedido novo substitui o que estiver em andamento (a ida para a arena pede o
 * preto antes de saber quanto a montagem demora, e outro ao chegar).
 */
public record EscurecerTela(int escurecendo, int preto, int clareando) implements CustomPacketPayload {
    public static final Type<EscurecerTela> TIPO = new Type<>(MineRWK.id("escurecer_tela"));

    public static final StreamCodec<ByteBuf, EscurecerTela> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EscurecerTela::escurecendo,
            ByteBufCodecs.VAR_INT, EscurecerTela::preto,
            ByteBufCodecs.VAR_INT, EscurecerTela::clareando,
            EscurecerTela::new);

    @Override
    public Type<EscurecerTela> type() {
        return TIPO;
    }
}
