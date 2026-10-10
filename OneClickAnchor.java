package com.example.oneclickanchor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OneClickAnchor implements ModInitializer {
    private static final Map<UUID, Integer> SAFE = new HashMap<>();

    @Override
    public void onInitialize() {
        Config.load();
        registerAnchor();
        registerMenu();
    }

    private void registerAnchor() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) return ActionResult.PASS;

            Config cfg = Config.INSTANCE;

            // Haath me glowstone ho to vanilla normal charge karega, hum kuch nahi karenge
            if (sp.getMainHandStack().isOf(Items.GLOWSTONE)) return ActionResult.PASS;

            if (cfg.oneClick && state.get(RespawnAnchorBlock.CHARGES) == 0) {
                if (cfg.consumeGlowstone) {
                    boolean found = false;
                    for (int i = 0; i < sp.getInventory().size(); i++) {
                        ItemStack s = sp.getInventory().getStack(i);
                        if (s.isOf(Items.GLOWSTONE)) {
                            s.decrement(1);
                            found = true;
                            break;
                        }
                    }
                    if (!found) return ActionResult.PASS;
                }
                world.setBlockState(pos, state.with(RespawnAnchorBlock.CHARGES, 1));
            }

            if (cfg.safeSelf) {
                SAFE.put(sp.getUuid(), sp.getServer().getTicks());
            }
            return ActionResult.PASS; // vanilla ab blast karega
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (Config.INSTANCE.safeSelf
                    && entity instanceof PlayerEntity p
                    && source.isOf(DamageTypes.BAD_RESPAWN_POINT)) {
                Integer t = SAFE.get(p.getUuid());
                if (t != null && p.getServer().getTicks() - t <= 1) return false; // damage cancel
            }
            return true;
        });
    }

    // ---------- Chat Menu: /anchormenu ----------
    private void registerMenu() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(CommandManager.literal("anchormenu")
                    // NOTE: 1.21.11 me permission API change hui hai. Error aaye to
                    // .requires(...) line hata do ya naye permission check se replace karo.
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> { sendMenu(ctx.getSource()); return 1; })
                    .then(CommandManager.literal("toggle")
                            .then(CommandManager.literal("oneclick").executes(ctx -> {
                                Config.INSTANCE.oneClick = !Config.INSTANCE.oneClick;
                                Config.save(); sendMenu(ctx.getSource()); return 1;
                            }))
                            .then(CommandManager.literal("safe").executes(ctx -> {
                                Config.INSTANCE.safeSelf = !Config.INSTANCE.safeSelf;
                                Config.save(); sendMenu(ctx.getSource()); return 1;
                            }))
                            .then(CommandManager.literal("consume").executes(ctx -> {
                                Config.INSTANCE.consumeGlowstone = !Config.INSTANCE.consumeGlowstone;
                                Config.save(); sendMenu(ctx.getSource()); return 1;
                            }))));
        });
    }

    private static void sendMenu(net.minecraft.server.command.ServerCommandSource src) {
        Config c = Config.INSTANCE;
        src.sendMessage(Text.literal("======= One Click Anchor Menu =======").formatted(Formatting.GOLD, Formatting.BOLD));
        src.sendMessage(row("1-Click Anchor", c.oneClick, "/anchormenu toggle oneclick"));
        src.sendMessage(row("Safe Self-Damage", c.safeSelf, "/anchormenu toggle safe"));
        src.sendMessage(row("Glowstone Consume", c.consumeGlowstone, "/anchormenu toggle consume"));
        src.sendMessage(Text.literal("(click karke ON/OFF karo)").formatted(Formatting.GRAY));
    }

    private static Text row(String label, boolean on, String cmd) {
        Text button = Text.literal(on ? "[ON]" : "[OFF]")
                .setStyle(Style.EMPTY
                        .withColor(on ? Formatting.GREEN : Formatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand(cmd)));
        return Text.literal(label + ": ").formatted(Formatting.WHITE).append(button);
    }
}
