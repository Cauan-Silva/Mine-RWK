package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: em qual cenário de arena o jogador está. A dimensão das arenas é uma só, então o
 * cliente precisa disto para desenhar o céu de cada uma. Sai ao chegar à arena e ao voltar dela
 * ({@link #NENHUM}).
 */
public record CenarioArena(String cenario) implements CustomPacketPayload {
    public static final String ESCOLA = "escola";
    public static final String INTERIOR = "interior";
    /** O mundo interior na fase 2 do Zangetsu: o céu escurece e chove (só no cliente de quem luta). */
    public static final String INTERIOR_CHUVA = "interior_chuva";
    public static final String ORIANA = "oriana";
    public static final String NENHUM = "";

    public static final Type<CenarioArena> TIPO = new Type<>(MineRWK.id("cenario_arena"));

    public static final StreamCodec<ByteBuf, CenarioArena> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CenarioArena::cenario,
            CenarioArena::new);

    @Override
    public Type<CenarioArena> type() {
        return TIPO;
    }
}
