package com.minerwk.core.kit;

import com.minerwk.core.maestria.Melhorias;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O kit de um jogador: personagem, recurso, recargas e a habilidade em uso.
 * Regras: uma habilidade por vez; custo cobrado no disparo; recarga começa no disparo;
 * interrupção durante a carga dá recarga de punição de 1/4. Efeitos soltos e passivas rodam todo tick,
 * independentes da habilidade em uso. O jogador está sempre numa forma do personagem, e as teclas valem
 * para as habilidades dela; a forma não é salva.
 */
public final class KitDoJogador {
    private final Personagem personagem;
    private final Recurso recurso;
    private final Recargas recargas = new Recargas();
    private Execucao execucao;
    private final List<Ativo> efeitos = new ArrayList<>();
    private final List<Passiva> passivas;
    private Melhorias melhorias = Melhorias.NENHUMA;
    /** Se o jogador já despertou (de vez ou só nesta luta): libera o que o personagem tranca. */
    private boolean desperto;
    private String forma = Forma.BASE;

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

    public Melhorias melhorias() {
        return melhorias;
    }

    public boolean desperto() {
        return desperto;
    }

    /** Liga ou desliga o despertar. Voltar a trancar não interrompe o golpe em andamento. */
    public void definirDesperto(boolean desperto) {
        this.desperto = desperto;
    }

    public String forma() {
        return forma;
    }

    /** A habilidade da tecla na forma atual. */
    public Optional<Habilidade> habilidade(Tecla tecla) {
        return personagem.naTecla(forma, tecla);
    }

    /**
     * Passa para outra forma do personagem. Um golpe ainda em carga é interrompido, sem recarga de punição;
     * um que já disparou segue até o fim (é o caso do golpe que faz a troca). Devolve se mudou.
     */
    public boolean mudarForma(String nova, Contexto contexto) {
        if (forma.equals(nova) || !personagem.temForma(nova)) return false;
        if (execucao != null && execucao.fase() == Execucao.Fase.CARGA) {
            Habilidade h = execucao.habilidade();
            limpar();
            h.aoInterromper(contexto);
        }
        forma = nova;
        recurso.forma(nova);
        return true;
    }

    /** Se a habilidade da tecla, na forma atual, ainda exige o despertar. */
    public boolean trancada(Tecla tecla) {
        return habilidade(tecla).map(this::trancada).orElse(false);
    }

    /** Se a habilidade ainda exige o despertar. */
    public boolean trancada(Habilidade h) {
        return !desperto && personagem.trancas().habilidades().contains(h.id());
    }

    /** As melhorias da árvore do jogador: valem para o próximo golpe e, no recurso, na hora. */
    public void definirMelhorias(Melhorias melhorias) {
        this.melhorias = melhorias;
        recurso.melhorias(melhorias);
    }

    /** Os números da habilidade já com as melhorias de carga e de recarga. */
    public Numeros numeros(Habilidade h) {
        return melhorias.ajustar(h.id(), h.numeros());
    }

    /** Ferramenta de teste: enche o recurso e zera todas as recargas, sem trocar de kit. */
    public void recarregar() {
        recargas.limpar();
        recurso.carregar(recurso.maximo());
    }

    public Optional<Execucao> execucao() {
        return Optional.ofNullable(execucao);
    }

    /** Tenta começar a habilidade da tecla. Devolve se começou. */
    public boolean apertar(Tecla tecla, long agora) {
        return apertar(tecla, agora, false);
    }

    /** Como {@link #apertar(Tecla, long)}; agachado, vale a variação da habilidade ({@link Habilidade#agachado()}). */
    public boolean apertar(Tecla tecla, long agora, boolean agachado) {
        if (execucao != null) return false;
        Habilidade h = habilidade(tecla).map(b -> agachado ? b.agachado() : b).orElse(null);
        if (h == null || trancada(h) || !recargas.pronta(h.id(), agora) || !recurso.pode(h.numeros().custo())) return false;
        execucao = new Execucao(h, numeros(h));
        pendenteIniciar = true;
        return true;
    }

    public void soltar(Tecla tecla) {
        Habilidade h = habilidade(tecla).orElse(null);
        if (execucao != null && h != null && (h == execucao.habilidade() || h.agachado() == execucao.habilidade())) {
            execucao.soltar();
        }
    }

    /** Atordoado: se a habilidade ainda está em carga, é cancelada com recarga de punição. */
    public void atordoar(long agora) {
        atordoar(agora, Contexto.NULO);
    }

