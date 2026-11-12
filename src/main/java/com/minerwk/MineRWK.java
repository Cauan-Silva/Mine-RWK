package com.minerwk;

import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.teste.PersonagemTeste;
import com.minerwk.kit.Anexos;
import com.minerwk.kit.ComandoTrocar;
import com.minerwk.core.personagens.gojo.Gojo;
import com.minerwk.kit.ApagadorServidor;
import com.minerwk.kit.ArremessosServidor;
import com.minerwk.kit.BarreirasServidor;
import com.minerwk.kit.ProtecoesServidor;
import com.minerwk.kit.ServicoKits;
import com.minerwk.kit.SuspensosServidor;
import com.minerwk.registro.Blocos;
import com.minerwk.registro.Efeitos;
import com.minerwk.registro.Regras;
import com.minerwk.rede.Pacotes;
import com.minerwk.registro.Sons;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

/** Ponto de entrada do Mine-RWK (lado comum: cliente e servidor). */
@Mod(MineRWK.MOD_ID)
public final class MineRWK {
    public static final String MOD_ID = "minerwk";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MineRWK(IEventBus modBus) {
        Sons.REGISTRO.register(modBus);
        Anexos.REGISTRO.register(modBus);
        Efeitos.REGISTRO.register(modBus);
        Blocos.REGISTRO.register(modBus);
        com.minerwk.registro.Itens.REGISTRO.register(modBus);
        com.minerwk.registro.Entidades.REGISTRO.register(modBus);
        modBus.addListener(com.minerwk.registro.Entidades::atributos);
        modBus.addListener(com.minerwk.registro.Itens::naAbaCriativa);
        Regras.registrar();
        Personagens.registrar(PersonagemTeste.criar());
        Personagens.registrar(Gojo.criar());
        Personagens.registrar(com.minerwk.core.personagens.ichigo.Ichigo.criar());
        Personagens.registrar(com.minerwk.core.personagens.cid.Cid.criar());
        ServicoKits.registrarEventos();
        com.minerwk.kit.ArmaServidor.registrarEventos();
        ProtecoesServidor.registrarEventos();
        com.minerwk.kit.HollowServidor.registrarEventos();
        com.minerwk.kit.CidServidor.registrarEventos();
        com.minerwk.kit.EspacoServidor.registrarEventos();
        com.minerwk.kit.ChamarizServidor.registrarEventos();
        // Depois das proteções: um golpe que o Infinito parou não chega à luta do Toji.
        com.minerwk.boss.LutasToji.registrarEventos();
        com.minerwk.boss.LutasZangetsu.registrarEventos();
        ArremessosServidor.registrarEventos();
        BarreirasServidor.registrarEventos();
        SuspensosServidor.registrarEventos();
        ApagadorServidor.registrarEventos();
        com.minerwk.arena.ArenasServidor.registrarEventos();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ComandoTrocar.registrar(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ComandoTrocar.registrarRecarregar(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ComandoTrocar.registrarMaestria(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ComandoTrocar.registrarDespertar(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ComandoTrocar.registrarToji(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ComandoTrocar.registrarZangetsu(e.getDispatcher()));
        modBus.addListener(Pacotes::registrar);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Mine-RWK carregado");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
