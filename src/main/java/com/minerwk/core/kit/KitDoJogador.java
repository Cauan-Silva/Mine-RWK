package com.minerwk.core.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O kit de um jogador: personagem, recurso, recargas e a habilidade em uso.
 * Regras: uma habilidade por vez; custo cobrado no disparo; recarga começa no disparo;
 * interrupção durante a carga dá recarga de punição de 1/4. Efeitos soltos e passivas rodam todo tick,
 * independentes da habilidade em uso.
 */
public final class KitDoJogador {
    private final Personagem personagem;
    private final Recurso recurso;
    private final Recargas recargas = new Recargas();
    private Execucao execucao;
    private final List<Ativo> efeitos = new ArrayList<>();
    private final List<Passiva> passivas;

    private static final class Ativo {
        final Efeito efeito;
        int t;

        Ativo(Efeito efeito) {
            this.efeito = efeito;
        }
    }
    /** O gancho de início de carga roda no próximo tick, quando há um Contexto. */
    private boolean pendenteIniciar;

    public KitDoJogador(Personagem personagem) {
        this.personagem = personagem;
        this.recurso = personagem.recurso().get();
        this.passivas = personagem.passivas().stream().map(java.util.function.Supplier::get).toList();
    }

    public Personagem personagem() {
        return personagem;
    }

    public Recurso recurso() {
        return recurso;
    }

    public Recargas recargas() {
        return recargas;
    }

    public Optional<Execucao> execucao() {
        return Optional.ofNullable(execucao);
    }

    /** Tenta começar a habilidade da tecla. Devolve se começou. */
    public boolean apertar(Tecla tecla, long agora) {
        if (execucao != null) return false;
        Habilidade h = personagem.naTecla(tecla).orElse(null);
        if (h == null || !recargas.pronta(h.id(), agora) || !recurso.pode(h.numeros().custo())) return false;
        execucao = new Execucao(h);
        pendenteIniciar = true;
        return true;
    }

    public void soltar(Tecla tecla) {
        if (execucao != null && personagem.naTecla(tecla).orElse(null) == execucao.habilidade()) {
            execucao.soltar();
        }
    }

    /** Atordoado: se a habilidade ainda está em carga, é cancelada com recarga de punição. */
    public void atordoar(long agora) {
        if (execucao == null || execucao.fase() != Execucao.Fase.CARGA) return;
        Numeros n = execucao.habilidade().numeros();
        recargas.iniciar(execucao.habilidade().id(), agora, n.recarga() / 4);
        limpar();
    }

    /** Levou um golpe: se a habilidade em carga não aguenta esse dano, é interrompida como em {@link #atordoar}. */
    public void levouDano(float dano, long agora) {
        if (execucao != null && dano >= execucao.habilidade().danoQueInterrompe()) atordoar(agora);
    }

    /** Começa um efeito solto; ele roda a partir do próximo tick. */
    public void adicionarEfeito(Efeito efeito) {
        efeitos.add(new Ativo(efeito));
    }

    public List<Efeito> efeitos() {
        return efeitos.stream().map(a -> a.efeito).toList();
    }

    /** Cancela sem cobrar nem punir e encerra os efeitos sem {@code aoAcabar} (jogador saiu ou morreu). */
    public void descartar() {
        limpar();
        efeitos.clear();
    }

    public List<Execucao.Evento> tick(long agora, EstadoDoJogador estado, Contexto contexto) {
        recurso.tick(estado);
        for (Passiva p : passivas) p.tick(contexto, estado);
        tickEfeitos(contexto);
        List<Execucao.Evento> eventos = new ArrayList<>(1);
        if (execucao == null) return eventos;
        Habilidade h = execucao.habilidade();
        if (pendenteIniciar) {
            pendenteIniciar = false;
            h.aoIniciarCarga(contexto);
        }
        Execucao.Evento e = execucao.tick();
        if (e instanceof Execucao.Disparou d) {
            Numeros n = h.numeros();
            if (!recurso.pode(n.custo())) {
                // Sem recurso na hora do disparo: conta como interrupção na carga.
                recargas.iniciar(h.id(), agora, n.recarga() / 4);
                limpar();
                return eventos;
            }
            recurso.gastar(n.custo());
            recargas.iniciar(h.id(), agora, n.recarga());
            h.aoDisparar(contexto, d.forca());
            eventos.add(e);
        } else if (e == Execucao.Evento.TERMINOU) {
            h.aoTerminar(contexto);
            limpar();
            eventos.add(e);
        }
        return eventos;
    }

    private void tickEfeitos(Contexto contexto) {
        // Cópia: um efeito pode criar outro durante o tick.
        for (Ativo a : List.copyOf(efeitos)) {
            if (!efeitos.contains(a)) continue;
            a.efeito.tick(contexto, a.t++);
            if (a.t >= a.efeito.duracao() || a.efeito.encerrado()) {
                efeitos.remove(a);
                a.efeito.aoAcabar(contexto);
            }
        }
    }

    private void limpar() {
        execucao = null;
        pendenteIniciar = false;
    }
}
