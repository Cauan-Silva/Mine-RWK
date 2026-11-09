package com.minerwk.core.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O kit de um jogador: personagem, recurso, recargas e a habilidade em uso.
 * Regras: uma habilidade por vez; custo cobrado no disparo; recarga começa no disparo;
 * interrupção durante a carga dá recarga de punição de 1/4.
 */
public final class KitDoJogador {
    private final Personagem personagem;
    private final Recurso recurso;
    private final Recargas recargas = new Recargas();
    private Execucao execucao;
    /** O gancho de início de carga roda no próximo tick, quando há um Contexto. */
    private boolean pendenteIniciar;

    public KitDoJogador(Personagem personagem) {
        this.personagem = personagem;
        this.recurso = personagem.recurso().get();
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

    /** Cancela sem cobrar nem punir (jogador saiu ou morreu). */
    public void descartar() {
        limpar();
    }

    public List<Execucao.Evento> tick(long agora, EstadoDoJogador estado, Contexto contexto) {
        recurso.tick(estado);
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

    private void limpar() {
        execucao = null;
        pendenteIniciar = false;
    }
}
