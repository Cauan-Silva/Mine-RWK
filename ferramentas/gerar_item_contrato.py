"""Gera a textura 16x16 do Contrato do Matador de Feiticeiros (pergaminho, selo vermelho e uma lâmina).

Uso, na raiz do projeto: python ferramentas/gerar_item_contrato.py
"""
from PIL import Image

SAIDA = 'src/main/resources/assets/minerwk/textures/item/contrato_toji.png'

im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
px = im.load()
papel, sombra, borda, tinta = (226, 208, 160, 255), (190, 168, 118, 255), (112, 88, 52, 255), (70, 54, 36, 255)
for y in range(2, 14):
    for x in range(3, 13):
        px[x, y] = papel if x < 11 else sombra
for x in range(3, 13):
    px[x, 1] = borda
    px[x, 14] = borda
for y in range(2, 14):
    px[2, y] = borda
    px[13, y] = borda
# As linhas do texto.
for y in (4, 6, 8):
    for x in range(5, 11):
        px[x, y] = tinta
# O selo.
selo, selo_escuro = (170, 28, 34, 255), (110, 14, 22, 255)
for (x, y) in ((9, 10), (10, 10), (9, 11), (10, 11), (8, 11), (11, 11), (9, 12), (10, 12)):
    px[x, y] = selo
px[10, 12] = selo_escuro
px[11, 11] = selo_escuro
# A lâmina cruzando o canto.
aco, fio = (200, 206, 214, 255), (120, 128, 140, 255)
for i in range(6):
    px[1 + i, 14 - i] = aco
    px[2 + i, 14 - i] = fio
px[0, 15] = (90, 60, 30, 255)
im.save(SAIDA)
print('gravado', SAIDA)
