package com.minerwk.rede;

import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Um personagem como a tela de escolha precisa: nomes e golpes por tecla (os textos vêm das traduções). */
public record PersonagemEscolha(String id, String nome, String obra, List<GolpeEscolha> golpes) {

    public record GolpeEscolha(Tecla tecla, String id) {
        static final StreamCodec<ByteBuf, GolpeEscolha> CODEC = StreamCodec.composite(
                UsarTecla.TECLA, GolpeEscolha::tecla,
                ByteBufCodecs.STRING_UTF8, GolpeEscolha::id,
                GolpeEscolha::new);
    }

    static final StreamCodec<ByteBuf, PersonagemEscolha> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PersonagemEscolha::id,
            ByteBufCodecs.STRING_UTF8, PersonagemEscolha::nome,
            ByteBufCodecs.STRING_UTF8, PersonagemEscolha::obra,
            GolpeEscolha.CODEC.apply(ByteBufCodecs.list()), PersonagemEscolha::golpes,
            PersonagemEscolha::new);

    public static PersonagemEscolha de(Personagem p) {
        List<GolpeEscolha> golpes = new ArrayList<>();
        for (Tecla t : Tecla.values()) {
            p.naTecla(t).ifPresent(h -> golpes.add(new GolpeEscolha(t, h.id())));
        }
        return new PersonagemEscolha(p.id(), p.nome(), p.obra(), golpes);
    }
}
