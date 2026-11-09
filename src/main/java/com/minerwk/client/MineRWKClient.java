package com.minerwk.client;

import com.minerwk.MineRWK;
import com.minerwk.client.animacao.AnimacoesJogador;
import com.minerwk.client.comando.ComandosTeste;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Ponto de entrada do lado do cliente: animações do jogador e VFX. */
@Mod(value = MineRWK.MOD_ID, dist = Dist.CLIENT)
public final class MineRWKClient {

    public MineRWKClient(IEventBus modBus) {
        modBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> ComandosTeste.registrar(e.getDispatcher()));
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // A Player Animation Library exige o registro da camada dentro do enqueueWork no NeoForge.
        event.enqueueWork(AnimacoesJogador::registrarCamada);
        MineRWK.LOGGER.info("Mine-RWK: cliente pronto (Photon2 e Player Animation Library)");
    }
}
