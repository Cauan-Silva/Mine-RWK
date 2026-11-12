package com.minerwk.core.hud;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lembra quando cada golpe ficou pronto, para o brilho da HUD. Só conta a transição que o cliente viu (estava em
 * recarga e deixou de estar): no login ou na troca de personagem, os golpes que já estavam prontos não piscam.
 */
public final class Pronto {
    private final Set<String> emRecarga = new HashSet<>();
    private final Map<String, Long> prontaEm = new HashMap<>();

    public void ver(String id, boolean recarregando, long agora) {
        if (recarregando) {
            emRecarga.add(id);
        } else if (emRecarga.remove(id)) {
            prontaEm.put(id, agora);
        }
    }

    /** O tick em que o golpe ficou pronto pela última vez; -1 se o cliente não viu. */
    public long prontaEm(String id) {
        return prontaEm.getOrDefault(id, -1L);
    }

    public void limpar() {
        emRecarga.clear();
        prontaEm.clear();
    }
}
