package com.minerwk.core.vfx;

/**
 * A forma do Getsuga Tenshō (direção de arte do Minezin, com os números do Mine-RWK): uma meia-lua em
 * volume, de fio afiado na frente e dorso grosso atrás, que nasce pequena na lâmina, voa reta e se parte em
 * lascas ao parar. No Shikai é azul-clara; no Bankai, preta com a borda vermelha e chamas mais rasgadas.
 * Coordenadas no referencial do golpe: x para a direita, y para cima, z para a frente; origem no centro da
 * meia-lua. O passo e o alcance são os mesmos do golpe no servidor.
 */
public final class GetsugaIchigo {
    public record P(double x, double y, double z) {
    }

    /** Blocos por tick e alcance do Getsuga em cada forma (o kit usa estes mesmos números). */
    public static final double PASSO_SHIKAI = 1.6, ALCANCE_SHIKAI = 24, PASSO_BANKAI = 2.4, ALCANCE_BANKAI = 28;
    /** Ticks crescendo ao sair da lâmina e ticks se desfazendo depois de parar. */
    public static final int CRESCE = 3, DISSIPA = 4;
    public static final int PONTOS = 20, SECAO = 10, CHAMAS = 14, LASCAS = 12, ECOS = 2, LINGUAS = 14;
    /** Inclinação do corte (de cima à direita para baixo à esquerda), em graus, e meia abertura do arco. */
    public static final double INCLINACAO = 35, ABERTURA = Math.toRadians(76);
    /** De onde vem a luz, no referencial do golpe: de cima, da direita e de trás do jogador. */
    private static final double LX = .45, LY = .8, LZ = -.4;

    private GetsugaIchigo() {
    }

    private static double suave(double t) {
        return PerfilVfxGojo.suave(t);
    }

