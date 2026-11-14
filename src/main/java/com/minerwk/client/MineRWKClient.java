package com.minerwk.client;

import com.minerwk.MineRWK;
import com.minerwk.client.animacao.AnimacoesJogador;
import com.minerwk.client.comando.ComandosTeste;
import com.minerwk.client.camera.TremorCamera;
import com.minerwk.client.kit.HudKit;
import com.minerwk.client.kit.RecepcaoKit;
import com.minerwk.client.camera.CutsceneCliente;
import com.minerwk.client.vfx.ImpactoCamera;
import com.minerwk.client.vfx.Pincel;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import com.minerwk.client.vfx.RenderVisuaisGojo;
import com.minerwk.client.vfx.VisuaisAtivos;
import com.minerwk.client.kit.TeclasKit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Ponto de entrada do lado do cliente: animações do jogador, VFX, teclas e tela de escolha. */
@Mod(value = MineRWK.MOD_ID, dist = Dist.CLIENT)
public final class MineRWKClient {

    public MineRWKClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ConfigCliente.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(HudKit::registrar);
        modBus.addListener(com.minerwk.client.kit.Molduras::aoRegistrarRecarga);
        NeoForge.EVENT_BUS.addListener(HudKit::aoTick);
        NeoForge.EVENT_BUS.addListener(TremorCamera::aoTick);
        NeoForge.EVENT_BUS.addListener(TremorCamera::aoCalcularCamera);
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) ->
                {
                    e.registerEntityRenderer(com.minerwk.registro.Entidades.TOJI.get(), com.minerwk.client.boss.RenderToji::new);
                    e.registerEntityRenderer(com.minerwk.registro.Entidades.ZANGETSU.get(), com.minerwk.client.boss.RenderZangetsu::new);
                    e.registerEntityRenderer(com.minerwk.registro.Entidades.RAGNAROK.get(), com.minerwk.client.boss.RenderRagnarok::new);
                });
        modBus.addListener(this::clientSetup);
        modBus.addListener(TeclasKit::registrar);
        RecepcaoKit.ligar();
        modBus.addListener(Pincel::registrarShaders);
        modBus.addListener(com.minerwk.client.traje.CamadaTraje::aoRegistrarCamadas);
        modBus.addListener(com.minerwk.client.traje.CamadaTraje::aoAdicionarCamadas);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.traje.CamadaTraje::aoDesenharBraco);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.traje.CamadaTraje::antesDoJogador);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.traje.TecidoCliente::aoTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> com.minerwk.client.traje.TecidoCliente.limpar());
        com.minerwk.rede.Pacotes.aoPersonagemDe = com.minerwk.client.traje.PersonagensVistos::receber;
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> com.minerwk.client.traje.PersonagensVistos.limpar());
        NeoForge.EVENT_BUS.addListener(RenderVisuaisGojo::aoTick);
        NeoForge.EVENT_BUS.addListener(RenderVisuaisGojo::aoRenderizar);
        NeoForge.EVENT_BUS.addListener(RenderVisuaisGojo::aoCorDaNeblina);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.vfx.ImpactFrame::aoRenderizar);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.vfx.CeuRoxo::aoRenderizar);
        NeoForge.EVENT_BUS.addListener(ImpactoCamera::aoCalcularFov);
        NeoForge.EVENT_BUS.addListener(ImpactoCamera::aoCalcularAngulos);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoTick);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.vfx.DemoVfx::aoTick);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.kit.MovimentoCliente::aoTickJogador);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoTickAntes);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoInteragir);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoComecarQuadro);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoCalcularAngulos);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoCalcularFov);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoEntrada);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoCamadaGui);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoMao);
        NeoForge.EVENT_BUS.addListener(CutsceneCliente::aoNome);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> CutsceneCliente.encerrar());
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(CutsceneCliente.CAMADA, CutsceneCliente::desenhar));
        // O espaço do R do Cid: o céu, sem neblina, e os clarões que cobrem as trocas de dimensão (por baixo das barras).
        modBus.addListener(com.minerwk.client.vfx.EspacoCliente::registrar);
        modBus.addListener(com.minerwk.client.arena.CeuArena::registrar);
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerBelow(CutsceneCliente.CAMADA,
                com.minerwk.client.vfx.EspacoCliente.CAMADA, com.minerwk.client.vfx.EspacoCliente::desenhar));
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.vfx.EspacoCliente::aoAbrirTela);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.vfx.EspacoCliente::aoNeblina);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> com.minerwk.client.vfx.EspacoCliente.limpar());
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS,
                com.minerwk.MineRWK.id("clarao_gojo"), ImpactoCamera::desenharClarao));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> VisuaisAtivos.limpar());
        // Registrada depois da cutscene: a tela preta da arena fica por cima dela também.
        com.minerwk.rede.Pacotes.aoEscurecer = com.minerwk.client.arena.Escurecer::receber;
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(com.minerwk.client.arena.Escurecer.CAMADA,
                com.minerwk.client.arena.Escurecer::desenhar));
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.arena.Escurecer::aoTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> com.minerwk.client.arena.Escurecer.limpar());
        // O horizonte de cada arena: o servidor diz em qual cenário o jogador está.
        com.minerwk.rede.Pacotes.aoCenarioArena = com.minerwk.client.arena.CenarioCliente::receber;
        // A noite de Oriana antes de tudo o que pinta o céu e a névoa (prioridade alta): o horizonte, o céu escuro
        // do Roxo Máximo e do Mugetsu e a névoa do domínio caem por cima dela.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, false,
                net.neoforged.neoforge.client.event.RenderLevelStageEvent.class, com.minerwk.client.arena.NoiteOriana::aoRenderizar);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, false,
                net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor.class, com.minerwk.client.arena.NoiteOriana::aoCorDaNevoa);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.arena.FumacaOriana::aoTick);
        NeoForge.EVENT_BUS.addListener(com.minerwk.client.arena.HorizonteCliente::aoRenderizar);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> com.minerwk.client.arena.CenarioCliente.limpar());
        NeoForge.EVENT_BUS.addListener(TeclasKit::aoTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> RecepcaoKit.esquecer());
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> ComandosTeste.registrar(e.getDispatcher()));
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // A Player Animation Library exige o registro da camada dentro do enqueueWork no NeoForge.
        event.enqueueWork(AnimacoesJogador::registrarCamada);
        MineRWK.LOGGER.info("Mine-RWK: cliente pronto (Photon2 e Player Animation Library)");
    }
}
