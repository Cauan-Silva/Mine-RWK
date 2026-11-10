package com.minerwk.rede;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Tecla;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → o próprio jogador: estado do kit para o HUD e para a tela de escolha.
 *
 * @param personagem  id do personagem, vazio se ainda não escolheu
 * @param habilidades uma por tecla, com custo e recarga
 * @param emUso       id da habilidade em carga ou disparo, vazio se nenhuma
 * @param escolhiveis só preenchido sem personagem: nome, obra e golpes de cada personagem
 * @param nivel       nível de maestria no personagem (0 sem personagem)
 * @param xpNoNivel   XP já ganho dentro do nível
 * @param xpProximo   XP que o nível pede ao todo; 0 no nível máximo
 */
public record SincronizarKit(String personagem, String nomeRecurso, int corRecurso, double recurso, double maximo,
                             List<HabilidadeHud> habilidades, String emUso, List<PersonagemEscolha> escolhiveis,
                             int nivel, int xpNoNivel, int xpProximo)
        implements CustomPacketPayload {
    public static final Type<SincronizarKit> TIPO = new Type<>(MineRWK.id("sincronizar_kit"));

    public static final SincronizarKit VAZIO = new SincronizarKit("", "", 0, 0, 0, List.of(), "", List.of(), 0, 0, 0);

    /** @param prontaNoTick tick do jogo em que a recarga acaba @param duracao duração total dessa recarga */
    public record HabilidadeHud(Tecla tecla, String id, double custo, long prontaNoTick, int duracao) {
        static final StreamCodec<ByteBuf, HabilidadeHud> CODEC = StreamCodec.composite(
                UsarTecla.TECLA, HabilidadeHud::tecla,
                ByteBufCodecs.STRING_UTF8, HabilidadeHud::id,
                ByteBufCodecs.DOUBLE, HabilidadeHud::custo,
                ByteBufCodecs.VAR_LONG, HabilidadeHud::prontaNoTick,
                ByteBufCodecs.VAR_INT, HabilidadeHud::duracao,
                HabilidadeHud::new);
    }

    private static final StreamCodec<ByteBuf, List<HabilidadeHud>> HABILIDADES =
            HabilidadeHud.CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<PersonagemEscolha>> ESCOLHIVEIS =
            PersonagemEscolha.CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<ByteBuf, SincronizarKit> CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, p.personagem);
                ByteBufCodecs.STRING_UTF8.encode(buf, p.nomeRecurso);
                ByteBufCodecs.INT.encode(buf, p.corRecurso);
                ByteBufCodecs.DOUBLE.encode(buf, p.recurso);
                ByteBufCodecs.DOUBLE.encode(buf, p.maximo);
                HABILIDADES.encode(buf, p.habilidades);
                ByteBufCodecs.STRING_UTF8.encode(buf, p.emUso);
                ESCOLHIVEIS.encode(buf, p.escolhiveis);
                ByteBufCodecs.VAR_INT.encode(buf, p.nivel);
                ByteBufCodecs.VAR_INT.encode(buf, p.xpNoNivel);
                ByteBufCodecs.VAR_INT.encode(buf, p.xpProximo);
            },
            buf -> new SincronizarKit(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    HABILIDADES.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ESCOLHIVEIS.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<SincronizarKit> type() {
        return TIPO;
    }
}
