package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: o golpe deste jogador acertou alguém; a tela dele trava um instante. */
public record Acertou(float forca) implements CustomPacketPayload {
    public static final Type<Acertou> TIPO = new Type<>(MineRWK.id("acertou"));

    public static final StreamCodec<ByteBuf, Acertou> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Acertou::forca,
            Acertou::new);

    @Override
    public Type<Acertou> type() {
        return TIPO;
    }
}
