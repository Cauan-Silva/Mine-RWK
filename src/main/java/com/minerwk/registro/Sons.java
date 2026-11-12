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
    private static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> DO_RAGNAROK = new java.util.HashMap<>();

    static {
        // Sons do Toji, gerados por ferramentas/gerar_sfx_toji.py; tocados pelo servidor.
        for (String id : com.minerwk.core.boss.SonsToji.TODOS) {
            DO_TOJI.put(id, REGISTRO.register(id, () -> SoundEvent.createVariableRangeEvent(MineRWK.id(id))));
        }
        // Sons do Ragnarok, gerados por ferramentas/gerar_sfx_ragnarok.py; tocados pelo servidor.
        for (String id : com.minerwk.core.boss.SonsRagnarok.TODOS) {
            DO_RAGNAROK.put(id, REGISTRO.register(id, () -> SoundEvent.createVariableRangeEvent(MineRWK.id(id))));
        }
        // Sons do Gojo, gerados por ferramentas/gerar_sfx_gojo.py; os ids ficam em core (SonsGojo).
        for (String id : com.minerwk.core.personagens.gojo.SonsGojo.TODOS) {
            REGISTRO.register(id, () -> SoundEvent.createVariableRangeEvent(MineRWK.id(id)));
        }
        // Sons do Ichigo, gerados por ferramentas/gerar_sfx_ichigo.py; os ids ficam em core (SonsIchigo).
        for (String id : com.minerwk.core.personagens.ichigo.SonsIchigo.TODOS) {
            REGISTRO.register(id, () -> SoundEvent.createVariableRangeEvent(MineRWK.id(id)));
        }
        // Sons do Cid, gerados por ferramentas/gerar_sfx_cid.py; os ids ficam em core (SonsCid).
        for (String id : com.minerwk.core.personagens.cid.SonsCid.TODOS) {
            REGISTRO.register(id, () -> SoundEvent.createVariableRangeEvent(MineRWK.id(id)));
        }
    }

    /** O som do Toji com esse id de {@code SonsToji}. */
    public static SoundEvent doToji(String id) {
        return DO_TOJI.get(id).get();
    }

    /** O som do Ragnarok com esse id de {@code SonsRagnarok}. */
    public static SoundEvent doRagnarok(String id) {
        return DO_RAGNAROK.get(id).get();
    }

    private Sons() {
    }
}
