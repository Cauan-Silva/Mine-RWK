"""Confere as gravações CC0 contra os geradores de som: roda a síntese de todos os sons e lista as gravações de
art/audio/cc0 que nenhum gerador usa (créditos de um som que não entra no mod) e as que faltam no fontes.json.

Rodar: python ferramentas/conferir_gravacoes.py
"""
import importlib
import json
import sys

import sfx_motor as m

GERADORES = ('cid', 'gojo', 'ichigo', 'ragnarok', 'toji')


def conferir():
    usados = set()
    original = m.Som.amostra

    def espia(self, nome, *a, **k):
        usados.update(nome if isinstance(nome, (list, tuple)) else [nome])
        return original(self, nome, *a, **k)

    m.Som.amostra = espia
    for g in GERADORES:
        gerador = importlib.import_module('gerar_sfx_' + g)
        for nome, (duracao, *_) in gerador.SONS.items():
            gerador.sintese(nome, m.Som(duracao, 1))
    fontes = set(json.loads((m.ROOT / 'art/audio/cc0/fontes.json').read_text(encoding='utf-8')))
    return sorted(fontes - usados), sorted(usados - fontes)


if __name__ == '__main__':
    sobra, falta = conferir()
    print('sem uso:', sobra or 'nenhuma')
    print('fora do fontes.json:', falta or 'nenhuma')
    sys.exit(1 if sobra or falta else 0)
