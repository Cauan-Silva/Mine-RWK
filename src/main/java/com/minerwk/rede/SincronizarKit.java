package com.minerwk.rede;

import com.minerwk.MineRWK;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → o próprio jogador: estado do kit para o HUD e para a tela de escolha.
 *
 * @param personagem id do personagem, vazio se ainda não escolheu
 * @param recargas   habilidade → tick do jogo em que fica pronta
 * @param escolhiveis só preenchido sem personagem: {@code "id|nome|obra"}
 */
public record SincronizarKit(String personagem, double recurso, double maximo, Map<String, Long> recargas,
                             List<String> escolhiveis) implements CustomPacketPayload {
    public static final Type<SincronizarKit> TIPO = new Type<>(MineRWK.id("sincronizar_kit"));

    public static final StreamCodec<ByteBuf, SincronizarKit> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SincronizarKit::personagem,
            ByteBufCodecs.DOUBLE, SincronizarKit::recurso,
            ByteBufCodecs.DOUBLE, SincronizarKit::maximo,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), SincronizarKit::recargas,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SincronizarKit::escolhiveis,
            SincronizarKit::new);

    @Override
    public Type<SincronizarKit> type() {
        return TIPO;
    }
}
