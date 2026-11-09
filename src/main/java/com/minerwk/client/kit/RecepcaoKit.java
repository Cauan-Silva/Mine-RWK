package com.minerwk.client.kit;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.client.animacao.AnimacoesJogador;
import com.minerwk.client.camera.TremorCamera;
import com.minerwk.client.vfx.EfeitosVisuais;
import com.minerwk.rede.Pacotes;
import com.minerwk.rede.SincronizarKit;
import com.minerwk.rede.TocarApresentacao;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;

/** Recebe no cliente o estado do kit e os pedidos de apresentação (animação, efeito e som). */
public final class RecepcaoKit {
    private static SincronizarKit ultimo = SincronizarKit.VAZIO;
    private static boolean telaJaAberta;

    private RecepcaoKit() {
    }

    public static void ligar() {
        Pacotes.aoSincronizar = RecepcaoKit::aoSincronizar;
        Pacotes.aoTocar = RecepcaoKit::aoTocar;
        Pacotes.aoTremer = TremorCamera::receber;
    }

    /** O último estado recebido (o HUD do 1.4 lê daqui). */
    public static SincronizarKit estado() {
        return ultimo;
    }

    public static boolean semPersonagem() {
        return ultimo.personagem().isEmpty();
    }

    public static void abrirEscolha() {
        if (semPersonagem() && !ultimo.escolhiveis().isEmpty()) {
            Minecraft.getInstance().setScreen(new TelaEscolha(ultimo.escolhiveis()));
        }
    }

    /** Ao sair do mundo, para a tela abrir de novo na próxima entrada. */
    public static void esquecer() {
        ultimo = SincronizarKit.VAZIO;
        telaJaAberta = false;
    }

    private static void aoSincronizar(SincronizarKit pacote) {
        ultimo = pacote;
        if (semPersonagem() && !telaJaAberta) {
            telaJaAberta = true;
            abrirEscolha();
        }
    }

    private static void aoTocar(TocarApresentacao p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entidade()) instanceof AbstractClientPlayer jogador)) return;
        if (!p.animacao().isEmpty()) AnimacoesJogador.tocar(jogador, MineRWK.id(p.animacao()));
        if (!p.efeito().isEmpty()) EfeitosVisuais.naEntidade(MineRWK.id(p.efeito()), mc.level, jogador);
        if (!p.som().isEmpty() && ConfigCliente.sfxAnime()) {
            BuiltInRegistries.SOUND_EVENT.getOptional(MineRWK.id(p.som())).ifPresent(som ->
                    mc.level.playLocalSound(jogador.getX(), jogador.getY(), jogador.getZ(), som,
                            SoundSource.PLAYERS, 1f, 1f, false));
        }
    }
}
