package com.minerwk.rede;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Vetor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente (só para quem usou o golpe): começa a cutscene {@code id} a partir dos olhos
 * ({@code origem}) e do olhar ({@code direcao}) no tick {@code inicio} do servidor.
 */
public record Cutscene(String id, int dono, Vetor origem, Vetor direcao, long inicio) implements CustomPacketPayload {
    public static final Type<Cutscene> TIPO = new Type<>(MineRWK.id("cutscene"));

    public static final StreamCodec<ByteBuf, Cutscene> CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, p.id);
                ByteBufCodecs.VAR_INT.encode(buf, p.dono);
                MostrarVisual.VETOR.encode(buf, p.origem);
                MostrarVisual.VETOR.encode(buf, p.direcao);
                ByteBufCodecs.VAR_LONG.encode(buf, p.inicio);
            },
            buf -> new Cutscene(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    MostrarVisual.VETOR.decode(buf),
                    MostrarVisual.VETOR.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)));

    @Override
    public Type<Cutscene> type() {
        return TIPO;
    }
}
