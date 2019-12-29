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

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The '.commodore' file format is a simplified way of representing Brigadier
 * command node trees in a string form.
 *
 * <p>This class provides a means to parse these files into real {@link CommandNode}s.</p>
 */
public final class CommodoreFileFormat {
    private CommodoreFileFormat() {}

    /**
     * Parses a {@link LiteralCommandNode} from a commodore file.
     *
     * @param reader a reader for the file
     * @param <S> the command node sender type
     * @return the command node
     * @throws IOException if an error occurs whilst reading the file
     * @throws RuntimeException if an error occurs whilst lexing or parsing the file
     */
    public static <S> LiteralCommandNode<S> parse(Reader reader) throws IOException {
        try {
            return new Parser<S>(new Lexer(reader)).parse();
        } catch (Lexer.LexerException e) {
            if (e.getCause() instanceof IOException) {
                throw ((IOException) e.getCause());
            }
            throw e;
        }
    }

    /**
     * Parses a {@link LiteralCommandNode} from a commodore file.
     *
     * @param inputStream an inputStream for the file
     * @param <S> the command node sender type
     * @return the command node
     * @throws IOException if an error occurs whilst reading the file
     * @throws RuntimeException if an error occurs whilst lexing or parsing the file
     */
    public static <S> LiteralCommandNode<S> parse(InputStream inputStream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return parse(reader);
        }
    }

    /**
     * Parses a {@link LiteralCommandNode} from a commodore file.
     *
     * @param path the path to the file
     * @param <S> the command node sender type
     * @return the command node
     * @throws IOException if an error occurs whilst reading the file
     * @throws RuntimeException if an error occurs whilst lexing or parsing the file
     */
    public static <S> LiteralCommandNode<S> parse(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }

    /**
     * Parses a {@link LiteralCommandNode} from a commodore file.
     *
     * @param file the file
     * @param <S> the command node sender type
     * @return the command node
     * @throws IOException if an error occurs whilst reading the file
     * @throws RuntimeException if an error occurs whilst lexing or parsing the file
     */
    public static <S> LiteralCommandNode<S> parse(File file) throws IOException {
        return parse(file.toPath());
    }

}
