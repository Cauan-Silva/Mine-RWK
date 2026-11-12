package com.minerwk.registro;

import com.minerwk.MineRWK;
import com.minerwk.boss.TojiEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entidades do mod. */
public final class Entidades {
    public static final DeferredRegister<EntityType<?>> REGISTRO = DeferredRegister.create(Registries.ENTITY_TYPE, MineRWK.MOD_ID);

    /** O Toji como boss, Nêmesis do Gojo. */
    public static final DeferredHolder<EntityType<?>, EntityType<TojiEntity>> TOJI = REGISTRO.register("toji",
            () -> EntityType.Builder.<TojiEntity>of(TojiEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("toji"));

    /** O Zangetsu interior, Nêmesis do Ichigo. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.minerwk.boss.ZangetsuEntity>> ZANGETSU =
            REGISTRO.register("zangetsu", () -> EntityType.Builder
                    .<com.minerwk.boss.ZangetsuEntity>of(com.minerwk.boss.ZangetsuEntity::new, MobCategory.MONSTER)
                    .sized(.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("zangetsu"));

    private Entidades() {
    }

    public static void atributos(EntityAttributeCreationEvent evento) {
        evento.put(TOJI.get(), TojiEntity.atributos().build());
        evento.put(ZANGETSU.get(), com.minerwk.boss.ZangetsuEntity.atributos().build());
    }
}
