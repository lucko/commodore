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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility for using Minecraft's 1.13 'brigadier' library in Bukkit plugins.
 */
public interface Commodore {

    /**
     * Registers the provided argument data to the dispatcher, against all
     * aliases defined for the {@code command}.
     *
     * <p>Additionally applies the CraftBukkit {@link SuggestionProvider}
     * to all arguments within the node, so ASK_SERVER suggestions can continue
     * to function for the command.</p>
     *
     * <p>Players will only be sent argument data if they pass the provided
     * {@code permissionTest}.</p>
     *
     * @param command the command to read aliases from
     * @param node the argument data
     * @param permissionTest the predicate to check whether players should be sent argument data
     */
    void register(Command command, LiteralCommandNode<?> node, Predicate<? super Player> permissionTest);

    /**
     * Registers the provided argument data to the dispatcher, against all
     * aliases defined for the {@code command}.
     *
     * <p>Additionally applies the CraftBukkit {@link SuggestionProvider}
     * to all arguments within the node, so ASK_SERVER suggestions can continue
     * to function for the command.</p>
     *
     * <p>Players will only be sent argument data if they pass the provided
     * {@code permissionTest}.</p>
     *
     * @param command the command to read aliases from
     * @param argumentBuilder the argument data, in a builder form
     * @param permissionTest the predicate to check whether players should be sent argument data
     */
    default void register(Command command, LiteralArgumentBuilder<?> argumentBuilder, Predicate<? super Player> permissionTest) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        Objects.requireNonNull(permissionTest, "permissionTest");
        register(command, argumentBuilder.build(), permissionTest);
    }

    /**
     * Registers the provided argument data to the dispatcher, against all
     * aliases defined for the {@code command}.
     *
     * <p>Additionally applies the CraftBukkit {@link SuggestionProvider}
     * to all arguments within the node, so ASK_SERVER suggestions can continue
     * to function for the command.</p>
     *
     * @param command the command to read aliases from
     * @param node the argument data
     */
    default void register(Command command, LiteralCommandNode<?> node) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(node, "node");
        register(command, node, command::testPermissionSilent);
    }

    /**
     * Registers the provided argument data to the dispatcher, against all
     * aliases defined for the {@code command}.
     *
     * <p>Additionally applies the CraftBukkit {@link SuggestionProvider}
     * to all arguments within the node, so ASK_SERVER suggestions can continue
     * to function for the command.</p>
     *
     * @param command the command to read aliases from
     * @param argumentBuilder the argument data, in a builder form
     */
    default void register(Command command, LiteralArgumentBuilder<?> argumentBuilder) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        register(command, argumentBuilder.build());
    }

    /**
     * Registers the provided argument data to the dispatcher.
     *
     * <p>Equivalent to calling
     * {@link CommandDispatcher#register(LiteralArgumentBuilder)}.</p>
     *
     * <p>Prefer using {@link #register(Command, LiteralCommandNode)}.</p>
     *
     * @param node the argument data
     */
    void register(LiteralCommandNode<?> node);

    /**
     * Registers the provided argument data to the dispatcher.
     *
     * <p>Equivalent to calling
     * {@link CommandDispatcher#register(LiteralArgumentBuilder)}.</p>
     *
     * <p>Prefer using {@link #register(Command, LiteralArgumentBuilder)}.</p>
     *
     * @param argumentBuilder the argument data
     */
    default void register(LiteralArgumentBuilder<?> argumentBuilder) {
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        register(argumentBuilder.build());
    }

}