    static double acaso(double n) {
        double x = Math.sin(n * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }

    public static double passo(boolean bankai) {
        return bankai ? PASSO_BANKAI : PASSO_SHIKAI;
    }

    public static double alcance(boolean bankai) {
        return bankai ? ALCANCE_BANKAI : ALCANCE_SHIKAI;
    }

    /** Ticks de voo até o alcance máximo. */
    public static int voo(boolean bankai) {
        return (int) Math.ceil(alcance(bankai) / passo(bankai));
    }

    /** Quanto o visual dura: o voo inteiro e o tempo de se desfazer. */
    public static int duracao(boolean bankai) {
        return voo(bankai) + DISSIPA;
    }

    /** A que distância da saída está o centro da meia-lua no tick {@code t} (o primeiro passo é no tick 1). */
    public static double distancia(double t, boolean bankai) {
        return Math.max(0, Math.min(alcance(bankai), passo(bankai) * t));
    }

    /**
     * O tick em que a meia-lua parou num bloco: o do aviso do servidor, ou antes, se ela já tinha chegado
     * ao ponto da parada ({@code andou} blocos da saída). Sem isso ela seria desenhada um passo dentro da parede.
     */
    public static double parada(double avisoEm, double andou, boolean bankai) {
        return Math.max(0, Math.min(avisoEm, andou / passo(bankai)));
    }

    /** Raio do arco: a meia altura da meia-lua fica perto do raio do dano (1,5). */
    public static double raio(boolean bankai) {
        return bankai ? 1.75 : 1.55;
    }

    public static double espessura(boolean bankai) {
        return bankai ? .75 : .6;
    }

    public static double profundidade(boolean bankai) {
        return bankai ? .85 : .7;
    }

    /** De 0,35 a 1: a meia-lua nasce pequena na lâmina e cresce até o tamanho cheio. */
    public static double escala(double t) {
        return .35 + .65 * suave(t / CRESCE);
    }

    /** De 0 a 1 enquanto se desfaz, a partir do tick {@code fim} em que parou. */
    public static double dissolve(double t, double fim) {
        return suave((t - fim) / DISSIPA);
    }

    private static double angulo(int i) {
        return -ABERTURA + 2 * ABERTURA * i / (PONTOS - 1.0);
    }

    /** Ponto do arco de raio {@code rr}: o meio bojudo aponta para a frente e as pontas recurvam para trás. */
    private static P eixo(double th, double r, double rr, double recuo) {
        double a = Math.toRadians(INCLINACAO), u = Math.sin(th) * rr;
        return new P(Math.sin(a) * u, Math.cos(a) * u, Math.cos(th) * rr - r - recuo);
    }

    /** Direção da espessura (perpendicular ao plano do arco). */
    private static P perpendicular() {
        double a = Math.toRadians(INCLINACAO);
        return new P(Math.cos(a), -Math.sin(a), 0);
    }

    /** O fio da frente no ponto {@code i} do arco. */
    public static P frente(int i, boolean bankai) {
        double r = raio(bankai);
        return eixo(angulo(i), r, r, 0);
    }

    /** O dorso: grosso no meio, afinando nas pontas, com recortes de chama que mudam a cada 2 ticks. */
    public static P tras(int i, double t, boolean bankai) {
        double th = angulo(i), r = raio(bankai), k = Math.pow(Math.cos(th), 1.5);
        double rasgo = (bankai ? .3 : .16) * acaso(i * 3 + Math.floor(t / 2) * 11) * k;
        return eixo(th, r, r - espessura(bankai) * k - rasgo, .2 * k + rasgo * .3);
    }

    /**
     * O volume: uma seção em cunha (fio em {@code j} = 0, dorso em {@code j} = SECAO / 2) varrida pelo arco;
     * a profundidade some nas pontas.
     */
    public static P volume(int i, int j, double t, boolean bankai) {
        P o = frente(i, bankai), n = tras(i, t, bankai);
        double v = j * Math.PI * 2 / SECAO, k = Math.pow(Math.cos(angulo(i)), 1.5);
        double cx = (o.x + n.x) / 2, cy = (o.y + n.y) / 2, cz = (o.z + n.z) / 2;
        double rx = (o.x - n.x) / 2, ry = (o.y - n.y) / 2, rz = (o.z - n.z) / 2;
        double d = profundidade(bankai) * k * Math.sin(v) * (.3 + .7 * (1 - Math.cos(v)) / 2);
        P pp = perpendicular();
        return new P(cx + rx * Math.cos(v) + pp.x * d, cy + ry * Math.cos(v) + pp.y * d, cz + rz * Math.cos(v));
    }

    /** Normal da superfície no vértice, por diferenças no próprio volume. */
    public static P normal(int i, int j, double t, boolean bankai) {
        P a = volume(Math.min(PONTOS - 1, i + 1), j, t, bankai), b = volume(Math.max(0, i - 1), j, t, bankai);
        P c = volume(i, (j + 1) % SECAO, t, bankai), d = volume(i, (j + SECAO - 1) % SECAO, t, bankai);
        double ux = a.x - b.x, uy = a.y - b.y, uz = a.z - b.z, vx = c.x - d.x, vy = c.y - d.y, vz = c.z - d.z;
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return l < 1e-9 ? new P(0, 0, -1) : new P(nx / l, ny / l, nz / l);
    }

    /** De 0 a 1: quanto a superfície está de raspão para quem olha de ({@code vx}, {@code vy}, {@code vz}). */
    public static double borda(P n, double vx, double vy, double vz) {
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        return vl < 1e-9 ? 0 : Math.pow(1 - Math.abs((n.x * vx + n.y * vy + n.z * vz) / vl), 3);
    }

    /** Brilho do vértice: luz difusa (dos dois lados) mais a borda de raspão; de 0,28 a 1,25. */
    public static double luz(P n, double vx, double vy, double vz) {
        double l = Math.sqrt(LX * LX + LY * LY + LZ * LZ);
        double difusa = Math.abs(n.x * LX + n.y * LY + n.z * LZ) / l;
        return Math.min(1.25, .28 + .72 * difusa + .55 * borda(n, vx, vy, vz));
    }

    /** Chama {@code j} saindo do dorso para trás; {@code u} de 0 (no dorso) a 1 (na ponta). */
    public static P chama(int j, double u, double t, boolean bankai) {
        int i = 1 + (int) (acaso(j) * (PONTOS - 2));
        P b = tras(i, t, bankai);
        double th = angulo(i), a = Math.toRadians(INCLINACAO);
        double comprimento = (.35 + .6 * acaso(j + 7 + Math.floor(t / 2))) * (bankai ? 1.35 : 1);
        double fora = Math.sin(th) * .25 * u * comprimento, onda = Math.sin(u * 5 + t * .9 + j) * .08 * u;
        return new P(b.x + Math.sin(a) * fora + onda, b.y + Math.cos(a) * fora, b.z - u * comprimento);
    }

    /** Lasca {@code k} depois de parar no tick {@code fim}: sai de um ponto do fio e abre para fora. */
    public static P lasca(int k, double t, double fim, boolean bankai) {
        int i = (int) Math.round(k * (PONTOS - 1.0) / (LASCAS - 1));
        P f = frente(i, bankai);
        double d = dissolve(t, fim) * (.8 + acaso(k + 3) * 1.1);
        double n = Math.hypot(f.x, f.y);
        n = n < 1e-6 ? 1 : n;
        return new P(f.x + f.x / n * d, f.y + f.y / n * d + (acaso(k + 9) - .5) * d * .6, f.z + d * .6);
    }

    /**
     * Língua {@code j} de reiatsu subindo em volta do corpo (carga do Getsuga e do Bankai): {@code u} de 0
     * (nos pés) a 1 (na ponta), relativa aos pés.
     */
    public static P lingua(int j, double u, double t, double altura) {
        double a = j * Math.PI * 2 / LINGUAS + acaso(j) * .6, r = .55 + .12 * Math.sin(t * .8 + j);
        double alt = altura * (.65 + .35 * acaso(j + 5 + Math.floor(t / 2)));
        return new P(Math.cos(a) * r * (1 - u * .35) + Math.sin(u * 6 + t + j) * .08, u * alt, Math.sin(a) * r * (1 - u * .35));
    }
}
