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

package me.lucko.commodore.file;

import com.mojang.brigadier.arguments.ArgumentType;

import me.lucko.commodore.MinecraftArgumentTypes;

import org.bukkit.NamespacedKey;

import java.lang.reflect.Constructor;

/**
 * An {@link ArgumentTypeParser} for Minecraft argument types.
 */
public class MinecraftArgumentTypeParser implements ArgumentTypeParser {
    public static final MinecraftArgumentTypeParser INSTANCE = new MinecraftArgumentTypeParser();

    private MinecraftArgumentTypeParser() {

    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canParse(String namespace, String name) {
        if (namespace.equals("minecraft") && (name.equals("entity") || name.equals("score_holder"))) {
            return true;
        }
        return MinecraftArgumentTypes.isRegistered(new NamespacedKey(namespace, name));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArgumentType<?> parse(String namespace, String name, TokenStream tokens) throws ParseException {
        if (namespace.equals("minecraft")) {
            if (name.equals("entity")) {
                return parseEntityArgumentType(tokens);
            }
            if (name.equals("score_holder")) {
                return parseScoreHolderArgumentType(tokens);
            }
        }

        try {
            return MinecraftArgumentTypes.getByKey(new NamespacedKey(namespace, name));
        } catch (Throwable e) {
            throw tokens.createException("Invalid key for argument type (not found in registry): " + namespace + ":" + name, e);
        }
    }

    private ArgumentType<?> parseEntityArgumentType(TokenStream tokens) throws ParseException {
        Token token = tokens.next();
        if (!(token instanceof Token.StringToken)) {
            throw tokens.createException("Expected string token for entity selection type but got " + token);
        }

        boolean single;
        boolean playersOnly;

        String entitySelectionType = ((Token.StringToken) token).getString();
        switch (entitySelectionType) {
            case "entity":
                single = true;
                playersOnly = false;
                break;
            case "entities":
                single = false;
                playersOnly = false;
                break;
            case "player":
                single = true;
                playersOnly = true;
                break;
            case "players":
                single = false;
                playersOnly = true;
                break;
            default:
                throw tokens.createException("Unknown entity selection type: " + entitySelectionType);
        }

        return constructMinecraftArgumentType(NamespacedKey.minecraft("entity"), new Class[]{boolean.class, boolean.class}, single, playersOnly);
    }

    private static ArgumentType<?> parseScoreHolderArgumentType(TokenStream tokens) throws ParseException {
        boolean multiple = false;
        if (tokens.peek() instanceof Token.StringToken) {
            multiple = parseBoolean(tokens);
        }

        return constructMinecraftArgumentType(NamespacedKey.minecraft("score_holder"), new Class[]{boolean.class}, multiple);
    }

    private static ArgumentType<?> constructMinecraftArgumentType(NamespacedKey key, Class<?>[] argTypes, Object... args) {
        try {
            final Constructor<? extends ArgumentType<?>> constructor = MinecraftArgumentTypes.getClassByKey(key).getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean parseBoolean(TokenStream tokens) throws ParseException {
        Token token = tokens.next();
        if (!(token instanceof Token.StringToken)) {
            throw tokens.createException("Expected string token for boolean but got " + token);
        }
        String value = ((Token.StringToken) token).getString();

        if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else {
            throw tokens.createException("Expected true/false but got " + value);
        }
    }

}
