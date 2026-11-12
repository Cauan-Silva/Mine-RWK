package com.minerwk.registro;

import com.minerwk.MineRWK;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Itens do mod. Os trajes são itens escondidos: ninguém os recebe; servem só para o GeckoLib desenhar o modelo. */
public final class Itens {
    public static final DeferredRegister.Items REGISTRO = DeferredRegister.createItems(MineRWK.MOD_ID);

    /** O traje do Gojo. O jogador não veste o item: o cliente desenha o modelo em quem escolheu o Gojo. */
    public static final DeferredItem<Traje> TRAJE_GOJO = REGISTRO.register("traje_gojo", () -> new Traje("gojo"));

    /** Os trajes do Ichigo: o shihakushō com a Zangetsu e o casaco do Bankai com a Tensa Zangetsu. */
    public static final DeferredItem<Traje> TRAJE_ICHIGO = REGISTRO.register("traje_ichigo", () -> new Traje("ichigo"));
    public static final DeferredItem<Traje> TRAJE_ICHIGO_BANKAI =
            REGISTRO.register("traje_ichigo_bankai", () -> new Traje("ichigo_bankai"));
    /** A forma final do Mugetsu, vestida só durante a cutscene e o corte. */
    public static final DeferredItem<Traje> TRAJE_ICHIGO_MUGETSU =
            REGISTRO.register("traje_ichigo_mugetsu", () -> new Traje("ichigo_mugetsu"));
    /** A forma Hollow completa, vestida nos 10 s em que o Hollow toma o corpo. */
    public static final DeferredItem<Traje> TRAJE_ICHIGO_HOLLOW =
            REGISTRO.register("traje_ichigo_hollow", () -> new Traje("ichigo_hollow"));
    /** O traje do Shadow, com a espada de slime. */
    public static final DeferredItem<Traje> TRAJE_CID = REGISTRO.register("traje_cid", () -> new Traje("cid"));

    /** Invoca o Toji; feito na bancada com papel, lingote de ouro e espada de ferro. */
    public static final DeferredItem<com.minerwk.boss.ContratoToji> CONTRATO_TOJI =
            REGISTRO.register("contrato_toji", com.minerwk.boss.ContratoToji::new);

    /** Leva ao Zangetsu interior; feita na bancada com osso, pérola do Ender e espada de ferro. */
    public static final DeferredItem<com.minerwk.boss.MascaraRachada> MASCARA_RACHADA =
            REGISTRO.register("mascara_rachada", com.minerwk.boss.MascaraRachada::new);
    /** Abre a Rosa Negra e leva ao Ragnarok; feito na bancada com ouro, ametista e pérola do Ender. */
    public static final DeferredItem<com.minerwk.boss.AnelDaSucessao> ANEL_DA_SUCESSAO =
            REGISTRO.register("anel_da_sucessao", com.minerwk.boss.AnelDaSucessao::new);

    public static void naAbaCriativa(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent evento) {
        if (evento.getTabKey() == net.minecraft.world.item.CreativeModeTabs.COMBAT) {
            evento.accept(CONTRATO_TOJI.get());
            evento.accept(MASCARA_RACHADA.get());
            evento.accept(ANEL_DA_SUCESSAO.get());
        }
    }

    private Itens() {
    }

    /** Um traje: só o que o GeckoLib precisa para animar e desenhar o modelo de {@code geo/traje/<nome>}. */
    public static final class Traje extends Item implements GeoItem {
        private final String nome;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        Traje(String nome) {
            super(new Item.Properties().stacksTo(1));
            this.nome = nome;
        }

        public String nome() {
            return nome;
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
            // Sem animação própria: o traje segue os ossos do modelo do jogador.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }
}
