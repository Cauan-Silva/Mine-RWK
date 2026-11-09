package com.minerwk.registro;

import com.minerwk.MineRWK;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Efeitos de poção do mod. */
public final class Efeitos {
    public static final DeferredRegister<MobEffect> REGISTRO = DeferredRegister.create(Registries.MOB_EFFECT, MineRWK.MOD_ID);

    /**
     * Paralisia (Vazio Infinito): sem andar e sem pular. Atacar e usar golpes é bloqueado em
     * {@link com.minerwk.kit.ProtecoesServidor}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> PARALISIA = REGISTRO.register("paralisia", () -> new Paralisia()
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, MineRWK.id("paralisia_velocidade"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.FLYING_SPEED, MineRWK.id("paralisia_voo"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, MineRWK.id("paralisia_pulo"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private static final class Paralisia extends MobEffect {
        Paralisia() {
            super(MobEffectCategory.HARMFUL, 0xE8E8FF);
        }
    }

    private Efeitos() {
    }
}
