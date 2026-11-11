package com.minerwk.client.traje;

import com.minerwk.core.traje.TrajeIchigo;
import com.minerwk.core.traje.tecido.MolaManto;
import com.minerwk.core.traje.tecido.TecidoEspada;
import com.minerwk.core.traje.tecido.TecidoIchigo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * O pano dos trajes do Ichigo em cada jogador que este cliente vê: um tick por tick do jogo (20 Hz), nunca
 * por quadro. Só roda para quem está de traje, e recomeça no teleporte, na troca de mundo ou de entidade
 * (o Shunpo é um teleporte: o pano recomeça atrás do jogador em vez de esticar pelo caminho).
 */
public final class TecidoCliente {
    /** No máximo tantos jogadores com pano ao mesmo tempo. */
    private static final int MAXIMO = 64;
    /** Mais longe que isso num tick (blocos, ao quadrado) é teleporte. */
    private static final double TELEPORTE = 16;
    private static final MolaManto.Pose ZERO = new MolaManto.Pose(0, 0, 0);

    private static final class Estado {
        final TecidoIchigo roupa = new TecidoIchigo();
        final TecidoEspada espada = new TecidoEspada();
        Object mundo;
        Vec3 pos;
        float corpo, cabeca, inclinacao;
        int entidade;
        double braco;
        String traje = "";
    }

    private static final Map<UUID, Estado> ESTADOS = new HashMap<>();

    private TecidoCliente() {
    }

    /** Quanto o osso do traje do jogador gira além do repouso (radianos); zero se o osso não é de pano. */
    public static MolaManto.Pose pose(UUID jogador, String osso, float parcial) {
        Estado e = ESTADOS.get(jogador);
        if (e == null) return ZERO;
        return TecidoEspada.segmento(osso) >= 0 ? e.espada.pose(osso, parcial) : e.roupa.pose(osso, parcial);
    }

    /** Se o osso é mexido pelo pano (para o renderer não procurar os outros). */
    public static boolean dePano(String osso) {
        return osso.startsWith("bk_") || osso.startsWith("manto_manga_") || TecidoEspada.segmento(osso) >= 0;
    }

    public static void limpar() {
        ESTADOS.clear();
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ESTADOS.clear();
            return;
        }
        if (mc.isPaused()) return;
        Set<UUID> vistos = new HashSet<>();
        for (AbstractClientPlayer p : mc.level.players()) {
            String traje = CamadaTraje.trajeDe(p);
            if (!p.isAlive() || p.isSpectator() || !TrajeIchigo.eDoIchigo(traje)) continue;
            UUID id = p.getUUID();
            if (!ESTADOS.containsKey(id) && ESTADOS.size() >= MAXIMO) continue;
            vistos.add(id);
            Estado s = ESTADOS.computeIfAbsent(id, k -> new Estado());
            Vec3 pos = p.position();
            // A primeira vez de casaco é a revelação do Bankai: ele abre de uma vez e assenta.
            if (!traje.equals(s.traje)) {
                if (TrajeIchigo.BANKAI.equals(traje) && !s.traje.isEmpty()) s.roupa.impulso(.9);
                s.traje = traje;
            }
            if (s.mundo != mc.level || s.pos == null || s.entidade != p.getId() || pos.distanceToSqr(s.pos) > TELEPORTE) {
                if (s.pos != null) {
                    s.roupa.reset();
                    s.espada.reset();
                }
                s.mundo = mc.level;
                s.pos = pos;
                s.corpo = p.yBodyRot;
                s.cabeca = p.getYHeadRot();
                s.inclinacao = p.getXRot();
                s.entidade = p.getId();
                continue;
            }
            Vec3 d = pos.subtract(s.pos);
            double rad = Math.toRadians(p.yBodyRot);
            double frente = -Math.sin(rad) * d.x + Math.cos(rad) * d.z, lado = Math.cos(rad) * d.x + Math.sin(rad) * d.z;
            double giroCorpo = Math.toRadians(Mth.wrapDegrees(p.yBodyRot - s.corpo));
            double giroCabeca = Math.toRadians(Mth.wrapDegrees(p.getYHeadRot() - s.cabeca));
            double arfagem = Math.toRadians(p.getXRot() - s.inclinacao);
            double ataque = Math.sin(Math.PI * p.getAttackAnim(1));
            // O balanço dos braços do Minecraft (seno do ciclo da caminhada): as mangas seguem a mudança dele.
            double braco = Math.sin(p.walkAnimation.position() * .6662) * 1.4 * p.walkAnimation.speed(), db = braco - s.braco;
            s.braco = braco;
            // Um golpe de espada sacode as abas; agachar as deixa assentar.
            double golpe = ataque * .14, agacha = p.isCrouching() ? .07 : 0;
            s.roupa.tick(new MolaManto.Entrada(frente + agacha, lado, d.y, giroCorpo + golpe, p.onGround()), -db, db + golpe * 2);
            s.espada.tick(frente, lado, d.y, giroCabeca, arfagem, db, ataque * .6, p.onGround());
            s.pos = pos;
            s.corpo = p.yBodyRot;
            s.cabeca = p.getYHeadRot();
            s.inclinacao = p.getXRot();
        }
        ESTADOS.keySet().removeIf(id -> !vistos.contains(id));
    }
}
