/*
 * This file is part of commodore, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.commodore;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class CommodoreImpl implements Commodore {

    // obc.CraftServer#console field
    private static final Field CONSOLE_FIELD;

    // nms.MinecraftServer#getCommandDispatcher method
    private static final Method GET_COMMAND_DISPATCHER_METHOD;

    // nms.CommandDispatcher#getDispatcher (obfuscated) method
    private static final Method GET_BRIGADIER_DISPATCHER_METHOD;

    static {
        try {
            Class<?> craftServer = ReflectionUtil.obcClass("CraftServer");
            CONSOLE_FIELD = craftServer.getDeclaredField("console");
            CONSOLE_FIELD.setAccessible(true);

            Class<?> minecraftServer = ReflectionUtil.nmsClass("MinecraftServer");
            GET_COMMAND_DISPATCHER_METHOD = minecraftServer.getDeclaredMethod("getCommandDispatcher");
            GET_COMMAND_DISPATCHER_METHOD.setAccessible(true);

            Class<?> commandDispatcher = ReflectionUtil.nmsClass("CommandDispatcher");
            Method getBrigadierDispatcherMethod = null;
            for (Method method : commandDispatcher.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && CommandDispatcher.class.isAssignableFrom(method.getReturnType())) {
                    getBrigadierDispatcherMethod = method;
                    break;
                }
            }
            GET_BRIGADIER_DISPATCHER_METHOD = Objects.requireNonNull(getBrigadierDispatcherMethod, "getBrigadierDispatcherMethod");
            GET_BRIGADIER_DISPATCHER_METHOD.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void ensureSetup() {
        // do nothing - this is only called to trigger the static initializer
    }

    private final Plugin plugin;
    private final Object lock = new Object();

    private final List<LiteralCommandNode<?>> registeredNodes = new ArrayList<>();
    private List<LiteralCommandNode<?>> pendingRegistration = null;

    CommodoreImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandDispatcher getDispatcher() {
        try {
            Object mcServerObject = CONSOLE_FIELD.get(Bukkit.getServer());
            Object commandDispatcherObject = GET_COMMAND_DISPATCHER_METHOD.invoke(mcServerObject);
            return (CommandDispatcher) GET_BRIGADIER_DISPATCHER_METHOD.invoke(commandDispatcherObject);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LiteralCommandNode<?>> getRegisteredNodes() {
        return Collections.unmodifiableList(this.registeredNodes);
    }

    private void flush() {
        CommandDispatcher dispatcher = getDispatcher();

        synchronized (this.lock) {
            if (this.pendingRegistration == null) {
                return;
            }

            for (LiteralCommandNode<?> node : this.pendingRegistration) {
                //noinspection unchecked
                dispatcher.getRoot().addChild(node);
                this.registeredNodes.add(node);
            }
            this.pendingRegistration = null;
        }
    }

    @Override
    public void register(LiteralCommandNode<?> node) {
        Objects.requireNonNull(node, "node");

        synchronized (this.lock) {
            if (this.pendingRegistration == null) {
                this.pendingRegistration = new ArrayList<>();
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, this::flush, 1L);
            }
            this.pendingRegistration.add(node);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(Command command, LiteralCommandNode<?> node) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(node, "node");

        for (String alias : Lists.asList(command.getLabel(), command.getAliases().toArray(new String[0]))) {
            LiteralCommandNode<?> toRegister = node;
            if (!node.getLiteral().equals(alias)) {
                if (!node.getLiteral().equals(alias)) {
                    LiteralCommandNode clone = new LiteralCommandNode(alias, node.getCommand(), node.getRequirement(), node.getRedirect(), node.getRedirectModifier(), node.isFork());
                    for (CommandNode child : node.getChildren()) {
                        clone.addChild(child);
                    }
                    toRegister = clone;
                }
            }
            register(toRegister);
        }
    }

}
