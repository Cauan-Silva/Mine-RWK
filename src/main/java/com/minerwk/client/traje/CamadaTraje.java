package com.minerwk.client.traje;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelLayerLocation;
import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.client.kit.RecepcaoKit;
import com.minerwk.client.vfx.VisuaisAtivos;
import com.minerwk.core.traje.TrajeGojo;
import com.minerwk.core.traje.TrajeGojo.Lado;
import com.minerwk.core.traje.TrajeIchigo;
import com.minerwk.core.traje.Trajes;
import com.minerwk.core.traje.tecido.MolaManto;
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
    /** O modelo de um traje; cada renderer tem o seu, porque o GeckoLib guarda nele os ossos em uso. */
    private static final class Modelo extends GeoModel<Itens.Traje> {
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
    }

    /** O renderer do traje: uma peça só, com tudo visível (ou só um braço, em primeira pessoa). */
    private static final class Renderer extends GeoArmorRenderer<Itens.Traje> {
        /** Em primeira pessoa, o único braço a desenhar; null desenha o traje inteiro. */
        HumanoidArm soBraco;
        boolean semVenda;
        /** Balanço de cada segmento da barra neste quadro (nome do osso → radianos); vazio em primeira pessoa. */
        final Map<String, Float> barra = new HashMap<>();
        /** Trajes do Ichigo: de quem é o pano deste quadro (null em primeira pessoa) e se a espada está fora da mão. */
        java.util.UUID pano;
        float parcialDoPano;
        boolean semArma;

        Renderer() {
            super(new Modelo());
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
            // Sempre escrito, nos dois sentidos: o osso é do modelo, que vale para todos os jogadores do traje.
            modelo.getBone(TrajeIchigo.OSSO_ARMA).ifPresent(o -> o.setHidden(semArma));
            balancarPano(modelo);
        }

        /** Trajes do Ichigo: cada osso de pano gira o que o tecido mandar, a partir do repouso do modelo. */
        private void balancarPano(GeoModel<Itens.Traje> modelo) {
            for (GeoBone osso : modelo.getAnimationProcessor().getRegisteredBones()) {
                if (!TecidoCliente.dePano(osso.getName())) continue;
                var repouso = osso.getInitialSnapshot();
                MolaManto.Pose p = pano == null ? null : TecidoCliente.pose(pano, osso.getName(), parcialDoPano);
                osso.setRotX(repouso.getRotX() + (p == null ? 0 : (float) p.x()));
                osso.setRotY(repouso.getRotY() + (p == null ? 0 : (float) p.y()));
                osso.setRotZ(repouso.getRotZ() + (p == null ? 0 : (float) p.z()));
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

    /** Um renderer e um item escondido por traje (nome do modelo). */
    private static final Map<String, Renderer> RENDERERS = new HashMap<>();
    private static final Map<String, ItemStack> ITENS = new HashMap<>();

    private CamadaTraje() {
    }

    /** Se este jogador aparece com um traje: o personagem dele (o próprio ou o avisado pelo servidor) tem um. */
    public static boolean veste(AbstractClientPlayer jogador) {
        return !trajeDe(jogador).isEmpty();
    }

    /**
     * O traje com que este jogador aparece (nome do modelo), ou vazio. O personagem e a forma do próprio
     * jogador vêm do kit dele, que chega antes; os dos outros, do aviso do servidor.
     */
    public static String trajeDe(AbstractClientPlayer jogador) {
        PersonagensVistos.Visto visto = PersonagensVistos.visto(jogador.getId());
        boolean eu = jogador == Minecraft.getInstance().player;
        String personagem = eu ? RecepcaoKit.estado().personagem() : visto.personagem();
        String forma = eu ? RecepcaoKit.estado().forma() : visto.forma();
        String traje = Trajes.de(personagem, forma).orElse("");
        // A forma final do Mugetsu vem dos visuais do golpe, não da forma do kit: dura só a cutscene e o corte.
        if (TrajeIchigo.eDoIchigo(traje) && naFormaFinal(jogador)) return TrajeIchigo.MUGETSU;
        return traje;
    }

    private static boolean naFormaFinal(AbstractClientPlayer jogador) {
        long agora = jogador.level().getGameTime();
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.dono() == jogador.getId() && !v.encerrado() && TrajeIchigo.formaFinal(v.tipo(), v.idade(agora, 0))) return true;
        }
        return false;
    }

    /** Se a máscara Hollow está no rosto deste jogador (e ele não está na forma final, que não a usa). */
    private static boolean comMascara(AbstractClientPlayer jogador, String traje) {
        if (!TrajeIchigo.BANKAI.equals(traje)) return false;
        long agora = jogador.level().getGameTime();
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.dono() == jogador.getId() && !v.encerrado()
                    && TrajeIchigo.mascaraNoRosto(v.tipo(), v.idade(agora, 0), v.duracao())) return true;
        }
        return false;
    }

    // --- A máscara Hollow: uma peça presa à cabeça, desenhada por cima do traje ---

    public static final ModelLayerLocation MASCARA = new ModelLayerLocation(MineRWK.id("mascara_hollow"), "main");
    private static final ResourceLocation TEXTURA_DA_MASCARA = MineRWK.id("textures/traje/ichigo_mascara.png");
    private static ModelPart mascara;

    /**
     * A forma da máscara (do Minezin, mesmo autor): uma placa abaulada que envolve as bochechas, olhos
     * oblíquos, dentes serrilhados sobre a boca escura, uma listra vermelha no meio e outras abrindo em leque
     * de um lado só.
     */
    public static void aoRegistrarCamadas(EntityRenderersEvent.RegisterLayerDefinitions evento) {
        evento.registerLayerDefinition(MASCARA, () -> {
            MeshDefinition malha = new MeshDefinition();
            CubeListBuilder b = CubeListBuilder.create();
            b.texOffs(0, 0).addBox(-4.2f, -8.3f, -4.75f, 8.4f, 5.9f, .7f).addBox(-3.6f, -2.45f, -4.7f, 7.2f, 2.7f, .6f)
                    .addBox(-4.45f, -8f, -4.6f, .3f, 6.8f, 3.4f).addBox(4.15f, -8f, -4.6f, .3f, 6.8f, 3.4f)
                    .addBox(-3.4f, -8.6f, -4.6f, 6.8f, .35f, 2.6f);
            // Olhos oblíquos: fileiras finas subindo do canto de dentro até a ponta de fora.
            float[][] linhas = {{2.7f, 3.75f, -5.75f}, {2.0f, 3.7f, -5.45f}, {1.35f, 3.35f, -5.15f}, {1.0f, 2.6f, -4.85f}, {.9f, 1.6f, -4.55f}};
            for (int lado = -1; lado <= 1; lado += 2) {
                for (float[] l : linhas) {
                    b.texOffs(0, 16).addBox(lado > 0 ? l[0] : -l[1], l[2], -4.85f, l[1] - l[0], .32f, .2f);
                }
            }
            b.texOffs(0, 16).addBox(-3.3f, -1.7f, -4.82f, 6.6f, .75f, .15f);
            for (int k = 0; k < 8; k++) {
                float x = -3.2f + k * .82f;
                b.texOffs(0, 0).addBox(x, -2.0f, -4.9f, .6f, .45f, .15f).addBox(x + .15f, -1.55f, -4.9f, .3f, .3f, .15f)
                        .addBox(x + .35f, -.95f, -4.9f, .6f, .4f, .15f).addBox(x + .5f, -1.2f, -4.9f, .3f, .25f, .15f);
            }
            b.texOffs(18, 16).addBox(-1f, -8.3f, -4.92f, .6f, 6.1f, .2f).addBox(-3.6f, -7.9f, -4.92f, 2.6f, .45f, .2f)
                    .addBox(-4f, -7f, -4.92f, 2.8f, .45f, .2f).addBox(-4.2f, -6.1f, -4.92f, 2.8f, .4f, .2f)
                    .addBox(-4.2f, -4.1f, -4.92f, 3.2f, .45f, .2f).addBox(-4f, -3.3f, -4.92f, 2.6f, .4f, .2f)
                    .addBox(-3.4f, -.6f, -4.92f, 2.4f, .4f, .2f).addBox(-4.5f, -7.4f, -4.4f, .35f, .45f, 2.6f)
                    .addBox(-4.5f, -4.1f, -4.4f, .35f, .45f, 2.6f);
            malha.getRoot().addOrReplaceChild("mascara", b, PartPose.ZERO);
            return LayerDefinition.create(malha, 64, 64);
        });
    }

    private static void desenharMascara(PlayerModel<AbstractClientPlayer> modelo, PoseStack pose, MultiBufferSource buffer, int luz) {
        if (mascara == null || !modelo.head.visible) return;
        pose.pushPose();
        try {
            modelo.head.translateAndRotate(pose);
            mascara.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURA_DA_MASCARA)), luz, OverlayTexture.NO_OVERLAY);
        } finally {
            pose.popPose();
        }
    }

    private static Renderer renderer(String traje) {
        return RENDERERS.computeIfAbsent(traje, n -> new Renderer());
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
        mascara = evento.getEntityModels().bakeLayer(MASCARA).getChild("mascara");
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

    private static ItemStack item(String traje) {
        return ITENS.computeIfAbsent(traje, n -> new ItemStack(switch (n) {
            case TrajeIchigo.SHIKAI -> Itens.TRAJE_ICHIGO.get();
            case TrajeIchigo.BANKAI -> Itens.TRAJE_ICHIGO_BANKAI.get();
            case TrajeIchigo.MUGETSU -> Itens.TRAJE_ICHIGO_MUGETSU.get();
            case TrajeIchigo.HOLLOW -> Itens.TRAJE_ICHIGO_HOLLOW.get();
            default -> Itens.TRAJE_GOJO.get();
        }));
    }

    private static void desenhar(PlayerModel<AbstractClientPlayer> modelo, PoseStack pose, MultiBufferSource buffer, int luz,
                                 AbstractClientPlayer jogador, float passo, float passada, float parcial, float idade,
                                 float cabecaYaw, float cabecaPitch) {
        String traje = trajeDe(jogador);
        Renderer renderer = renderer(traje);
        renderer.soBraco = null;
        renderer.semVenda = semVenda(jogador);
        renderer.pano = jogador.getUUID();
        renderer.parcialDoPano = parcial;
        renderer.semArma = PersonagensVistos.visto(jogador.getId()).semArma();
        balancarBarra(renderer, jogador, idade);
        renderer.prepForRender(jogador, item(traje), EquipmentSlot.CHEST, modelo, buffer, parcial, passo, passada,
                cabecaYaw, cabecaPitch);
        renderer.renderToBuffer(pose, null, luz, OverlayTexture.NO_OVERLAY, -1);
        if (comMascara(jogador, traje)) desenharMascara(modelo, pose, buffer, luz);
    }

    /** Calcula o balanço da barra pela velocidade do jogador no referencial do corpo. */
    private static void balancarBarra(Renderer renderer, AbstractClientPlayer jogador, float idade) {
        double dx = jogador.getX() - jogador.xo, dy = jogador.getY() - jogador.yo, dz = jogador.getZ() - jogador.zo;
        float yaw = jogador.yBodyRot * Mth.DEG_TO_RAD;
        // Olhando para o sul (yaw 0), a frente é +z e a direita é -x.
        double frente = -dx * Mth.sin(yaw) + dz * Mth.cos(yaw);
        double lateral = -dx * Mth.cos(yaw) - dz * Mth.sin(yaw);
        renderer.barra.clear();
        for (Lado lado : Lado.values()) {
            for (int coluna = 0; coluna < lado.colunas(); coluna++) {
                for (int linha = 0; linha < 2; linha++) {
                    renderer.barra.put(TrajeGojo.ossoDaBarra(lado, coluna, linha),
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
        String traje = trajeDe(jogador);
        Renderer doTraje = renderer(traje);
        doTraje.soBraco = evento.getArm();
        doTraje.barra.clear();
        // O pano da espada continua vivo em primeira pessoa: é o que o jogador mais vê.
        doTraje.pano = jogador.getUUID();
        doTraje.parcialDoPano = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        doTraje.semArma = PersonagensVistos.visto(jogador.getId()).semArma();
        doTraje.prepForRender(jogador, item(traje), EquipmentSlot.CHEST, modelo, evento.getMultiBufferSource(), 1f, 0, 0, 0, 0);
        doTraje.renderToBuffer(evento.getPoseStack(), null, evento.getPackedLight(), OverlayTexture.NO_OVERLAY, -1);
        doTraje.soBraco = null;
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
