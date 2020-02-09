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
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.commodore.MinecraftArgumentTypes;
import me.lucko.commodore.file.Lexer.ConstantToken;
import me.lucko.commodore.file.Lexer.StringToken;
import me.lucko.commodore.file.Lexer.Token;
import org.bukkit.NamespacedKey;

import java.util.Arrays;

/**
 * A parser for the {@link CommodoreFileFormat}.
 *
 * <p>Transforms a list of {@link Token}s into a command node parse tree.</p>
 *
 * @param <S> the command node sender type
 */
class Parser<S> {
    private final Lexer lexer;

    Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Main parsing method.
     *
     * @return the parsed node
     */
    LiteralCommandNode<S> parse() {
        CommandNode<S> node = parseNode();
        if (!(node instanceof LiteralCommandNode)) {
            throw new ParserException(this, "Root command node is not a literal command node");
        }

        if (this.lexer.peek() != ConstantToken.EOF) {
            throw new ParserException(this, "Expected end of file but got " + this.lexer.peek());
        }
        return (LiteralCommandNode<S>) node;
    }

    private CommandNode<S> parseNode() {
        Token token = this.lexer.next();
        if (!(token instanceof StringToken)) {
            throw new ParserException(this, "Expected string token for node name but got " + token);
        }

        String name = ((StringToken) token).string;
        ArgumentBuilder<S, ?> node;

        if (this.lexer.peek() instanceof StringToken) {
            node = RequiredArgumentBuilder.argument(name, parseArgumentType());
        } else {
            node = LiteralArgumentBuilder.literal(name);
        }

        if (this.lexer.peek() == ConstantToken.OPEN_BRACKET) {
            this.lexer.next();
            while (this.lexer.peek() != ConstantToken.CLOSE_BRACKET) {
                CommandNode<S> child = parseNode();
                node.then(child);
            }
            this.lexer.next();
        } else {
            if (this.lexer.peek() != ConstantToken.SEMICOLON) {
                throw new ParserException(this, "Node definition not ended with semicolon, got " + this.lexer.peek());
            }
            this.lexer.next();
        }

        return node.build();
    }

    private ArgumentType<?> parseArgumentType() {
        Token token = this.lexer.next();
        if (!(token instanceof StringToken)) {
            throw new ParserException(this, "Expected string token for argument type but got " + token);
        }

        String argumentType = ((StringToken) token).string;
        switch (argumentType) {
            case "brigadier:bool":
                return BoolArgumentType.bool();
            case "brigadier:string":
                return parseStringArgumentType();
            case "brigadier:integer":
                return parseIntegerArgumentType();
            case "brigadier:long":
                return parseLongArgumentType();
            case "brigadier:float":
                return parseFloatArgumentType();
            case "brigadier:double":
                return parseDoubleArgumentType();
            default:
                return parseMinecraftArgumentType(argumentType);
        }
    }

    @SuppressWarnings("deprecation")
    private ArgumentType<?> parseMinecraftArgumentType(String argumentType) {
        String[] key = argumentType.split(":");
        if (key.length != 2) {
            throw new ParserException(this, "Invalid key for argument type: " + Arrays.toString(key));
        }

        NamespacedKey namespacedKey;
        try {
            namespacedKey = new NamespacedKey(key[0], key[1]);
        } catch (IllegalArgumentException e) {
            throw new ParserException(this, "Invalid key for argument type: " + argumentType, e);
        }

        try {
            return MinecraftArgumentTypes.getByKey(namespacedKey);
        } catch (Throwable e) {
            throw new ParserException(this, "Invalid key for argument type (not found in registry): " + argumentType, e);
        }
    }

    private StringArgumentType parseStringArgumentType() {
        Token token = this.lexer.next();
        if (!(token instanceof StringToken)) {
            throw new ParserException(this, "Expected string token for string type but got " + token);
        }

        String stringType = ((StringToken) token).string;
        switch (stringType) {
            case "single_word":
                return StringArgumentType.word();
            case "quotable_phrase":
                return StringArgumentType.string();
            case "greedy_phrase":
                return StringArgumentType.greedyString();
            default:
                throw new ParserException(this, "Unknown string type: " + stringType);
        }
    }

