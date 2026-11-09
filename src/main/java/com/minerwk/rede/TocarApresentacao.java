package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → clientes próximos: tocar animação, efeito e som num jogador (ids do mod; vazio = nada). */
public record TocarApresentacao(int entidade, String animacao, String efeito, String som) implements CustomPacketPayload {
    public static final Type<TocarApresentacao> TIPO = new Type<>(MineRWK.id("tocar_apresentacao"));

    public static final StreamCodec<ByteBuf, TocarApresentacao> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TocarApresentacao::entidade,
            ByteBufCodecs.STRING_UTF8, TocarApresentacao::animacao,
            ByteBufCodecs.STRING_UTF8, TocarApresentacao::efeito,
            ByteBufCodecs.STRING_UTF8, TocarApresentacao::som,
            TocarApresentacao::new);

    @Override
    public Type<TocarApresentacao> type() {
        return TIPO;
    }
}
