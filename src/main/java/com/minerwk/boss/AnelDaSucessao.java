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
 * Anel da Sucessão: na obra, a chave que abre a Rosa Negra. Leva aos arredores de Oriana quem é o Cid e
 * comprou o nó Despertar, para enfrentar o Ragnarok. Só é gasto quando a entrada começa; senão, uma
 * mensagem diz o que falta.
 */
public final class AnelDaSucessao extends Item {
    public AnelDaSucessao() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer servidor)) return InteractionResultHolder.consume(pilha);
        TojiBoss.Invocacao resposta = LutasRagnarok.invocar(servidor, true);
        if (resposta != TojiBoss.Invocacao.OK) {
            if (servidor.connection != null) servidor.displayClientMessage(Component.translatable(chave(resposta)), true);
            return InteractionResultHolder.fail(pilha);
        }
        // No criativo o anel não é gasto: não há o que devolver.
        if (!jogador.hasInfiniteMaterials()) LutasRagnarok.pagou(servidor);
        pilha.consume(1, jogador);
        jogador.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.success(pilha);
    }

    /** A mensagem de cada resposta. */
    public static String chave(TojiBoss.Invocacao resposta) {
        return switch (resposta) {
            case OUTRO_PERSONAGEM -> "mensagem.minerwk.ragnarok.outro_personagem";
            case SEM_NO -> "mensagem.minerwk.ragnarok.sem_no";
            case JA_EXISTE -> "mensagem.minerwk.ragnarok.ja_existe";
            case NA_ARENA -> "mensagem.minerwk.ragnarok.na_arena";
            case OK -> "mensagem.minerwk.ragnarok.invocado";
        };
    }
}
