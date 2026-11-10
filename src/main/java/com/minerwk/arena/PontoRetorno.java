package com.minerwk.arena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** De onde o jogador saiu para a arena: para lá ele volta quando a luta acaba. */
public record PontoRetorno(ResourceLocation dimensao, double x, double y, double z, float yaw, float pitch) {
    public static final Codec<PontoRetorno> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("dimensao").forGetter(PontoRetorno::dimensao),
            Codec.DOUBLE.fieldOf("x").forGetter(PontoRetorno::x),
            Codec.DOUBLE.fieldOf("y").forGetter(PontoRetorno::y),
            Codec.DOUBLE.fieldOf("z").forGetter(PontoRetorno::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(PontoRetorno::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(PontoRetorno::pitch)
    ).apply(i, PontoRetorno::new));

    public static PontoRetorno de(ServerPlayer jogador) {
        return new PontoRetorno(jogador.level().dimension().location(), jogador.getX(), jogador.getY(), jogador.getZ(),
                jogador.getYRot(), jogador.getXRot());
    }
}
