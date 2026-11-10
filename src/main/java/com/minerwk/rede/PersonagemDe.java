package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: o jogador com este id de entidade escolheu este personagem (vazio = nenhum). Vai para
 * quem passa a ver o jogador e para quem já o vê quando ele troca; serve para desenhar o traje nos outros.
 */
public record PersonagemDe(int entidade, String personagem) implements CustomPacketPayload {
    public static final Type<PersonagemDe> TIPO = new Type<>(MineRWK.id("personagem_de"));
    public static final StreamCodec<ByteBuf, PersonagemDe> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PersonagemDe::entidade,
            ByteBufCodecs.STRING_UTF8, PersonagemDe::personagem,
            PersonagemDe::new);

    @Override
    public Type<PersonagemDe> type() {
        return TIPO;
    }
}
