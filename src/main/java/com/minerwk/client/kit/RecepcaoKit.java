package com.minerwk.client.kit;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.client.animacao.AnimacoesJogador;
import com.minerwk.client.camera.TremorCamera;
import com.minerwk.client.vfx.EfeitosVisuais;
import com.minerwk.client.vfx.VisuaisAtivos;
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
        Pacotes.aoMostrarVisual = VisuaisAtivos::receber;
        Pacotes.aoEncerrarVisual = p -> {
            VisuaisAtivos.encerrar(p);
            com.minerwk.client.camera.CutsceneCliente.aoEncerrarVisual(p);
        };
        Pacotes.aoCutscene = com.minerwk.client.camera.CutsceneCliente::receber;
        Pacotes.aoAcertar = com.minerwk.client.vfx.ImpactoCamera::acertou;
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
        SONS_DE_CARGA.clear();
    }

    private static void aoSincronizar(SincronizarKit pacote) {
        ultimo = pacote;
        if (semPersonagem() && !telaJaAberta) {
            telaJaAberta = true;
            abrirEscolha();
        }
    }

    /** O som da carga em andamento de cada jogador (id da entidade), para cortar se for interrompida. */
    private static final java.util.Map<Integer, net.minecraft.client.resources.sounds.SoundInstance> SONS_DE_CARGA =
            new java.util.HashMap<>();

    private static void aoTocar(TocarApresentacao p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entidade()) instanceof AbstractClientPlayer jogador)) return;
        if (TocarApresentacao.PARAR.equals(p.animacao())) {
            AnimacoesJogador.parar(jogador);
            // Golpe interrompido: o som que veio com a carga dele para junto.
            com.minerwk.client.vfx.SonsVisuais.parar(SONS_DE_CARGA.remove(p.entidade()));
        } else if (!p.animacao().isEmpty()) AnimacoesJogador.tocar(jogador, MineRWK.id(p.animacao()));
        if (!p.efeito().isEmpty()) EfeitosVisuais.naEntidade(MineRWK.id(p.efeito()), mc.level, jogador);
        if (!p.som().isEmpty()) {
            var som = com.minerwk.client.vfx.SonsVisuais.tocar(p.som(),
                    new com.minerwk.core.kit.Vetor(jogador.getX(), jogador.getY(), jogador.getZ()));
            // Som que veio junto com uma animação é o da carga: fica guardado para poder ser cortado.
            if (som != null && !p.animacao().isEmpty()) SONS_DE_CARGA.put(p.entidade(), som);
        }
    }
}
