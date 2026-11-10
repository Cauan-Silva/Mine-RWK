package com.minerwk.boss;

import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.personagens.gojo.ArvoreGojo;
import com.minerwk.core.personagens.gojo.Gojo;
import com.minerwk.kit.DadosKit;
import com.minerwk.kit.ServicoKits;
import com.minerwk.registro.Entidades;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** As lutas contra o Toji em andamento: quem tem um Toji vivo, quem está caído, e a invocação. */
public final class LutasToji {
    /** O Toji de cada jogador que invocou um. */
    private static final Map<UUID, TojiEntity> VIVOS = new HashMap<>();
    /** Quem está no chão esperando o despertar: não leva dano. */
    static final Set<UUID> CAIDOS = new HashSet<>();

    private LutasToji() {
    }

    /** Registrar depois das proteções do kit: um golpe que o Infinito parou não chega aqui. */
    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(LutasToji::aoReceberDano);
        // Os mapas são estáticos: no jogo de um jogador só eles sobreviveriam ao fechar e abrir o mundo.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> {
            VIVOS.clear();
            CAIDOS.clear();
            ServicoKits.esquecerDespertosNaLuta();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (CAIDOS.remove(e.getEntity().getUUID())) e.getEntity().setForcedPose(null);
        });
    }

    static void registrar(TojiEntity toji) {
        VIVOS.put(toji.invocadorId(), toji);
    }

    static void esquecer(TojiEntity toji) {
        if (toji.invocadorId() != null) VIVOS.remove(toji.invocadorId(), toji);
    }

    /** O Toji vivo que esse jogador invocou. */
    public static Optional<TojiEntity> de(UUID jogador) {
        TojiEntity toji = VIVOS.get(jogador);
        if (toji != null && (toji.isRemoved() || !toji.isAlive())) {
            VIVOS.remove(jogador);
            toji = null;
        }
        return Optional.ofNullable(toji);
    }

    public static boolean caido(Player jogador) {
        return CAIDOS.contains(jogador.getUUID());
    }

    private static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer vitima)) return;
        // /kill e o vazio continuam matando.
        if (evento.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (CAIDOS.contains(vitima.getUUID())) {
            evento.setCanceled(true);
            return;
        }
        // O golpe do Toji que mataria quem o invocou vira o despertar.
        if (evento.getSource().getEntity() instanceof TojiEntity toji && toji.podeDerrubar(vitima)
                && TojiBoss.golpeMortal(vitima.getHealth(), evento.getAmount())) {
            evento.setCanceled(true);
            toji.derrubar(vitima);
        }
    }

    /**
     * Invoca o Toji a 8 blocos de {@code jogador}, na direção do olhar. Com {@code exigir}, vale a regra do
     * Contrato (ser Gojo e ter o nó Despertar); sem, só não pode haver outro Toji dele (comando de teste).
     */
    public static TojiBoss.Invocacao invocar(ServerPlayer jogador, boolean exigir) {
        DadosKit dados = ServicoKits.dados(jogador);
        boolean temNo = dados.temPersonagem() && dados.maestriaAtual().nos.getOrDefault(ArvoreGojo.DESPERTAR, 0) > 0;
        boolean temToji = de(jogador.getUUID()).isPresent();
        TojiBoss.Invocacao resposta = exigir ? TojiBoss.podeInvocar(dados.personagem, Gojo.ID, temNo, temToji)
                : temToji ? TojiBoss.Invocacao.JA_EXISTE : TojiBoss.Invocacao.OK;
        if (resposta != TojiBoss.Invocacao.OK) return resposta;
        ServerLevel nivel = jogador.serverLevel();
        TojiEntity toji = Entidades.TOJI.get().create(nivel);
        if (toji == null) return TojiBoss.Invocacao.JA_EXISTE;
        Vec3 olhar = jogador.getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0, olhar.z);
        frente = frente.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, jogador.getYRot()) : frente.normalize();
        Vec3 ponto = chao(nivel, toji, jogador.position().add(frente.scale(TojiBoss.DISTANCIA_DA_INVOCACAO)),
                jogador.position());
        // De frente para quem invocou.
        float yaw = (float) (Math.toDegrees(Math.atan2(-frente.z, -frente.x)) - 90);
        toji.moveTo(ponto.x, ponto.y, ponto.z, yaw, 0);
        toji.yBodyRot = yaw;
        toji.yHeadRot = yaw;
        toji.comecarLuta(jogador);
        if (!nivel.addFreshEntity(toji)) {
            esquecer(toji);
            return TojiBoss.Invocacao.JA_EXISTE;
        }
        return TojiBoss.Invocacao.OK;
    }

    /** O chão livre mais perto de {@code ponto} (3 blocos acima até 6 abaixo); sem chão, fica onde o jogador está. */
    private static Vec3 chao(ServerLevel nivel, TojiEntity toji, Vec3 ponto, Vec3 reserva) {
        BlockPos base = BlockPos.containing(ponto);
        for (int dy = 3; dy >= -6; dy--) {
            BlockPos p = base.above(dy);
            double x = p.getX() + .5, z = p.getZ() + .5;
            if (nivel.getBlockState(p.below()).blocksMotion()
                    && nivel.noCollision(toji.getType().getDimensions().makeBoundingBox(x, p.getY(), z))) {
                return new Vec3(x, p.getY(), z);
            }
        }
        return reserva;
    }
}
