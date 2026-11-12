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

    /**
     * De onde o jogador saiu para a arena. Só existe enquanto ele está lá (ver com {@code hasData}); o
     * valor padrão nunca é usado. Salvo no disco e mantido na morte.
     */
    public static final Supplier<AttachmentType<com.minerwk.arena.PontoRetorno>> RETORNO_ARENA = REGISTRO.register("retorno_arena",
            () -> AttachmentType.builder(() -> new com.minerwk.arena.PontoRetorno(
                            net.minecraft.world.level.Level.OVERWORLD.location(), 0, 0, 0, 0, 0))
                    .serialize(com.minerwk.arena.PontoRetorno.CODEC).copyOnDeath().build());

    /**
     * De onde o jogador saiu para o espaço, no R do Cid. Só existe enquanto ele está lá. Salvo no disco (o
     * servidor pode cair no meio), mas não segue na morte: quem morre renasce no lugar normal.
     */
    public static final Supplier<AttachmentType<com.minerwk.arena.PontoRetorno>> RETORNO_ESPACO = REGISTRO.register("retorno_espaco",
            () -> AttachmentType.builder(() -> new com.minerwk.arena.PontoRetorno(
                            net.minecraft.world.level.Level.OVERWORLD.location(), 0, 0, 0, 0, 0))
                    .serialize(com.minerwk.arena.PontoRetorno.CODEC).build());

    private Anexos() {
    }
}
