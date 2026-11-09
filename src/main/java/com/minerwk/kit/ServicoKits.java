package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Liga o núcleo dos kits aos jogadores no servidor: carrega, roda o tick e salva. */
public final class ServicoKits {
    private static final double LIMIAR_PARADO = 0.01;
    private static final int SALVAR_A_CADA = 20;

    private static final Map<UUID, KitDoJogador> KITS = new HashMap<>();
    private static final Set<UUID> APANHOU = new HashSet<>();
    private static final Set<UUID> CAUSOU_DANO = new HashSet<>();

    private ServicoKits() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoTick);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoDano);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoEntrar);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoSair);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoClonar);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoMudarDeDimensao);
    }

    public static DadosKit dados(Player jogador) {
        return jogador.getData(Anexos.DADOS_KIT);
    }

    /** O kit do jogador, criado a partir dos dados salvos na primeira chamada. Vazio se ele não escolheu. */
    public static Optional<KitDoJogador> kit(ServerPlayer jogador) {
        KitDoJogador kit = KITS.get(jogador.getUUID());
        if (kit != null) return Optional.of(kit);
        DadosKit dados = dados(jogador);
        Optional<Personagem> personagem = Personagens.porId(dados.personagem);
        if (personagem.isEmpty()) return Optional.empty();
        kit = new KitDoJogador(personagem.get());
        kit.recurso().carregar(dados.recurso);
        kit.recargas().carregar(dados.recargas, jogador.level().getGameTime());
        KITS.put(jogador.getUUID(), kit);
        return Optional.of(kit);
    }

    /** Define (ou troca) o personagem: recurso novo (cheio) e recargas zeradas. */
    public static void definirPersonagem(ServerPlayer jogador, Personagem personagem) {
        KitDoJogador antigo = KITS.remove(jogador.getUUID());
        if (antigo != null) antigo.descartar();
        KitDoJogador novo = new KitDoJogador(personagem);
        KITS.put(jogador.getUUID(), novo);
        salvar(jogador, novo);
        Sincronizacao.enviar(jogador);
    }

    /** Só jogador vivo e fora do modo espectador usa habilidades. */
    static boolean podeAgir(ServerPlayer jogador) {
        return !jogador.isDeadOrDying() && !jogador.isSpectator();
    }

    /** Tecla apertada ou solta no cliente. Sem personagem, nada acontece. */
    public static void usarTecla(ServerPlayer jogador, Tecla tecla, boolean apertou) {
        kit(jogador).ifPresent(k -> {
            if (apertou && podeAgir(jogador) && !ProtecoesServidor.paralisado(jogador)) k.apertar(tecla, jogador.level().getGameTime());
            else k.soltar(tecla);
        });
    }

    /**
     * Escolha feita no menu. Só vale se o jogador ainda não tem personagem e o id está entre os escolhíveis.
     * Devolve se aceitou.
     */
    public static boolean escolher(ServerPlayer jogador, String id) {
        if (dados(jogador).temPersonagem()) {
            MineRWK.LOGGER.debug("{} tentou escolher {} mas já tem personagem", jogador.getName().getString(), id);
            return false;
        }
        Optional<Personagem> personagem = Personagens.escolhiveis().stream().filter(p -> p.id().equals(id)).findFirst();
        if (personagem.isEmpty()) {
            MineRWK.LOGGER.debug("{} tentou escolher personagem inválido: {}", jogador.getName().getString(), id);
            return false;
        }
        definirPersonagem(jogador, personagem.get());
        return true;
    }

    private static void salvar(ServerPlayer jogador, KitDoJogador kit) {
        jogador.setData(Anexos.DADOS_KIT,
                new DadosKit(kit.personagem().id(), kit.recurso().salvar(), kit.recargas().salvar()));
    }

    private static void aoTick(PlayerTickEvent.Post evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) tickJogador(jogador);
    }

    /** Um tick do kit do jogador (separado do evento para os GameTests chamarem direto). */
    static void tickJogador(ServerPlayer jogador) {
        UUID id = jogador.getUUID();
        boolean apanhou = APANHOU.remove(id);
        boolean causou = CAUSOU_DANO.remove(id);
        Optional<KitDoJogador> kit = kit(jogador);
        if (kit.isEmpty()) return;
        if (!podeAgir(jogador)) {
            // Morto ou espectador: o golpe em andamento some sem cobrar nem punir, e nada mais roda.
            kit.get().descartar(new ContextoServidor(jogador));
            return;
        }
        long agora = jogador.level().getGameTime();
        ContextoServidor contexto = new ContextoServidor(jogador);
        // Paralisado (Vazio Infinito): a carga em andamento cai, como num atordoamento.
        if (ProtecoesServidor.paralisado(jogador)) kit.get().atordoar(agora, contexto);
        boolean parado = jogador.getDeltaMovement().horizontalDistanceSqr() < LIMIAR_PARADO * LIMIAR_PARADO;
        kit.get().tick(agora, new EstadoDoJogador(parado, apanhou, causou), contexto);
        if (agora % SALVAR_A_CADA == 0) salvar(jogador, kit.get());
        Sincronizacao.aoTick(jogador, kit.get());
    }

    private static void aoDano(LivingDamageEvent.Post evento) {
        if (evento.getEntity() instanceof ServerPlayer vitima) {
            APANHOU.add(vitima.getUUID());
            kit(vitima).ifPresent(k -> k.levouDano(evento.getNewDamage(), vitima.level().getGameTime(),
                    new ContextoServidor(vitima)));
        }
        if (evento.getSource().getEntity() instanceof ServerPlayer atacante) CAUSOU_DANO.add(atacante.getUUID());
    }

    private static void aoEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) Sincronizacao.enviar(jogador);
    }

    private static void aoSair(PlayerEvent.PlayerLoggedOutEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador)) return;
        KitDoJogador kit = KITS.remove(jogador.getUUID());
        if (kit != null) {
            kit.descartar(new ContextoServidor(jogador));
            salvar(jogador, kit);
        }
        Sincronizacao.esquecer(jogador);
    }

    /** Os efeitos guardam coordenadas da dimensão antiga: acabam na troca (o domínio não segue para o Nether). */
    private static void aoMudarDeDimensao(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            KitDoJogador kit = KITS.get(jogador.getUUID());
            if (kit != null) kit.descartar(new ContextoServidor(jogador));
        }
    }

    /** Morte ou volta do End: o jogador vira outra entidade. Salva antes para o copyOnDeath levar os dados atuais. */
    private static void aoClonar(PlayerEvent.Clone evento) {
        if (!(evento.getOriginal() instanceof ServerPlayer antigo)) return;
        KitDoJogador kit = KITS.remove(antigo.getUUID());
        if (kit == null) return;
        kit.descartar(new ContextoServidor(antigo));
        salvar(antigo, kit);
        evento.getEntity().setData(Anexos.DADOS_KIT, dados(antigo));
    }
}
