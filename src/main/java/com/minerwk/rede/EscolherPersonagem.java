package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: escolha feita na tela. O servidor só aceita se o jogador ainda não tem personagem. */
public record EscolherPersonagem(String id) implements CustomPacketPayload {
    public static final Type<EscolherPersonagem> TIPO = new Type<>(MineRWK.id("escolher_personagem"));

    public static final StreamCodec<ByteBuf, EscolherPersonagem> CODEC =
            ByteBufCodecs.stringUtf8(64).map(EscolherPersonagem::new, EscolherPersonagem::id);

    @Override
    public Type<EscolherPersonagem> type() {
        return TIPO;
    }
}
