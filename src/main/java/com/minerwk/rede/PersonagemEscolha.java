package com.minerwk.rede;

import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Um personagem como a tela de escolha precisa: nomes, golpes por tecla da forma base e os de cada
 * transformação (os textos vêm das traduções).
 */
public record PersonagemEscolha(String id, String nome, String obra, List<GolpeEscolha> golpes,
                                List<FormaEscolha> transformacoes) {
    public PersonagemEscolha(String id, String nome, String obra, List<GolpeEscolha> golpes) {
        this(id, nome, obra, golpes, List.of());
    }


    public record GolpeEscolha(Tecla tecla, String id) {
        static final StreamCodec<ByteBuf, GolpeEscolha> CODEC = StreamCodec.composite(
                UsarTecla.TECLA, GolpeEscolha::tecla,
                ByteBufCodecs.STRING_UTF8, GolpeEscolha::id,
                GolpeEscolha::new);
    }

    /** Os golpes de uma transformação; o nome dela vem da tradução {@code forma.minerwk.<id>}. */
    public record FormaEscolha(String id, List<GolpeEscolha> golpes) {
        static final StreamCodec<ByteBuf, FormaEscolha> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, FormaEscolha::id,
                GolpeEscolha.CODEC.apply(ByteBufCodecs.list()), FormaEscolha::golpes,
                FormaEscolha::new);
    }

    static final StreamCodec<ByteBuf, PersonagemEscolha> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PersonagemEscolha::id,
            ByteBufCodecs.STRING_UTF8, PersonagemEscolha::nome,
            ByteBufCodecs.STRING_UTF8, PersonagemEscolha::obra,
            GolpeEscolha.CODEC.apply(ByteBufCodecs.list()), PersonagemEscolha::golpes,
            FormaEscolha.CODEC.apply(ByteBufCodecs.list()), PersonagemEscolha::transformacoes,
            PersonagemEscolha::new);

    public static PersonagemEscolha de(Personagem p) {
        List<GolpeEscolha> golpes = new ArrayList<>();
        for (Tecla t : Tecla.values()) {
            p.naTecla(t).ifPresent(h -> golpes.add(new GolpeEscolha(t, h.id())));
        }
        List<FormaEscolha> formas = new ArrayList<>();
        for (com.minerwk.core.kit.Forma f : p.transformacoes()) {
            List<GolpeEscolha> daForma = new ArrayList<>();
            for (Tecla t : Tecla.values()) f.naTecla(t).ifPresent(h -> daForma.add(new GolpeEscolha(t, h.id())));
            // Forma sem golpe em tecla (a Hollow do Ichigo, que ataca sozinha) não entra na tela de escolha.
            if (!daForma.isEmpty()) formas.add(new FormaEscolha(f.id(), daForma));
        }
        return new PersonagemEscolha(p.id(), p.nome(), p.obra(), golpes, formas);
    }
}
