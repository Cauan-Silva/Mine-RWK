package com.minerwk.rede;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Tecla;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: o jogador apertou ou soltou a tecla de uma habilidade. */
public record UsarTecla(Tecla tecla, boolean apertou) implements CustomPacketPayload {
    public static final Type<UsarTecla> TIPO = new Type<>(MineRWK.id("usar_tecla"));

    static final StreamCodec<ByteBuf, Tecla> TECLA = ByteBufCodecs.idMapper(i -> Tecla.values()[i], Tecla::ordinal);

    public static final StreamCodec<ByteBuf, UsarTecla> CODEC = StreamCodec.composite(
            TECLA, UsarTecla::tecla,
            ByteBufCodecs.BOOL, UsarTecla::apertou,
            UsarTecla::new);

    @Override
    public Type<UsarTecla> type() {
        return TIPO;
    }
}
