package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.maestria.Maestria;
import com.minerwk.core.maestria.Melhorias;
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
    /** O jogador cujo golpe do kit está causando dano agora (o servidor roda numa thread só). */
    private static ServerPlayer golpeDoKit;

    private ServicoKits() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoComecarAVer);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoTick);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoDano);
        NeoForge.EVENT_BUS.addListener(ServicoKits::aoMorrer);
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
        // Antes de carregar o recurso: a reserva comprada na árvore muda o máximo.
        kit.definirMelhorias(new Melhorias(personagem.get().arvore(), dados.maestriaAtual().nos));
        kit.recurso().carregar(dados.recurso);
        kit.recargas().carregar(dados.recargas, jogador.level().getGameTime());
        KITS.put(jogador.getUUID(), kit);
        return Optional.of(kit);
    }

    /** Define (ou troca) o personagem: recurso novo (cheio) e recargas zeradas. */
    public static void definirPersonagem(ServerPlayer jogador, Personagem personagem) {
        KitDoJogador antigo = KITS.remove(jogador.getUUID());
        // Com o contexto do jogador: a carga em andamento desfaz o visual e para a animação.
        if (antigo != null) antigo.descartar(new ContextoServidor(jogador));
        KitDoJogador novo = new KitDoJogador(personagem);
        KITS.put(jogador.getUUID(), novo);
        salvar(jogador, novo);
        atualizarMelhorias(jogador);
        novo.recarregar();
        Sincronizacao.enviar(jogador);
        // Quem já está vendo o jogador passa a vê-lo com o traje do personagem novo.
        com.minerwk.rede.Envio.paraQuemVe(jogador, new com.minerwk.rede.PersonagemDe(jogador.getId(), personagem.id()));
    }

    /** Um jogador passou a ver outro: recebe o personagem dele, para desenhar o traje. */
    private static void aoComecarAVer(PlayerEvent.StartTracking evento) {
        if (evento.getTarget() instanceof ServerPlayer visto && evento.getEntity() instanceof ServerPlayer quemVe) {
            String personagem = kit(visto).map(k -> k.personagem().id()).orElse("");
            com.minerwk.rede.Envio.para(quemVe, new com.minerwk.rede.PersonagemDe(visto.getId(), personagem));
        }
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
        jogador.setData(Anexos.DADOS_KIT, new DadosKit(kit.personagem().id(), kit.recurso().salvar(),
                kit.recargas().salvar(), dados(jogador).maestria));
    }

    /**
     * Roda {@code golpe} marcando o dano como vindo do kit do {@code jogador}: só esse dano (e o abate que
     * ele causar) dá XP de maestria. Um soco ou uma espada não contam.
     */
    static void comoGolpeDoKit(ServerPlayer jogador, Runnable golpe) {
        ServerPlayer antes = golpeDoKit;
        golpeDoKit = jogador;
        try {
            golpe.run();
        } finally {
            golpeDoKit = antes;
        }
    }

    private static boolean inimigo(net.minecraft.world.entity.Entity alvo) {
        return alvo instanceof net.minecraft.world.entity.monster.Enemy;
    }

    private static void aoMorrer(net.neoforged.neoforge.event.entity.living.LivingDeathEvent evento) {
        if (golpeDoKit != null && evento.getSource().getEntity() == golpeDoKit) {
            ganharXp(golpeDoKit, Maestria.xpPorAbate(inimigo(evento.getEntity())));
        }
    }

    /** Dá XP de maestria ao jogador no personagem atual; avisa e festeja se subiu de nível. */
    public static void ganharXp(ServerPlayer jogador, long ganho) {
        DadosKit dados = dados(jogador);
        if (ganho <= 0 || !dados.temPersonagem()) return;
        DadosMaestria m = dados.maestriaAtual();
        definirXp(jogador, Maestria.somar(m.xp, ganho));
    }

    /** Os nós comprados mudaram (compra, zerar ou troca de personagem): o kit passa a usar os novos. */
    public static void atualizarMelhorias(ServerPlayer jogador) {
        DadosKit dados = dados(jogador);
        kit(jogador).ifPresent(k -> k.definirMelhorias(new Melhorias(k.personagem().arvore(), dados.maestriaAtual().nos)));
    }

    /** Sobe um nível de um nó da árvore do personagem atual, se as regras deixarem. O servidor é quem decide. */
    public static Arvore.Resultado comprarNo(ServerPlayer jogador, String no) {
        DadosKit dados = dados(jogador);
        Optional<Personagem> personagem = Personagens.porId(dados.personagem);
        if (personagem.isEmpty()) return Arvore.Resultado.NO_DESCONHECIDO;
        DadosMaestria m = dados.maestriaAtual();
        Arvore.Resultado r = personagem.get().arvore().comprar(m.nos, no, Maestria.nivel(m.xp), m.despertou);
        if (r == Arvore.Resultado.OK) {
            atualizarMelhorias(jogador);
            Sincronizacao.enviar(jogador);
        }
        return r;
    }

    /**
     * Marca (ou desmarca) o jogador como desperto no personagem atual: abre os nós de depois do despertar.
     * Por enquanto só o comando de teste chama; a vitória sobre o Nêmesis vai chamar daqui.
     */
    public static void definirDespertou(ServerPlayer jogador, boolean despertou) {
        DadosKit dados = dados(jogador);
        if (!dados.temPersonagem()) return;
        dados.maestriaAtual().despertou = despertou;
        Sincronizacao.enviar(jogador);
    }

    /** Define o XP total no personagem atual (também usado pelos comandos de teste). */
    public static void definirXp(ServerPlayer jogador, long xp) {
        DadosKit dados = dados(jogador);
        if (!dados.temPersonagem()) return;
        DadosMaestria m = dados.maestriaAtual();
        int antes = Maestria.nivel(m.xp);
        m.xp = Math.max(0, Math.min(Maestria.XP_MAX, xp));
        int depois = Maestria.nivel(m.xp);
        if (depois > antes) aoSubirDeNivel(jogador, depois, depois - antes);
    }

    private static void aoSubirDeNivel(ServerPlayer jogador, int nivel, int pontos) {
        // Jogador sem conexão (GameTest) não recebe mensagem.
        if (jogador.connection != null) {
            jogador.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "mensagem.minerwk.maestria", nivel, pontos), true);
        }
        if (jogador.level() instanceof net.minecraft.server.level.ServerLevel mundo) {
            mundo.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, .8f, 1f);
            mundo.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, jogador.getX(),
                    jogador.getY() + 1, jogador.getZ(), 30, .4, .6, .4, .25);
        }
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
        if (evento.getSource().getEntity() instanceof ServerPlayer atacante) {
            CAUSOU_DANO.add(atacante.getUUID());
            if (atacante == golpeDoKit) {
                ganharXp(atacante, Maestria.xpPorDano(evento.getNewDamage(), inimigo(evento.getEntity())));
            }
        }
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
