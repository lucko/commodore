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
import me.lucko.commodore.file.Lexer.ConstantToken;
import me.lucko.commodore.file.Lexer.StringToken;
import me.lucko.commodore.file.Lexer.Token;

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
                return IntegerArgumentType.integer();
            case "brigadier:long":
                return LongArgumentType.longArg();
            case "brigadier:float":
                return FloatArgumentType.floatArg();
            case "brigadier:double":
                return DoubleArgumentType.doubleArg();
            default:
                throw new ParserException(this, "Unknown argument type: " + argumentType);
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

    /**
     * Exception thrown if parsing fails.
     */
    static final class ParserException extends RuntimeException {
        private ParserException(Parser<?> parser, String message) {
            super(message + " (at line " + parser.lexer.currentLine() + ")");
        }
    }
}
