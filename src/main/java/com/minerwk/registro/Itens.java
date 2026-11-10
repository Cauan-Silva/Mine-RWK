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

    /** Invoca o Toji; feito na bancada com papel, lingote de ouro e espada de ferro. */
    public static final DeferredItem<com.minerwk.boss.ContratoToji> CONTRATO_TOJI =
            REGISTRO.register("contrato_toji", com.minerwk.boss.ContratoToji::new);

    public static void naAbaCriativa(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent evento) {
        if (evento.getTabKey() == net.minecraft.world.item.CreativeModeTabs.COMBAT) evento.accept(CONTRATO_TOJI.get());
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
