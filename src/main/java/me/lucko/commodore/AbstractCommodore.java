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
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractCommodore implements Commodore {

    // ArgumentCommandNode#customSuggestions field
    protected static final Field CUSTOM_SUGGESTIONS_FIELD;

    // CommandNode#command
    protected static final Field COMMAND_EXECUTE_FUNCTION_FIELD;

    // CommandNode#children, CommandNode#literals, CommandNode#arguments fields
    protected static final Field CHILDREN_FIELD;
    protected static final Field LITERALS_FIELD;
    protected static final Field ARGUMENTS_FIELD;

    // An array of the CommandNode fields above: [#children, #literals, #arguments]
    protected static final Field[] CHILDREN_FIELDS;

    // Dummy instance of Command used to ensure the executable bit gets set on
    // mock commands when they're encoded into data sent to the client
    protected static final com.mojang.brigadier.Command<?> DUMMY_COMMAND;
    protected static final SuggestionProvider<?> DUMMY_SUGGESTION_PROVIDER;

    static {
        try {
            CUSTOM_SUGGESTIONS_FIELD = ArgumentCommandNode.class.getDeclaredField("customSuggestions");
            CUSTOM_SUGGESTIONS_FIELD.setAccessible(true);

            COMMAND_EXECUTE_FUNCTION_FIELD = CommandNode.class.getDeclaredField("command");
            COMMAND_EXECUTE_FUNCTION_FIELD.setAccessible(true);

            CHILDREN_FIELD = CommandNode.class.getDeclaredField("children");
            LITERALS_FIELD = CommandNode.class.getDeclaredField("literals");
            ARGUMENTS_FIELD = CommandNode.class.getDeclaredField("arguments");
            CHILDREN_FIELDS = new Field[]{CHILDREN_FIELD, LITERALS_FIELD, ARGUMENTS_FIELD};
            for (Field field : CHILDREN_FIELDS) {
                field.setAccessible(true);
            }

            // should never be called
            // if ReflectionCommodore: bukkit handling should override
            // if PaperCommodore: this is only sent to the client, not used for actual command handling
            DUMMY_COMMAND = (ctx) -> { throw new UnsupportedOperationException(); };
            // should never be called - only used in clientbound root node, and the server impl will pass anything through
            // SuggestionProviders#safelySwap (swap it for the ASK_SERVER provider) before sending
            DUMMY_SUGGESTION_PROVIDER = (context, builder) -> { throw new UnsupportedOperationException(); };

        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static void removeChild(RootCommandNode root, String name) {
        try {
            for (Field field : CHILDREN_FIELDS) {
                Map<String, ?> children = (Map<String, ?>) field.get(root);
                children.remove(name);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void setRequiredHackyFieldsRecursively(CommandNode<?> node, SuggestionProvider<?> suggestionProvider) {
        // set command execution function so the server sets the executable flag on the command
        try {
            COMMAND_EXECUTE_FUNCTION_FIELD.set(node, DUMMY_COMMAND);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (suggestionProvider != null && node instanceof ArgumentCommandNode) {
            ArgumentCommandNode<?, ?> argumentNode = (ArgumentCommandNode<?, ?>) node;

            // set the custom suggestion provider field so tab completions work
            try {
                CUSTOM_SUGGESTIONS_FIELD.set(argumentNode, suggestionProvider);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        for (CommandNode<?> child : node.getChildren()) {
            setRequiredHackyFieldsRecursively(child, suggestionProvider);
        }
    }

    protected static <S> LiteralCommandNode<S> renameLiteralNode(LiteralCommandNode<S> node, String newLiteral) {
        LiteralCommandNode<S> clone = new LiteralCommandNode<>(newLiteral, node.getCommand(), node.getRequirement(), node.getRedirect(), node.getRedirectModifier(), node.isFork());
        for (CommandNode<S> child : node.getChildren()) {
            clone.addChild(child);
        }
        return clone;
    }

    /**
     * Gets the aliases known for the given command.
     *
     * <p>This will include the main label, as well as defined aliases, and
     * aliases including the fallback prefix added by Bukkit.</p>
     *
     * @param command the command
     * @return the aliases
     */
    protected static Collection<String> getAliases(Command command) {
        Objects.requireNonNull(command, "command");

        Stream<String> aliasesStream = Stream.concat(
                Stream.of(command.getLabel()),
                command.getAliases().stream()
        );

        if (command instanceof PluginCommand) {
            String fallbackPrefix = ((PluginCommand) command).getPlugin().getName().toLowerCase().trim();
            aliasesStream = aliasesStream.flatMap(alias -> Stream.of(
                    alias,
                    fallbackPrefix + ":" + alias
            ));
        }

        return aliasesStream.distinct().collect(Collectors.toList());
    }

}