    private IntegerArgumentType parseIntegerArgumentType() {
        if (this.lexer.peek() instanceof StringToken) {
            int min = parseInt();
            if (this.lexer.peek() instanceof StringToken) {
                int max = parseInt();
                return IntegerArgumentType.integer(min, max);
            }
            return IntegerArgumentType.integer(min);
        }
        return IntegerArgumentType.integer();
    }
    
    private int parseInt() {
        Token token = this.lexer.next();
        if (!(token instanceof StringToken)) {
            throw new ParserException(this, "Expected string token for integer but got " + token);
        }
        String value = ((StringToken) token).string;

        if (value.equals("min")) {
            return Integer.MIN_VALUE;
        }
        if (value.equals("max")) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ParserException(this, "Expected int but got " + value, e);
        }
    }

    private LongArgumentType parseLongArgumentType() {
        if (this.lexer.peek() instanceof StringToken) {
            long min = parseLong();
            if (this.lexer.peek() instanceof StringToken) {
                long max = parseLong();
                return LongArgumentType.longArg(min, max);
            }
            return LongArgumentType.longArg(min);
        }
        return LongArgumentType.longArg();
    }

    private long parseLong() {
        Token token = this.lexer.next();
        if (!(token instanceof StringToken)) {
            throw new ParserException(this, "Expected string token for long but got " + token);
        }
        String value = ((StringToken) token).string;

        if (value.equals("min")) {
            return Long.MIN_VALUE;
        }
        if (value.equals("max")) {
            return Long.MAX_VALUE;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ParserException(this, "Expected long but got " + value, e);
        }
    }

    private FloatArgumentType parseFloatArgumentType() {
        if (this.lexer.peek() instanceof StringToken) {
            float min = parseFloat();
            if (this.lexer.peek() instanceof StringToken) {
                float max = parseFloat();
                return FloatArgumentType.floatArg(min, max);
            }
            return FloatArgumentType.floatArg(min);
        }
        return FloatArgumentType.floatArg();
    }

    private float parseFloat() {
        Token token = this.lexer.next();
        if (!(token instanceof StringToken)) {
            throw new ParserException(this, "Expected string token for float but got " + token);
        }
        String value = ((StringToken) token).string;

        if (value.equals("min")) {
            return Float.MIN_VALUE;
        }
        if (value.equals("max")) {
            return Float.MAX_VALUE;
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new ParserException(this, "Expected float but got " + value, e);
        }
    }

    private DoubleArgumentType parseDoubleArgumentType() {
        if (this.lexer.peek() instanceof StringToken) {
            double min = parseDouble();
            if (this.lexer.peek() instanceof StringToken) {
                double max = parseDouble();
                return DoubleArgumentType.doubleArg(min, max);
            }
            return DoubleArgumentType.doubleArg(min);
        }
        return DoubleArgumentType.doubleArg();
    }

    private double parseDouble() {
        Token token = this.lexer.next();
        if (!(token instanceof StringToken)) {
            throw new ParserException(this, "Expected string token for double but got " + token);
        }
        String value = ((StringToken) token).string;

        if (value.equals("min")) {
            return Double.MIN_VALUE;
        }
        if (value.equals("max")) {
            return Double.MAX_VALUE;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ParserException(this, "Expected double but got " + value);
        }
    }

    /**
     * Exception thrown if parsing fails.
     */
    static final class ParserException extends RuntimeException {
        private ParserException(Parser<?> parser, String message) {
            super(message + " (at line " + parser.lexer.currentLine() + ")");
        }

        private ParserException(Parser<?> parser, String message, Throwable cause) {
            super(message + " (at line " + parser.lexer.currentLine() + ")", cause);
        }
    }
}
