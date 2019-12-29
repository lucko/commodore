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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

/**
 * A lexer for the {@link CommodoreFileFormat}.
 *
 * <p>Transforms string data (from a {@link Reader}) into an iterator of {@link Token}s.</p>
 */
class Lexer extends AbstractIterator<Lexer.Token> implements PeekingIterator<Lexer.Token> {
    private final StreamTokenizer tokenizer;
    private boolean end = false;

    Lexer(Reader reader) {
        this.tokenizer = new StreamTokenizer(reader);
        this.tokenizer.resetSyntax();
        this.tokenizer.wordChars('!', '~'); // all ascii characters
        this.tokenizer.quoteChar('"');
        this.tokenizer.whitespaceChars('\u0000', ' ');
        "{};".chars().forEach(this.tokenizer::ordinaryChar);
        this.tokenizer.slashSlashComments(true);
        this.tokenizer.slashStarComments(true);
    }

    public int currentLine() {
        return this.tokenizer.lineno();
    }

    @Override
    protected Token computeNext() {
        if (this.end) {
            return endOfData();
        }
        try {
            int token = this.tokenizer.nextToken();
            switch (token) {
                case StreamTokenizer.TT_EOF:
                    this.end = true;
                    return ConstantToken.EOF;
                case StreamTokenizer.TT_WORD:
                    return new StringToken(this.tokenizer.sval);
                case '{':
                    return ConstantToken.OPEN_BRACKET;
                case '}':
                    return ConstantToken.CLOSE_BRACKET;
                case ';':
                    return ConstantToken.SEMICOLON;
                default:
                    throw new LexerException(this, "Unknown token: " + ((char) token) + "(" + token + ")");
            }
        } catch (IOException e) {
            throw new LexerException(this, e);
        }
    }

    interface Token { }

    enum ConstantToken implements Token {
        OPEN_BRACKET, CLOSE_BRACKET, SEMICOLON, EOF
    }

    static final class StringToken implements Token {
        final String string;

        private StringToken(String string) {
            this.string = string;
        }
    }

    /**
     * Exception thrown if lexing fails.
     */
    static final class LexerException extends RuntimeException {
        private LexerException(Lexer lexer, String message) {
            super(message + " (at line " + lexer.currentLine() + ")");
        }

        private LexerException(Lexer lexer, Throwable cause) {
            super("At line " + lexer.currentLine(), cause);
        }
    }

}
