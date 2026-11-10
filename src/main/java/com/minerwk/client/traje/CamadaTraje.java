package com.minerwk.client.traje;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.client.kit.RecepcaoKit;
import com.minerwk.client.vfx.VisuaisAtivos;
import com.minerwk.core.traje.TrajeGojo;
import com.minerwk.core.traje.TrajeGojo.Lado;
import com.minerwk.registro.Itens;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * O traje do personagem, desenhado por cima da skin de quem o escolheu, sem item vestido. Usa o renderer de
 * armadura do GeckoLib com um item escondido, então o modelo segue os ossos (e as animações) do jogador.
 * <p>
 * Quem desenha é a própria camada de armadura do jogador, trocada por uma subclasse: assim a armadura normal
 * pode ser escondida, e o traje aparece também nos braços que a Player Animation Library desenha em primeira
 * pessoa (ela só deixa passar as camadas de armadura e de item na mão). A manga da mão parada em primeira
 * pessoa vem do {@link RenderArmEvent}.
 */
public final class CamadaTraje {
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

    /** O renderer do traje: uma peça só, com tudo visível (ou só um braço, em primeira pessoa). */
    private static final class Renderer extends GeoArmorRenderer<Itens.Traje> {
        /** Em primeira pessoa, o único braço a desenhar; null desenha o traje inteiro. */
        HumanoidArm soBraco;
        boolean semVenda;
        /** Balanço de cada segmento da barra neste quadro (nome do osso → radianos); vazio em primeira pessoa. */
        final Map<String, Float> barra = new HashMap<>();

        Renderer() {
            super(MODELO);
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        protected void applyBoneVisibilityBySlot(EquipmentSlot espaco) {
            // O padrão mostra só as peças do espaço pedido. O traje é uma peça só: mostra cada parte se a parte
            // do modelo do jogador estiver visível (em primeira pessoa com animação, só os braços estão).
            setAllVisible(false);
            boolean cabeca = false;
            if (soBraco != null) {
                setBoneVisible(soBraco == HumanoidArm.RIGHT ? rightArm : leftArm, true);
            } else if (baseModel != null) {
                cabeca = baseModel.head.visible;
                setBoneVisible(head, baseModel.head.visible);
                setBoneVisible(body, baseModel.body.visible);
                setBoneVisible(rightArm, baseModel.rightArm.visible);
                setBoneVisible(leftArm, baseModel.leftArm.visible);
                setBoneVisible(rightLeg, baseModel.rightLeg.visible);
                setBoneVisible(leftLeg, baseModel.leftLeg.visible);
                setBoneVisible(rightBoot, baseModel.rightLeg.visible);
                setBoneVisible(leftBoot, baseModel.leftLeg.visible);
            }
            boolean comCabeca = cabeca;
            GeoModel<Itens.Traje> modelo = getGeoModel();
            modelo.getBone(TrajeGojo.OSSO_VENDA).ifPresent(o -> o.setHidden(!comCabeca || semVenda));
            modelo.getBone(TrajeGojo.OSSO_OLHOS).ifPresent(o -> o.setHidden(!comCabeca || !semVenda || !ConfigCliente.olhosBrilhando()));
            for (Lado lado : Lado.values()) {
                for (int coluna = 0; coluna < lado.colunas(); coluna++) {
                    for (int linha = 0; linha < 2; linha++) {
                        String nome = TrajeGojo.ossoDaBarra(lado, coluna, linha);
                        float a = barra.getOrDefault(nome, 0f);
                        modelo.getBone(nome).ifPresent(o -> girar(o, lado, a));
                    }
                }
            }
        }

        /** Abre o segmento para fora do corpo: os de trás e da frente giram em X, os dos lados em Z. */
        private static void girar(GeoBone osso, Lado lado, float a) {
            switch (lado) {
                case TRAS -> osso.setRotX(-a);
                case FRENTE -> osso.setRotX(a);
                case ESQUERDA -> osso.setRotZ(-a);
                case DIREITA -> osso.setRotZ(a);
            }
        }
    }

    private static final Renderer RENDERER = new Renderer();
    private static ItemStack traje = ItemStack.EMPTY;

    private CamadaTraje() {
    }

    /** Se este jogador aparece com o traje: o personagem dele (o próprio ou o avisado pelo servidor) tem um. */
    public static boolean veste(AbstractClientPlayer jogador) {
        String personagem = jogador == Minecraft.getInstance().player
                ? RecepcaoKit.estado().personagem() : PersonagensVistos.de(jogador.getId());
        return TrajeGojo.de(personagem).isPresent();
    }

