package com.Zhu_Yii.zylob;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(Zylob.MODID)
public class Zylob {
    public static final String MODID = "zylob";



    // 使用更现代的构造方式
    public Zylob(FMLJavaModLoadingContext context) {
        // 获取统一的事件总线实例
        IEventBus modEventBus = context.getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;


        // 事件监听器注册
        modEventBus.addListener(this::onCommonSetup);

        // Forge 事件总线注册
        MinecraftForge.EVENT_BUS.register(this);

    }



    public void addPackFindersEvent(AddPackFindersEvent event) {
    }


    // 在主类初始化时注册
    public void onCommonSetup(FMLCommonSetupEvent event) {



    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 服务器启动逻辑
    }

    // ========================
    //     客户端事件订阅
    // ========================
    @Mod.EventBusSubscriber(
            modid = MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}