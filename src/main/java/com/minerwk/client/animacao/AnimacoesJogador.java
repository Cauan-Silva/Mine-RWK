package com.minerwk.client.animacao;

import com.minerwk.MineRWK;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Animações do jogador pela Player Animation Library.
 * Os arquivos ficam em {@code assets/minerwk/player_animations/} (JSON do Blockbench/GeckoLib).
 */
public final class AnimacoesJogador {
    /** Camada dos golpes dos kits. Prioridade 1500+ = jogabilidade importante (por cima de andar e emotes). */
    public static final ResourceLocation CAMADA_KIT = MineRWK.id("kit");
    private static final int PRIORIDADE_KIT = 1500;

    private AnimacoesJogador() {
    }

    public static void registrarCamada() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(CAMADA_KIT, PRIORIDADE_KIT,
                player -> {
                    PlayerAnimationController controle =
                            new PlayerAnimationController(player, (controller, state, animSetter) -> PlayState.STOP);
                    // Em primeira pessoa, os braços do modelo animado aparecem no lugar da mão normal.
                    controle.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                    controle.setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, true, true));
                    controle.setFirstPersonFollowsCamera(true);
                    return controle;
                });
    }

    /** Toca a animação {@code animacao} no jogador (em primeira pessoa aparecem os braços). Devolve false se ela não existir. */
    public static boolean tocar(AbstractClientPlayer jogador, ResourceLocation animacao) {
        if (PlayerAnimationAccess.getPlayerAnimationLayer(jogador, CAMADA_KIT) instanceof PlayerAnimationController controle) {
            return controle.triggerAnimation(animacao);
        }
        return false;
    }

    public static void parar(AbstractClientPlayer jogador) {
        if (PlayerAnimationAccess.getPlayerAnimationLayer(jogador, CAMADA_KIT) instanceof PlayerAnimationController controle) {
            controle.stop();
        }
    }
}
