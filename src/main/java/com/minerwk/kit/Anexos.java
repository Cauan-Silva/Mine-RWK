package com.minerwk.kit;

import com.minerwk.MineRWK;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data Attachments do mod. */
public final class Anexos {
    public static final DeferredRegister<AttachmentType<?>> REGISTRO =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MineRWK.MOD_ID);

    /** Personagem, recurso e recargas do jogador. Salvo no disco e mantido na morte. */
    public static final Supplier<AttachmentType<DadosKit>> DADOS_KIT = REGISTRO.register("dados_kit",
            () -> AttachmentType.builder(() -> new DadosKit()).serialize(DadosKit.CODEC).copyOnDeath().build());

    private Anexos() {
    }
}
