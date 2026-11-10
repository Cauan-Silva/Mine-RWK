package com.minerwk.registro;

import com.minerwk.MineRWK;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sons do mod. Os arquivos ficam em {@code assets/minerwk/sounds/} e são listados no {@code sounds.json}. */
public final class Sons {
    public static final DeferredRegister<SoundEvent> REGISTRO = DeferredRegister.create(Registries.SOUND_EVENT, MineRWK.MOD_ID);

    /** Som de teste da prova de conceito (Kenney Sci-Fi Sounds, CC0). */
    public static final DeferredHolder<SoundEvent, SoundEvent> TESTE =
            REGISTRO.register("teste", () -> SoundEvent.createVariableRangeEvent(MineRWK.id("teste")));

    private static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> DO_TOJI = new java.util.HashMap<>();

    static {
        // Sons do Toji, gerados por ferramentas/gerar_sfx_toji.py; tocados pelo servidor.
        for (String id : com.minerwk.core.boss.SonsToji.TODOS) {
            DO_TOJI.put(id, REGISTRO.register(id, () -> SoundEvent.createVariableRangeEvent(MineRWK.id(id))));
        }
        // Sons do Gojo, gerados por ferramentas/gerar_sfx_gojo.py; os ids ficam em core (SonsGojo).
        for (String id : com.minerwk.core.personagens.gojo.SonsGojo.TODOS) {
            REGISTRO.register(id, () -> SoundEvent.createVariableRangeEvent(MineRWK.id(id)));
        }
    }

    /** O som do Toji com esse id de {@code SonsToji}. */
    public static SoundEvent doToji(String id) {
        return DO_TOJI.get(id).get();
    }

    private Sons() {
    }
}
