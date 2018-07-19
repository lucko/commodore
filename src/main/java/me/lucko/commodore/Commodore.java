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
import com.mojang.brigadier.tree.LiteralCommandNode;

import org.bukkit.command.Command;

import java.util.List;
import java.util.Objects;

/**
 * Utility for using Minecraft's 1.13 'brigadier' library in Bukkit plugins.
 */
public interface Commodore {

    /**
     * Gets the current command dispatcher instance.
     *
     * <p>CraftBukkit doesn't use the same dispatcher instance throughout
     * the runtime of the server. The dispatcher instance is completely wiped
     * (and replaced with a new instace) every time new plugins are loaded.</p>
     *
     * @return the command dispatcher
     */
    CommandDispatcher getDispatcher();

    /**
     * Gets a list of all nodes registered to the {@link CommandDispatcher} by
     * this instance.
     *
     * <p>Bear in mind that registrations are pushed to the dispatcher with a 1
     * tick delay. Nodes will not appear in this list until they have actually
     * been registered.</p>
     *
     * @return a list of all registered nodes.
     */
    List<LiteralCommandNode<?>> getRegisteredNodes();

    /**
     * Registers the provided argument data to the dispatcher.
     *
     * <p>Equivalent to calling
     * {@link CommandDispatcher#register(LiteralArgumentBuilder)}.</p>
     *
     * <p>The registration action occurs after a delay of one tick. This is
     * because the dispatcher instance is completely replaced after all plugins
     * have been loaded.</p>
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
     * <p>The registration action occurs after a delay of one tick. This is
     * because the dispatcher instance is completely replaced after all plugins
     * have been loaded.</p>
     *
     * @param argumentBuilder the argument data
     */
    default void register(LiteralArgumentBuilder<?> argumentBuilder) {
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        register(argumentBuilder.build());
    }

    /**
     * Registers the provided argument data to the dispatcher, against all
     * aliases defined for the {@code command}.
     *
     * @param command the command to read aliases from
     * @param node the argument data
     */
    void register(Command command, LiteralCommandNode<?> node);

    /**
     * Registers the provided argument data to the dispatcher, against all
     * aliases defined for the {@code command}.
     *
     * @param command the command to read aliases from
     * @param argumentBuilder the argument data, in a builder form
     */
    default void register(Command command, LiteralArgumentBuilder<?> argumentBuilder) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        register(command, argumentBuilder.build());
    }

}
