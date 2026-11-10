package com.minerwk.client.vfx;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.SonsGojo;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;

/**
 * O som preso ao visual: toca no ponto do visual quando ele começa, para junto se o visual for encerrado
 * antes da hora (carga solta cedo ou interrompida) e, para alguns, toca um pouco antes de o visual acabar
 * (o domínio rachando). Qual som vai com qual visual fica no núcleo ({@link SonsGojo}).
 */
public final class SonsVisuais {
    /** Sons que param junto com o visual, enquanto ele existe. */
    private static final Map<VisuaisAtivos.Visual, SoundInstance> TOCANDO = new IdentityHashMap<>();

    private SonsVisuais() {
    }

    /** Toca {@code id} (um som do mod) em {@code onde}; devolve o som, para poder pará-lo, ou null. */
    public static SoundInstance tocar(String id, Vetor onde) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ConfigCliente.sfxAnime()) return null;
        return BuiltInRegistries.SOUND_EVENT.getOptional(MineRWK.id(id)).map(evento -> {
            SoundInstance som = new SimpleSoundInstance(evento, SoundSource.PLAYERS, 1f, 1f,
                    mc.level.random, onde.x(), onde.y(), onde.z());
            mc.getSoundManager().play(som);
            return som;
        }).orElse(null);
    }

    public static void parar(SoundInstance som) {
        if (som != null) Minecraft.getInstance().getSoundManager().stop(som);
    }

    /** Um visual começou: toca o som dele, se tiver. */
    static void aoComecar(VisuaisAtivos.Visual v) {
        SonsGojo.doVisual(v.tipo).ifPresent(id -> {
            SoundInstance som = tocar(id, v.origem);
            if (som != null && SonsGojo.cortaComOVisual(v.tipo)) TOCANDO.put(v, som);
        });
    }

    /** A cada tick do cliente: corta o som dos visuais encerrados e toca o som de fim dos que estão acabando. */
    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        for (Iterator<Map.Entry<VisuaisAtivos.Visual, SoundInstance>> it = TOCANDO.entrySet().iterator(); it.hasNext();) {
            Map.Entry<VisuaisAtivos.Visual, SoundInstance> e = it.next();
            VisuaisAtivos.Visual v = e.getKey();
            if (v.fimEm >= 0) {
                parar(e.getValue());
                it.remove();
            } else if (v.acabou(agora) || !VisuaisAtivos.ativos().contains(v)) {
                it.remove(); // chegou ao fim natural: o som acaba sozinho
            }
        }
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.fimEm >= 0 || v.somDeFimTocado) continue;
            SonsGojo.antesDoFim(v.tipo).ifPresent(fim -> {
                if (!fim.naJanela(agora - v.inicio, v.duracao)) return;
                v.somDeFimTocado = true;
                tocar(fim.som(), v.origem);
            });
        }
    }

    static void limpar() {
        TOCANDO.values().forEach(SonsVisuais::parar);
        TOCANDO.clear();
    }
}
