package com.minerwk.rede;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Vetor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: começa um visual de golpe. O cliente desenha sozinho a partir daqui
 * (trajetórias com as mesmas contas do servidor), sem receber posição a cada tick.
 *
 * @param dono    id da entidade do jogador que usou o golpe
 * @param tipo    id do visual (ex.: {@code gojo_azul}, {@code gojo_roxo_carga})
 * @param inicio  tick de jogo do servidor em que começou
 */
public record MostrarVisual(int dono, String tipo, Vetor origem, Vetor direcao, float forca, int duracao, long inicio)
        implements CustomPacketPayload {
    public static final Type<MostrarVisual> TIPO = new Type<>(MineRWK.id("mostrar_visual"));

    static final StreamCodec<ByteBuf, Vetor> VETOR = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vetor::x,
            ByteBufCodecs.DOUBLE, Vetor::y,
            ByteBufCodecs.DOUBLE, Vetor::z,
            Vetor::new);

    public static final StreamCodec<ByteBuf, MostrarVisual> CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.dono);
                ByteBufCodecs.STRING_UTF8.encode(buf, p.tipo);
                VETOR.encode(buf, p.origem);
                VETOR.encode(buf, p.direcao);
                ByteBufCodecs.FLOAT.encode(buf, p.forca);
                ByteBufCodecs.VAR_INT.encode(buf, p.duracao);
                ByteBufCodecs.VAR_LONG.encode(buf, p.inicio);
            },
            buf -> new MostrarVisual(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    VETOR.decode(buf),
                    VETOR.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)));

    @Override
    public Type<MostrarVisual> type() {
        return TIPO;
    }
}
