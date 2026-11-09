package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: tremer a tela com esta força (já reduzida pela distância) por {@code duracao} ticks. */
public record Tremer(float forca, int duracao) implements CustomPacketPayload {
    public static final Type<Tremer> TIPO = new Type<>(MineRWK.id("tremer"));

    public static final StreamCodec<ByteBuf, Tremer> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Tremer::forca,
            ByteBufCodecs.VAR_INT, Tremer::duracao,
            Tremer::new);

    @Override
    public Type<Tremer> type() {
        return TIPO;
    }
}
