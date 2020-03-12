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

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;

/**
 * Some useful {@link SuggestionProvider} instances.
 */
public final class Suggestions {
    private Suggestions() {}

    private static final SuggestionProvider<?> DONT_ASK_SERVER = (context, builder) -> { throw new RuntimeException(); };

    /**
     * Returns a {@link SuggestionProvider} that when added to a {@link ArgumentCommandNode},
     * will mark that the server should not be asked for suggestions for that argument.
     *
     * @param <S> the sender type
     * @return the suggestion provider
     */
    @SuppressWarnings("unchecked")
    public static <S> SuggestionProvider<S> dontAskServer() {
        return (SuggestionProvider<S>) DONT_ASK_SERVER;
    }
}
