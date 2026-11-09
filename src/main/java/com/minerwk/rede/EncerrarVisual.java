package com.minerwk.rede;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Vetor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: um visual acabou antes da hora (carga interrompida, Infinito sem energia)
 * ou terminou num ponto que o cliente não sabe (onde a esfera do Vermelho bateu).
 */
public record EncerrarVisual(int dono, String tipo, Vetor ponto) implements CustomPacketPayload {
    public static final Type<EncerrarVisual> TIPO = new Type<>(MineRWK.id("encerrar_visual"));

    public static final StreamCodec<ByteBuf, EncerrarVisual> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EncerrarVisual::dono,
            ByteBufCodecs.STRING_UTF8, EncerrarVisual::tipo,
            MostrarVisual.VETOR, EncerrarVisual::ponto,
            EncerrarVisual::new);

    @Override
    public Type<EncerrarVisual> type() {
        return TIPO;
    }
}
