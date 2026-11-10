package com.minerwk.boss;

import com.minerwk.core.boss.TojiBoss;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * Contrato do Matador de Feiticeiros: invoca o Toji para quem é Gojo e comprou o nó Despertar. Só é gasto
 * quando o Toji aparece; senão, uma mensagem diz o que falta.
 */
public final class ContratoToji extends Item {
    public ContratoToji() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer servidor)) return InteractionResultHolder.consume(pilha);
        TojiBoss.Invocacao resposta = LutasToji.invocar(servidor, true);
        if (resposta != TojiBoss.Invocacao.OK) {
            if (servidor.connection != null) servidor.displayClientMessage(Component.translatable(chave(resposta)), true);
            return InteractionResultHolder.fail(pilha);
        }
        pilha.consume(1, jogador);
        jogador.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.success(pilha);
    }

    /** A mensagem de cada recusa. */
    public static String chave(TojiBoss.Invocacao resposta) {
        return switch (resposta) {
            case OUTRO_PERSONAGEM -> "mensagem.minerwk.toji.outro_personagem";
            case SEM_NO -> "mensagem.minerwk.toji.sem_no";
            case JA_EXISTE -> "mensagem.minerwk.toji.ja_existe";
            case OK -> "mensagem.minerwk.toji.invocado";
        };
    }
}
