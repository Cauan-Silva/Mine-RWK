package com.minerwk.client.vfx;

import com.minerwk.client.camera.TremorCamera;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.vfx.ImpactoGojo;
import com.minerwk.rede.Tremer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** A câmera de quem vê reage aos momentos fortes do Gojo: tremor, clarão na tela e soco de FOV. */
public final class ImpactoCamera {
    private record Ativo(ImpactoGojo.Reacao reacao, double fator, long inicio) {
        double idade(long agora, float parcial) {
            return agora - inicio + parcial;
        }

        boolean acabou(long agora) {
            return agora - inicio > Math.max(reacao.ticksClarao(), reacao.ticksFov()) + 1;
        }
    }

    private static final List<Ativo> ATIVOS = new ArrayList<>();
    private static final int CARGA_200 = 80;

    private ImpactoCamera() {
    }

    /** Um momento do Gojo começou em {@code ponto} (um visual novo ou a explosão do Vermelho). */
    public static void momento(String tipo, Vetor ponto) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ImpactoGojo.de(tipo).ifPresent(r -> {
            Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
            double fator = ImpactoGojo.queda(cam.distanceTo(new Vec3(ponto.x(), ponto.y(), ponto.z())), r.raio());
            if (fator <= 0) return;
            TremorCamera.receber(new Tremer((float) (r.tremor() * fator), r.ticksTremor()));
            ATIVOS.add(new Ativo(r, fator, mc.level.getGameTime()));
        });
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ATIVOS.clear();
            return;
        }
        long agora = mc.level.getGameTime();
        ATIVOS.removeIf(a -> a.acabou(agora));
        if (agora % 4 != 0) return;
        // A carga do Roxo 200% faz o chão tremer cada vez mais.
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (!v.tipo.equals("gojo_roxo_200_carga") || v.fimEm >= 0) continue;
            Entity dono = mc.level.getEntity(v.dono);
            if (dono == null) continue;
            double fator = ImpactoGojo.queda(cam.distanceTo(dono.position()), ImpactoGojo.RAIO_CARGA_200);
            if (fator > 0) {
                float forca = (float) (ImpactoGojo.tremorCarga200(v.idade(agora, 0), CARGA_200) * fator);
                TremorCamera.receber(new Tremer(forca, 5));
            }
        }
    }

    public static void limpar() {
        ATIVOS.clear();
    }

    public static void aoCalcularFov(ViewportEvent.ComputeFov evento) {
        Minecraft mc = Minecraft.getInstance();
        // Só o FOV do mundo; o da mão fica parado.
        if (mc.level == null || ATIVOS.isEmpty() || !evento.usedConfiguredFov()) return;
        long agora = mc.level.getGameTime();
        float parcial = (float) evento.getPartialTick();
        double escala = mc.options.fovEffectScale().get();
        double mult = 1;
        for (Ativo a : ATIVOS) mult *= 1 + (ImpactoGojo.fov(a.reacao, a.idade(agora, parcial)) - 1) * a.fator * escala;
        evento.setFOV(evento.getFOV() * mult);
    }

    /** Camada de GUI: o clarão pinta a tela inteira e some rápido. */
    public static void desenharClarao(GuiGraphics g, DeltaTracker tempo) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ATIVOS.isEmpty()) return;
        long agora = mc.level.getGameTime();
        float parcial = tempo.getGameTimeDeltaPartialTick(false);
        for (Ativo a : ATIVOS) {
            float alpha = ImpactoGojo.alphaClarao(a.reacao, a.idade(agora, parcial)) * (float) a.fator;
            if (alpha <= .004f) continue;
            int argb = ((int) (alpha * 255) << 24) | (a.reacao.corClarao() & 0xFFFFFF);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), argb);
        }
    }
}
