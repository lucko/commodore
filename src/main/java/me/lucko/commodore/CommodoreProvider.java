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

import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Factory for obtaining instances of {@link Commodore}.
 */
public final class CommodoreProvider {

    /**
     * Checks to see if the Brigadier command system is supported by the server.
     *
     * @return true if commodore is supported.
     */
    public static boolean isSupported() {
        try {
            Class.forName("com.mojang.brigadier.CommandDispatcher");
            CommodoreImpl.ensureSetup();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Obtains a {@link Commodore} instance for the given plugin.
     *
     * <p>Throws a {@link RuntimeException} if brigadier is not
     * {@link #isSupported() supported} by the server.</p>
     *
     * @param plugin the plugin
     * @return the commodore instance
     */
    public static Commodore getCommodore(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (!isSupported()) {
            throw new RuntimeException("Brigadier is not supported by the server.");
        }

        return new CommodoreImpl(plugin);
    }

    private CommodoreProvider() {
        throw new AssertionError();
    }
}
