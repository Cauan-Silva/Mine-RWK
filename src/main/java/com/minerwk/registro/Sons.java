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

    private Sons() {
    }
}
