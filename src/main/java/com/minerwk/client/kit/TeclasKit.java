package com.minerwk.client.kit;

import com.minerwk.core.kit.Tecla;
import com.minerwk.rede.UsarTecla;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Teclas das habilidades (configuráveis em Controles). O cliente só avisa apertou/soltou; o servidor decide. */
public final class TeclasKit {
    private static final String CATEGORIA = "key.categories.minerwk";
    private static final Map<Tecla, KeyMapping> TECLAS = new EnumMap<>(Tecla.class);
    private static final Map<Tecla, Boolean> APERTADA = new EnumMap<>(Tecla.class);
    private static final KeyMapping ESCOLHER = new KeyMapping("key.minerwk.escolher", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORIA);
    private static final KeyMapping ARVORE = new KeyMapping("key.minerwk.arvore", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORIA);

    static {
        int[] padrao = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_R};
        for (Tecla t : Tecla.values()) {
            TECLAS.put(t, new KeyMapping("key.minerwk." + t.name().toLowerCase(), KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, padrao[t.ordinal()], CATEGORIA));
            APERTADA.put(t, false);
        }
    }

    private TeclasKit() {
    }

    public static void registrar(RegisterKeyMappingsEvent evento) {
        TECLAS.values().forEach(evento::register);
        evento.register(ESCOLHER);
        evento.register(ARVORE);
    }

    /** Se a tecla apertada (dentro de uma tela) é a que abre a árvore. */
    static boolean eDaArvore(int tecla, int scan) {
        return ARVORE.matches(tecla, scan);
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        while (ESCOLHER.consumeClick()) RecepcaoKit.abrirEscolha();
        if (RecepcaoKit.semPersonagem()) return;
        while (ARVORE.consumeClick()) TelaArvore.abrir();
        for (Tecla t : Tecla.values()) {
            boolean agora = TECLAS.get(t).isDown();
            if (agora != APERTADA.get(t)) {
                APERTADA.put(t, agora);
                PacketDistributor.sendToServer(new UsarTecla(t, agora));
            }
        }
    }
}
