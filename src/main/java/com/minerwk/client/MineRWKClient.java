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
        NeoForge.EVENT_BUS.addListener(HudKit::aoTick);
        NeoForge.EVENT_BUS.addListener(TremorCamera::aoTick);
        NeoForge.EVENT_BUS.addListener(TremorCamera::aoCalcularCamera);
        modBus.addListener(this::clientSetup);
        modBus.addListener(TeclasKit::registrar);
        RecepcaoKit.ligar();
        modBus.addListener(Pincel::registrarShaders);
        NeoForge.EVENT_BUS.addListener(RenderVisuaisGojo::aoTick);
        NeoForge.EVENT_BUS.addListener(RenderVisuaisGojo::aoRenderizar);
        NeoForge.EVENT_BUS.addListener(RenderVisuaisGojo::aoCorDaNeblina);
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
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> CutsceneCliente.encerrar());
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(CutsceneCliente.CAMADA, CutsceneCliente::desenhar));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS,
                com.minerwk.MineRWK.id("clarao_gojo"), ImpactoCamera::desenharClarao));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> VisuaisAtivos.limpar());
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
