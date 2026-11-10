package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: pedido para subir um nível de um nó da árvore. O servidor confere tudo antes de aceitar. */
public record ComprarNo(String no) implements CustomPacketPayload {
    public static final Type<ComprarNo> TIPO = new Type<>(MineRWK.id("comprar_no"));

    public static final StreamCodec<ByteBuf, ComprarNo> CODEC =
            ByteBufCodecs.stringUtf8(64).map(ComprarNo::new, ComprarNo::no);

    @Override
    public Type<ComprarNo> type() {
        return TIPO;
    }
}
