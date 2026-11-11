package com.minerwk.core.arena;

import com.minerwk.core.kit.Barreira;
import com.minerwk.core.kit.Vetor;

/**
 * A caixa de uma vaga da dimensão da arena. As vagas ficam numa grade de {@link #LADO_VAGA} blocos, então
 * a caixa de qualquer posição sai só da posição: cliente e servidor aplicam a mesma regra sem conversar.
 */
public record CaixaArena(int indiceX, int indiceZ) {
    public static final int LADO_VAGA = 512;
    public static final int LARGURA = 101;
    public static final int ALTURA = 76;
    /** Blocos do centro até a face (o bloco do centro não conta). */
    public static final int MEIA = 50;
    /** Y dos pés de quem está no pátio. */
    public static final int Y_PATIO = 64;
    public static final int Y_FUNDO = 44;
    public static final int Y_TETO = 120;
    public static final int VAGAS_POR_LINHA = 64;
    /** Do jogador até o Toji na entrada. */
    public static final int DISTANCIA_DO_TOJI = 16;

    public static CaixaArena daVaga(int indice) {
        return new CaixaArena(indice % VAGAS_POR_LINHA, indice / VAGAS_POR_LINHA);
    }

    public static CaixaArena daPosicao(double x, double z) {
        return new CaixaArena(Math.floorDiv((int) Math.floor(x), LADO_VAGA), Math.floorDiv((int) Math.floor(z), LADO_VAGA));
    }

    /** O índice que {@link #daVaga} recebe; só vale para vagas da grade (índices de 0 a 63 em x, de 0 em z). */
    public int indice() {
        return indiceZ * VAGAS_POR_LINHA + indiceX;
    }

    /** X do bloco do centro. */
    public int centroX() {
        return LADO_VAGA / 2 + LADO_VAGA * indiceX;
    }

    /** Z do bloco do centro. */
    public int centroZ() {
        return LADO_VAGA / 2 + LADO_VAGA * indiceZ;
    }

    public boolean contemBloco(int x, int y, int z) {
        return Math.abs(x - centroX()) <= MEIA && Math.abs(z - centroZ()) <= MEIA && y >= Y_FUNDO && y < Y_TETO;
    }

    /** O meio do bloco do centro, na altura do pátio. */
    public Vetor centro() {
        return new Vetor(centroX() + .5, Y_PATIO, centroZ() + .5);
    }

    /** Lado sul do pátio; quem entra olha para o norte (yaw 180). */
    public Vetor entradaJogador() {
        return new Vetor(centroX() + .5, Y_PATIO, centroZ() + 16.5);
    }

    public Vetor entradaToji() {
        Vetor j = entradaJogador();
        return new Vetor(j.x(), j.y(), j.z() - DISTANCIA_DO_TOJI);
    }

    public boolean abaixoDoFundo(double y) {
        return y < Y_FUNDO;
    }

    /**
     * Paredes e teto para um corpo de {@code meiaLargura} e {@code altura}. O fundo é aberto: quem cai
     * abaixo dele é tratado pela regra da queda no vazio.
     */
    public Barreira.Correcao corrigir(Vetor pes, Vetor velocidade, double meiaLargura, double altura) {
        double folga = meiaLargura + Barreira.MARGEM;
        double x = pes.x(), y = pes.y(), z = pes.z();
        double vx = velocidade.x(), vy = velocidade.y(), vz = velocidade.z();
        double xMin = centroX() - MEIA + folga, xMax = centroX() + MEIA + 1 - folga;
        double zMin = centroZ() - MEIA + folga, zMax = centroZ() + MEIA + 1 - folga;
        if (x < xMin) {
            x = xMin;
            vx = Math.max(0, vx);
        } else if (x > xMax) {
            x = xMax;
            vx = Math.min(0, vx);
        }
        if (z < zMin) {
            z = zMin;
            vz = Math.max(0, vz);
        } else if (z > zMax) {
            z = zMax;
            vz = Math.min(0, vz);
        }
        if (y > Y_TETO - altura) {
            y = Y_TETO - altura;
            vy = Math.min(0, vy);
        }
        if (x == pes.x() && y == pes.y() && z == pes.z()) return new Barreira.Correcao(pes, velocidade, false);
        return new Barreira.Correcao(new Vetor(x, y, z), new Vetor(vx, vy, vz), false);
    }
}