    /** Se a venda deste jogador está fora, pelos visuais dele em andamento. */
    private static boolean semVenda(AbstractClientPlayer jogador) {
        List<String> tipos = new ArrayList<>();
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.dono() == jogador.getId() && !v.encerrado()) tipos.add(v.tipo());
        }
        return TrajeGojo.vendaFora(tipos);
    }

    /** Troca a camada de armadura de todos os modelos de jogador pela que também desenha o traje. */
    public static void aoAdicionarCamadas(EntityRenderersEvent.AddLayers evento) {
        for (PlayerSkin.Model skin : evento.getSkins()) {
            if (!(evento.getSkin(skin) instanceof PlayerRenderer jogador)) continue;
            boolean fino = skin == PlayerSkin.Model.SLIM;
            // Os mesmos modelos que o PlayerRenderer usa para a armadura.
            var interno = new HumanoidArmorModel<AbstractClientPlayer>(evento.getContext().bakeLayer(
                    fino ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR));
            var externo = new HumanoidArmorModel<AbstractClientPlayer>(evento.getContext().bakeLayer(
                    fino ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR));
            List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> camadas = jogador.layers;
            for (int i = 0; i < camadas.size(); i++) {
                if (camadas.get(i) instanceof HumanoidArmorLayer) {
                    camadas.set(i, new ArmaduraETraje(jogador, interno, externo, evento.getContext().getModelManager()));
                }
            }
        }
    }

    private static ItemStack traje() {
        if (traje.isEmpty()) traje = new ItemStack(Itens.TRAJE_GOJO.get());
        return traje;
    }

    private static void desenhar(PlayerModel<AbstractClientPlayer> modelo, PoseStack pose, MultiBufferSource buffer, int luz,
                                 AbstractClientPlayer jogador, float passo, float passada, float parcial, float idade,
                                 float cabecaYaw, float cabecaPitch) {
        RENDERER.soBraco = null;
        RENDERER.semVenda = semVenda(jogador);
        balancarBarra(jogador, idade);
        RENDERER.prepForRender(jogador, traje(), EquipmentSlot.CHEST, modelo, buffer, parcial, passo, passada,
                cabecaYaw, cabecaPitch);
        RENDERER.renderToBuffer(pose, null, luz, OverlayTexture.NO_OVERLAY, -1);
    }

    /** Calcula o balanço da barra pela velocidade do jogador no referencial do corpo. */
    private static void balancarBarra(AbstractClientPlayer jogador, float idade) {
        double dx = jogador.getX() - jogador.xo, dy = jogador.getY() - jogador.yo, dz = jogador.getZ() - jogador.zo;
        float yaw = jogador.yBodyRot * Mth.DEG_TO_RAD;
        // Olhando para o sul (yaw 0), a frente é +z e a direita é -x.
        double frente = -dx * Mth.sin(yaw) + dz * Mth.cos(yaw);
        double lateral = -dx * Mth.cos(yaw) - dz * Mth.sin(yaw);
        RENDERER.barra.clear();
        for (Lado lado : Lado.values()) {
            for (int coluna = 0; coluna < lado.colunas(); coluna++) {
                for (int linha = 0; linha < 2; linha++) {
                    RENDERER.barra.put(TrajeGojo.ossoDaBarra(lado, coluna, linha),
                            (float) TrajeGojo.balanco(lado, linha, frente, lateral, dy, idade + coluna * 2.3));
                }
            }
        }
    }

    /** Primeira pessoa: a manga do traje no braço que o jogo está desenhando. */
    public static void aoDesenharBraco(RenderArmEvent evento) {
        AbstractClientPlayer jogador = evento.getPlayer();
        if (!veste(jogador)) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(jogador) instanceof PlayerRenderer renderer)) return;
        PlayerModel<AbstractClientPlayer> modelo = renderer.getModel();
        // A mesma pose em que o jogo desenha o braço em primeira pessoa (PlayerRenderer.renderHand).
        modelo.attackTime = 0;
        modelo.crouching = false;
        modelo.swimAmount = 0;
        modelo.setupAnim(jogador, 0, 0, 0, 0, 0);
        ModelPart braco = evento.getArm() == HumanoidArm.RIGHT ? modelo.rightArm : modelo.leftArm;
        braco.xRot = 0;
        RENDERER.soBraco = evento.getArm();
        RENDERER.barra.clear();
        RENDERER.prepForRender(jogador, traje(), EquipmentSlot.CHEST, modelo, evento.getMultiBufferSource(), 1f, 0, 0, 0, 0);
        RENDERER.renderToBuffer(evento.getPoseStack(), null, evento.getPackedLight(), OverlayTexture.NO_OVERLAY, -1);
        RENDERER.soBraco = null;
    }

    /**
     * A camada de armadura do jogador, que antes dela desenha o traje. A armadura normal continua protegendo,
     * mas não é desenhada por cima do traje (a config inverte).
     */
    private static final class ArmaduraETraje extends HumanoidArmorLayer<AbstractClientPlayer,
            PlayerModel<AbstractClientPlayer>, HumanoidArmorModel<AbstractClientPlayer>> {
        ArmaduraETraje(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pai,
                       HumanoidArmorModel<AbstractClientPlayer> interno, HumanoidArmorModel<AbstractClientPlayer> externo,
                       ModelManager modelos) {
            super(pai, interno, externo, modelos);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int luz, AbstractClientPlayer jogador, float passo,
                           float passada, float parcial, float idade, float cabecaYaw, float cabecaPitch) {
            if (veste(jogador)) {
                if (!jogador.isInvisible()) {
                    desenhar(getParentModel(), pose, buffer, luz, jogador, passo, passada, parcial, idade, cabecaYaw, cabecaPitch);
                }
                if (!ConfigCliente.armaduraSobreTraje()) return;
            }
            super.render(pose, buffer, luz, jogador, passo, passada, parcial, idade, cabecaYaw, cabecaPitch);
        }
    }
}
