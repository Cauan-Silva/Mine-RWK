package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: o jogador com este id de entidade escolheu este personagem (vazio = nenhum), está
 * nesta forma e com ou sem a arma na mão. Vai para o próprio jogador, para quem passa a vê-lo e para quem
 * já o vê quando algo disso muda; serve para desenhar o traje e a arma.
 */
public record PersonagemDe(int entidade, String personagem, String forma, boolean semArma) implements CustomPacketPayload {
    public PersonagemDe(int entidade, String personagem) {
        this(entidade, personagem, com.minerwk.core.kit.Forma.BASE, false);
    }

    public static final Type<PersonagemDe> TIPO = new Type<>(MineRWK.id("personagem_de"));
    public static final StreamCodec<ByteBuf, PersonagemDe> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PersonagemDe::entidade,
            ByteBufCodecs.STRING_UTF8, PersonagemDe::personagem,
            ByteBufCodecs.STRING_UTF8, PersonagemDe::forma,
            ByteBufCodecs.BOOL, PersonagemDe::semArma,
            PersonagemDe::new);

    @Override
    public Type<PersonagemDe> type() {
        return TIPO;
    }
}
