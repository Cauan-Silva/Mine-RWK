package com.minerwk.kit;

import com.minerwk.core.kit.Arremessos;
import com.minerwk.core.kit.Vetor;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Aplica os {@link Arremessos} nas entidades: congela, lança e gira. Só na thread do servidor.
 * O congelamento segura a posição a cada tick em vez de mexer em NoAI/NoGravity, que iriam para o
 * disco: um mob salvo no meio do golpe voltaria parado para sempre.
 */
public final class ArremessosServidor {
    /** Graus por tick do giro no ar. */
    private static final float GIRO = 36;

    private static final class Ativo {
        final Arremessos.Estado estado;
        final Vec3 parado;

        Ativo(Arremessos.Estado estado, LivingEntity alvo) {
            this.estado = estado;
            this.parado = alvo.position();
        }
    }

    private static final Map<LivingEntity, Ativo> ATIVOS = new IdentityHashMap<>();

    private ArremessosServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ATIVOS.clear());
    }

    public static boolean emArremesso(LivingEntity alvo) {
        return ATIVOS.containsKey(alvo);
    }

    /** Começa o arremesso; quem já está sendo arremessado é ignorado. */
    static void iniciar(LivingEntity alvo, Vetor velocidade, int congelar) {
        if (ATIVOS.containsKey(alvo) || !alvo.isAlive()) return;
        Ativo a = new Ativo(new Arremessos.Estado(velocidade, congelar), alvo);
        ATIVOS.put(alvo, a);
        if (congelar > 0) congelar(alvo, a);
    }

    /** Segura o alvo onde foi atingido, sem velocidade nem caminho. */
    private static void congelar(LivingEntity alvo, Ativo a) {
        if (alvo instanceof Mob m) m.getNavigation().stop();
        alvo.setDeltaMovement(Vec3.ZERO);
        alvo.teleportTo(a.parado.x, a.parado.y, a.parado.z);
        alvo.hurtMarked = true;
        alvo.hasImpulse = true;
    }

    static void tick() {
        Iterator<Map.Entry<LivingEntity, Ativo>> it = ATIVOS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Ativo> e = it.next();
            LivingEntity alvo = e.getKey();
            Ativo a = e.getValue();
            if (!alvo.isAlive() || alvo.isRemoved()) {
                it.remove();
                continue;
            }
            switch (a.estado.tick(alvo.onGround())) {
                case CONGELAR -> congelar(alvo, a);
                case LANCAR -> {
                    Vetor v = a.estado.velocidade();
                    alvo.setDeltaMovement(v.x(), v.y(), v.z());
                    alvo.hurtMarked = true;
                    alvo.hasImpulse = true;
                }
                case GIRAR -> {
                    // Jogadores não giram: a câmera viraria junto.
                    if (!(alvo instanceof Player)) {
                        float giro = alvo.getYRot() + GIRO;
                        alvo.setYRot(giro);
                        alvo.setYBodyRot(giro);
                        alvo.setYHeadRot(giro);
                    }
                }
                case FIM -> it.remove();
            }
        }
    }
}
