package com.minerwk.client.traje;

import com.minerwk.MineRWK;
import com.minerwk.client.kit.RecepcaoKit;
import com.minerwk.registro.Itens;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * O traje do personagem, desenhado por cima da skin de quem o escolheu, sem item vestido. Usa o renderer de
 * armadura do GeckoLib com um item escondido, então o modelo segue os ossos (e as animações) do jogador.
 */
public final class CamadaTraje extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final GeoModel<Itens.Traje> MODELO = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(Itens.Traje traje) {
            return MineRWK.id("geo/traje/" + traje.nome() + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(Itens.Traje traje) {
            return MineRWK.id("textures/traje/" + traje.nome() + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(Itens.Traje traje) {
            return MineRWK.id("animations/traje/" + traje.nome() + ".animation.json");
        }
    };

    private final GeoArmorRenderer<Itens.Traje> renderer = new GeoArmorRenderer<>(MODELO);
    private ItemStack traje = ItemStack.EMPTY;
    private static final EquipmentSlot[] ESPACOS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private CamadaTraje(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pai) {
        super(pai);
    }

    /** Se este jogador aparece com o traje (protótipo: só o próprio jogador, quando escolheu o Gojo). */
    public static boolean veste(AbstractClientPlayer jogador) {
        return jogador == Minecraft.getInstance().player && "gojo".equals(RecepcaoKit.estado().personagem());
    }

    /** Põe a camada do traje em todos os modelos de jogador e embrulha a de armadura, para poder escondê-la. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void aoAdicionarCamadas(EntityRenderersEvent.AddLayers evento) {
        for (var skin : evento.getSkins()) {
            if (!(evento.getSkin(skin) instanceof PlayerRenderer jogador)) continue;
            List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> camadas = jogador.layers;
            for (int i = 0; i < camadas.size(); i++) {
                if (camadas.get(i) instanceof HumanoidArmorLayer armadura) {
                    camadas.set(i, new CamadaArmadura(jogador, (RenderLayer) armadura));
                }
            }
            jogador.addLayer(new CamadaTraje(jogador));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int luz, AbstractClientPlayer jogador, float passo,
                       float passada, float parcial, float idade, float cabecaYaw, float cabecaPitch) {
        if (!veste(jogador) || jogador.isInvisible()) return;
        if (traje.isEmpty()) traje = new ItemStack(Itens.TRAJE_GOJO.get());
        // O renderer do GeckoLib só mostra os ossos do espaço pedido (e reaplica isso ao desenhar), então o
        // traje, que é uma peça só, é desenhado uma vez por espaço, como a armadura normal.
        for (EquipmentSlot espaco : ESPACOS) {
            renderer.prepForRender(jogador, traje, espaco, getParentModel(), buffer, parcial, passo, passada,
                    cabecaYaw, cabecaPitch);
            renderer.renderToBuffer(pose, null, luz, OverlayTexture.NO_OVERLAY, -1);
        }
    }

    /** A armadura normal continua protegendo, mas não é desenhada por cima do traje. */
    private static final class CamadaArmadura extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> original;

        CamadaArmadura(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pai,
                       RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> original) {
            super(pai);
            this.original = original;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int luz, AbstractClientPlayer jogador, float passo,
                           float passada, float parcial, float idade, float cabecaYaw, float cabecaPitch) {
            if (veste(jogador)) return;
            original.render(pose, buffer, luz, jogador, passo, passada, parcial, idade, cabecaYaw, cabecaPitch);
        }
    }
}
