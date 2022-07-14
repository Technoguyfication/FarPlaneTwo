/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.client.network.play;

import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.client.network.play.IMixinClientPlayNetHandler1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.player.FarPlayerClient1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.world.FWorldClient1_16;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(ClientPlayNetHandler.class)
public abstract class MixinClientPlayNetHandler1_16 implements IMixinClientPlayNetHandler1_16 {
    @Unique
    protected FWorldClient1_16 fp2_worldClient;
    @Unique
    protected FarPlayerClient1_16 fp2_playerClient;

    @Override
    public void fp2_initClient() {
        checkState(this.fp2_worldClient == null && this.fp2_playerClient == null, "already initialized!");

        this.fp2_worldClient = new FWorldClient1_16((FP2Forge1_16) fp2(), uncheckedCast(this)).init();
        this.fp2_playerClient = new FarPlayerClient1_16((FP2Forge1_16) fp2(), this.fp2_worldClient, uncheckedCast(this));
    }

    @Override
    public void fp2_closeClient() {
        if (this.fp2_worldClient == null && this.fp2_playerClient == null) {
            fp2().log().alert("attempted to close client world, but it was either not initialized or already closed!");
            return;
        }

        //try-with-resources to ensure everything is closed
        try (FWorldClient1_16 worldClient = this.fp2_worldClient;
             FarPlayerClient1_16 playerClient = this.fp2_playerClient) {
            this.fp2_worldClient = null;
            this.fp2_playerClient = null;
        }
    }

    @Override
    public Optional<FWorldClient1_16> fp2_worldClient() {
        return Optional.ofNullable(this.fp2_worldClient);
    }

    @Override
    public Optional<FarPlayerClient1_16> fp2_playerClient() {
        return Optional.ofNullable(this.fp2_playerClient);
    }

    @Inject(method = "Lnet/minecraft/client/network/play/ClientPlayNetHandler;<init>*",
            at = @At("RETURN"),
            require = 1, allow = 1)
    private void fp2_$init$_doInit(CallbackInfo ci) {
        this.fp2_initClient();
    }

    @Inject(method = "Lnet/minecraft/client/network/play/ClientPlayNetHandler;cleanup()V",
            at = @At("HEAD"),
            require = 1, allow = 1)
    private void fp2_cleanup_doClose(CallbackInfo ci) {
        this.fp2_closeClient();
    }

    @Inject(
            method = {
                    "Lnet/minecraft/client/network/play/ClientPlayNetHandler;handleLogin(Lnet/minecraft/network/play/server/SJoinGamePacket;)V",
                    "Lnet/minecraft/client/network/play/ClientPlayNetHandler;handleRespawn(Lnet/minecraft/network/play/server/SRespawnPacket;)V"
            },
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/client/entity/player/ClientPlayerEntity;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER))
    private void fp2_notifyContextReady(CallbackInfo ci) {
        fp2().client().currentPlayer().ifPresent(IFarPlayerClient::ready);
    }
}
