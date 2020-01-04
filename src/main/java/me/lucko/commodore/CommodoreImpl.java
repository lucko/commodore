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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final Plugin plugin;
    private final List<LiteralCommandNode<?>> registeredNodes = new ArrayList<>();

    CommodoreImpl(Plugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(new ServerReloadListener(), this.plugin);
    }

    @Override
    public CommandDispatcher<?> getDispatcher() {
        try {
            Object mcServerObject = CONSOLE_FIELD.get(Bukkit.getServer());
            Object commandDispatcherObject = GET_COMMAND_DISPATCHER_METHOD.invoke(mcServerObject);
            return (CommandDispatcher<?>) GET_BRIGADIER_DISPATCHER_METHOD.invoke(commandDispatcherObject);
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
    public void register(Command command, LiteralCommandNode<?> node, Predicate<? super Player> permissionTest) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(permissionTest, "permissionTest");

        register(command, node);
        this.plugin.getServer().getPluginManager().registerEvents(new PermissionListener(command, permissionTest), this.plugin);
    }

    @Override
    public void register(Command command, LiteralCommandNode<?> node) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(node, "node");

        try {
            SuggestionProvider<?> wrapper = (SuggestionProvider<?>) COMMAND_WRAPPER_CONSTRUCTOR.newInstance(Bukkit.getServer(), command);
            setCustomSuggestionProvider(node, wrapper);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        for (String alias : Commodore.getAliases(command)) {
            if (node.getLiteral().equals(alias)) {
                register(node);
            } else {
                register(renameLiteralNode(node, alias));
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void register(LiteralCommandNode<?> node) {
        Objects.requireNonNull(node, "node");

        CommandDispatcher dispatcher = getDispatcher();
        dispatcher.getRoot().addChild(node);
        this.registeredNodes.add(node);
    }

    private static void setCustomSuggestionProvider(CommandNode<?> node, SuggestionProvider<?> suggestionProvider) {
        if (node instanceof ArgumentCommandNode) {
            ArgumentCommandNode<?, ?> argumentNode = (ArgumentCommandNode<?, ?>) node;
            try {
                CUSTOM_SUGGESTIONS_FIELD.set(argumentNode, suggestionProvider);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // apply recursively to child nodes
        for (CommandNode<?> child : node.getChildren()) {
            setCustomSuggestionProvider(child, suggestionProvider);
        }
    }

    private static <S> LiteralCommandNode<S> renameLiteralNode(LiteralCommandNode<S> node, String newLiteral) {
        LiteralCommandNode<S> clone = new LiteralCommandNode<>(newLiteral, node.getCommand(), node.getRequirement(), node.getRedirect(), node.getRedirectModifier(), node.isFork());
        for (CommandNode<S> child : node.getChildren()) {
            clone.addChild(child);
        }
        return clone;
    }

    /**
     * Listens for server (re)loads, and re-adds all registered nodes to the dispatcher.
     */
    private final class ServerReloadListener implements Listener {
        @SuppressWarnings({"rawtypes", "unchecked"})
        @EventHandler
        public void onLoad(ServerLoadEvent e) {
            CommandDispatcher dispatcher = getDispatcher();
            for (LiteralCommandNode<?> node : CommodoreImpl.this.registeredNodes) {
                dispatcher.getRoot().addChild(node);
            }
        }
    }

    /**
     * Removes argument data for players without permission to view them.
     */
    private static final class PermissionListener implements Listener {
        private final List<String> aliases;
        private final Predicate<? super Player> permissionTest;

        PermissionListener(Command pluginCommand, Predicate<? super Player> permissionTest) {
            this.aliases = Commodore.getAliases(pluginCommand).stream()
                    .flatMap(alias -> Stream.of(alias, "minecraft:" + alias))
                    .collect(Collectors.toList());
            this.permissionTest = permissionTest;
        }

        @EventHandler
        public void onCommandSend(PlayerCommandSendEvent e) {
            if (!this.permissionTest.test(e.getPlayer())) {
                e.getCommands().removeAll(this.aliases);
            }
        }
    }

    static void ensureSetup() {
        // do nothing - this is only called to trigger the static initializer
    }

}
