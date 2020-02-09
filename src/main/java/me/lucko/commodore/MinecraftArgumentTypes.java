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

import com.mojang.brigadier.arguments.ArgumentType;
import org.bukkit.NamespacedKey;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A registry of the {@link ArgumentType}s provided by Minecraft.
 */
public final class MinecraftArgumentTypes {
    private MinecraftArgumentTypes() {}

    // MinecraftKey constructor
    private static final Constructor<?> MINECRAFT_KEY_CONSTRUCTOR;

    // ArgumentRegistry#getByKey (obfuscated) method
    private static final Method ARGUMENT_REGISTRY_GET_BY_KEY_METHOD;

    static {
        try {
            Class<?> minecraftKey = ReflectionUtil.nmsClass("MinecraftKey");
            MINECRAFT_KEY_CONSTRUCTOR = minecraftKey.getConstructor(String.class, String.class);

            Class<?> argumentRegistry = ReflectionUtil.nmsClass("ArgumentRegistry");
            ARGUMENT_REGISTRY_GET_BY_KEY_METHOD = Arrays.stream(argumentRegistry.getDeclaredMethods())
                    .filter(method -> method.getParameterCount() == 1)
                    .filter(method -> minecraftKey.equals(method.getParameterTypes()[0]))
                    .filter(method -> ArgumentType.class.isAssignableFrom(method.getReturnType()))
                    .findFirst().orElseThrow(NoSuchMethodException::new);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Gets a registered argument type by key.
     *
     * @param key the key
     * @return the returned argument
     * @throws IllegalArgumentException if no such argument is registered
     */
    public static ArgumentType<?> getByKey(NamespacedKey key) throws IllegalArgumentException {
        try {
            Object minecraftKey = MINECRAFT_KEY_CONSTRUCTOR.newInstance(key.getNamespace(), key.getKey());
            Object argument = ARGUMENT_REGISTRY_GET_BY_KEY_METHOD.invoke(null, minecraftKey);
            if (argument == null) {
                throw new IllegalArgumentException(key.toString());
            }
            return (ArgumentType<?>) argument;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    static void ensureSetup() {
        // do nothing - this is only called to trigger the static initializer
    }
}