    /** Como {@link #atordoar(long)}, avisando a habilidade para desfazer o visual da carga. */
    public void atordoar(long agora, Contexto contexto) {
        if (execucao == null || execucao.fase() != Execucao.Fase.CARGA) return;
        Habilidade h = execucao.habilidade();
        recargas.iniciar(h.id(), agora, numeros(h).recarga() / 4);
        limpar();
        h.aoInterromper(contexto);
    }

    /** Levou um golpe: se a habilidade em carga não aguenta esse dano, é interrompida como em {@link #atordoar}. */
    public void levouDano(float dano, long agora) {
        levouDano(dano, agora, Contexto.NULO);
    }

    public void levouDano(float dano, long agora, Contexto contexto) {
        if (execucao != null && dano >= execucao.habilidade().danoQueInterrompe()) atordoar(agora, contexto);
    }

    /** Começa um efeito solto; ele roda a partir do próximo tick. */
    public void adicionarEfeito(Efeito efeito) {
        efeitos.add(new Ativo(efeito));
    }

    public List<Efeito> efeitos() {
        return efeitos.stream().map(a -> a.efeito).toList();
    }

    /**
     * Um golpe que anula técnicas acertou o jogador: os efeitos anuláveis acabam na hora, com o
     * {@code aoAcabar} deles. A recarga da habilidade já corre desde o disparo. Devolve se anulou algum.
     */
    public boolean anularTecnicas(Contexto contexto) {
        boolean anulou = false;
        for (Ativo a : List.copyOf(efeitos)) {
            if (a.efeito.anulavel() && efeitos.remove(a)) {
                a.efeito.aoAcabar(contexto);
                anulou = true;
            }
        }
        return anulou;
    }

    /** Acaba na hora os efeitos desse tipo, com o {@code aoAcabar} deles (o Overdrive desligado). Devolve se havia algum. */
    public boolean encerrarEfeitos(Class<? extends Efeito> tipo, Contexto contexto) {
        boolean havia = false;
        for (Ativo a : List.copyOf(efeitos)) {
            if (tipo.isInstance(a.efeito) && efeitos.remove(a)) {
                a.efeito.aoAcabar(contexto);
                havia = true;
            }
        }
        return havia;
    }

    /**
     * Cancela sem cobrar nem punir, encerra os efeitos sem {@code aoAcabar} e volta à forma base (jogador
     * saiu ou morreu).
     */
    public void descartar() {
        descartar(Contexto.NULO);
    }

    /** Como {@link #descartar()}, avisando a habilidade em carga para desfazer o visual. */
    public void descartar(Contexto contexto) {
        descartar(contexto, false);
    }

    /**
     * Como {@link #descartar(Contexto)}, mas o jogador continua na forma em que está: é o caso de mudar de
     * dimensão (entrar na arena, um portal), em que só o golpe e os efeitos acabam.
     */
    public void descartarMantendoAForma(Contexto contexto) {
        descartar(contexto, true);
    }

    private void descartar(Contexto contexto, boolean manterForma) {
        Habilidade emCarga = execucao != null && execucao.fase() == Execucao.Fase.CARGA ? execucao.habilidade() : null;
        // A animação cobre o golpe inteiro, não só a carga: havendo golpe ou efeito no ar, ela para.
        boolean emAndamento = execucao != null || !efeitos.isEmpty();
        limpar();
        for (Ativo a : List.copyOf(efeitos)) a.efeito.aoDescartar(contexto);
        efeitos.clear();
        if (emCarga != null) emCarga.aoInterromper(contexto);
        else if (emAndamento) contexto.pararAnimacao();
        // Uma cutscene no meio do golpe (a parte 2 do R) acaba junto.
        if (emAndamento) contexto.encerrarCutscene();
        // Quem morre ou sai volta à forma base.
        if (!manterForma && !Forma.BASE.equals(forma)) {
            forma = Forma.BASE;
            recurso.forma(forma);
        }
    }

    public List<Execucao.Evento> tick(long agora, EstadoDoJogador estado, Contexto contexto) {
        recurso.tick(estado);
        if (desperto || !personagem.trancas().passivas()) {
            for (Passiva p : passivas) p.tick(contexto, estado);
        }
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
            Numeros n = numeros(h);
            if (!recurso.pode(n.custo())) {
                // Sem recurso na hora do disparo: conta como interrupção na carga.
                recargas.iniciar(h.id(), agora, n.recarga() / 4);
                limpar();
                h.aoInterromper(contexto);
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
