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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class CommodoreImpl implements Commodore {

    // obc.CraftServer#console field
    private static final Field CONSOLE_FIELD;

    // nms.MinecraftServer#getCommandDispatcher method
    private static final Method GET_COMMAND_DISPATCHER_METHOD;

    // nms.CommandListenerWrapper#getBukkitSender method
    private static final Method GET_BUKKIT_SENDER_METHOD;

    // nms.CommandDispatcher#getDispatcher (obfuscated) method
    private static final Method GET_BRIGADIER_DISPATCHER_METHOD;

    // obc.command.BukkitCommandWrapper constructor
    private static final Constructor<?> COMMAND_WRAPPER_CONSTRUCTOR;

    // ArgumentCommandNode#customSuggestions field
    private static final Field CUSTOM_SUGGESTIONS_FIELD;


    static {
        try {
            Class<?> craftServer = ReflectionUtil.obcClass("CraftServer");
            CONSOLE_FIELD = craftServer.getDeclaredField("console");
            CONSOLE_FIELD.setAccessible(true);

            Class<?> minecraftServer = ReflectionUtil.nmsClass("MinecraftServer");
            GET_COMMAND_DISPATCHER_METHOD = minecraftServer.getDeclaredMethod("getCommandDispatcher");
            GET_COMMAND_DISPATCHER_METHOD.setAccessible(true);

            Class<?> commandListenerWrapper = ReflectionUtil.nmsClass("CommandListenerWrapper");
            GET_BUKKIT_SENDER_METHOD = commandListenerWrapper.getDeclaredMethod("getBukkitSender");
            GET_BUKKIT_SENDER_METHOD.setAccessible(true);

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

            Class<?> commandWrapperClass = ReflectionUtil.obcClass("command.BukkitCommandWrapper");
            COMMAND_WRAPPER_CONSTRUCTOR = commandWrapperClass.getConstructor(craftServer, Command.class);

            CUSTOM_SUGGESTIONS_FIELD = ArgumentCommandNode.class.getDeclaredField("customSuggestions");
            CUSTOM_SUGGESTIONS_FIELD.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void ensureSetup() {
        // do nothing - this is only called to trigger the static initializer
    }

    private final List<LiteralCommandNode<?>> registeredNodes = new ArrayList<>();

    CommodoreImpl(Plugin plugin) {
        // add all of our nodes when the server (re)loads - the dispatcher is replaced
        // when this happens
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onLoad(ServerLoadEvent e) {
                CommandDispatcher dispatcher = getDispatcher();
                for (LiteralCommandNode<?> node : CommodoreImpl.this.registeredNodes) {
                    //noinspection unchecked
                    dispatcher.getRoot().addChild(node);
                }
            }

        }, plugin);
    }

    @Override
    public CommandDispatcher<?> getDispatcher() {
        try {
            Object mcServerObject = CONSOLE_FIELD.get(Bukkit.getServer());
            Object commandDispatcherObject = GET_COMMAND_DISPATCHER_METHOD.invoke(mcServerObject);
            return (CommandDispatcher) GET_BRIGADIER_DISPATCHER_METHOD.invoke(commandDispatcherObject);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommandSender getBukkitSender(Object commandWrapperListener) {
        Objects.requireNonNull(commandWrapperListener, "commandWrapperListener");
        try {
            return (CommandSender) GET_BUKKIT_SENDER_METHOD.invoke(commandWrapperListener);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LiteralCommandNode<?>> getRegisteredNodes() {
        return Collections.unmodifiableList(this.registeredNodes);
    }

    @Override
    public void register(LiteralCommandNode<?> node) {
        Objects.requireNonNull(node, "node");

        CommandDispatcher<?> dispatcher = getDispatcher();
        //noinspection unchecked
        dispatcher.getRoot().addChild((CommandNode) node);
        this.registeredNodes.add(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(Command command, LiteralCommandNode<?> node) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(node, "node");

        try {
            SuggestionProvider wrapper = (SuggestionProvider) COMMAND_WRAPPER_CONSTRUCTOR.newInstance(Bukkit.getServer(), command);
            applyServerSuggestionCall(wrapper, Collections.singleton(node));
        } catch (Throwable e) {
            e.printStackTrace();
        }

        for (String alias : Commodore.getAliases(command)) {
            LiteralCommandNode<?> toRegister = node;
            if (!node.getLiteral().equals(alias)) {
                LiteralCommandNode clone = new LiteralCommandNode(alias, node.getCommand(), node.getRequirement(), node.getRedirect(), node.getRedirectModifier(), node.isFork());
                for (CommandNode child : node.getChildren()) {
                    clone.addChild(child);
                }
                toRegister = clone;
            }
            register(toRegister);
        }
    }

    private void applyServerSuggestionCall(SuggestionProvider suggestionProvider, Collection<? extends CommandNode<?>> nodes) {
        for (CommandNode<?> node : nodes) {
            if (node instanceof ArgumentCommandNode) {
                ArgumentCommandNode argumentNode = (ArgumentCommandNode) node;
                try {
                    CUSTOM_SUGGESTIONS_FIELD.set(argumentNode, suggestionProvider);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            // recursively.
            Collection<? extends CommandNode<?>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                applyServerSuggestionCall(suggestionProvider, children);
            }
        }
    }

}
