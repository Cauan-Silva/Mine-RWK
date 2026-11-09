package com.minerwk.client.camera;

import com.minerwk.client.ConfigCliente;
import com.minerwk.core.camera.Tremor;
import com.minerwk.rede.Tremer;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Aplica o {@link Tremor} na câmera do jogador. */
public final class TremorCamera {
    private static final Tremor TREMOR = new Tremor();

    private TremorCamera() {
    }

    public static void receber(Tremer pacote) {
        if (Minecraft.getInstance().level != null) TREMOR.adicionar(pacote.forca(), pacote.duracao());
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        if (Minecraft.getInstance().level == null) TREMOR.limpar();
        else TREMOR.tick();
    }

    public static void aoCalcularCamera(ViewportEvent.ComputeCameraAngles evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || TREMOR.ativos() == 0) return;
        float tempo = (mc.level.getGameTime() + (float) evento.getPartialTick()) / 20f;
        Tremor.Deslocamento d = TREMOR.deslocamento(tempo, (float) ConfigCliente.intensidadeTremor());
        evento.setYaw(evento.getYaw() + d.guinada());
        evento.setPitch(evento.getPitch() + d.inclinacao());
        evento.setRoll(evento.getRoll() + d.rotacao());
    }
}
