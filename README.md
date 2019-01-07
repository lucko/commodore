# commodore  [![Javadocs](https://javadoc.io/badge/me.lucko/commodore.svg)](https://javadoc.io/doc/me.lucko/commodore) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.lucko/commodore/badge.svg)](https://maven-badges.herokuapp.com/maven-central/me.lucko/commodore)

Utility for using Minecraft's 1.13 'brigadier' library in Bukkit plugins.

### Usage

This guide assumes your plugin is built using Maven. The steps will be similar for Gradle though.

#### 1) Configure your build script to shade commodore into your plugin jar

You need to add (or merge) the following sections into your `pom.xml` file.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.2.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <artifactSet>
                            <includes>
                                <include>me.lucko:commodore</include>
                            </includes>
                        </artifactSet>
                        <relocations>
                            <relocation>
                                <pattern>me.lucko.commodore</pattern>
                                <!-- vvv Replace with the package of your plugin vvv -->
                                <shadedPattern>com.yourdomain.yourplugin.commodore</shadedPattern>
                            </relocation>
                        </relocations>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

<dependencies>
    <dependency>
        <groupId>me.lucko</groupId>
        <artifactId>commodore</artifactId>
        <version>1.3</version>
        <scope>compile</scope>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>minecraft-repo</id>
        <url>https://libraries.minecraft.net/</url>
    </repository>
</repositories>
```

#### 2) Setup commodore in your plugin

```java
package me.lucko.example;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class TestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        // register your command executor as normal.
        PluginCommand command = getCommand("mycommand");
        command.setExecutor(new MyCommandExecutor());

        // check if brigadier is supported
        if (CommodoreProvider.isSupported()) {
            
            // get a commodore instance
            Commodore commodore = CommodoreProvider.getCommodore(this);

            // register your completions.
            registerCompletions(commodore, command);
        }
    }
    
    // You will need to put this method inside another class to prevent classloading
    // errors when your plugin loads on pre 1.13 versions.
    private static void registerCompletions(Commodore commodore, PluginCommand command) {
        commodore.register(command, LiteralArgumentBuilder.literal("mycommand")
                .then(RequiredArgumentBuilder.argument("some-argument", StringArgumentType.string()))
                .then(RequiredArgumentBuilder.argument("some-other-argument", BoolArgumentType.bool()))
        );
    }
}
```

The `com.mojang.brigadier` packages will be automatically imported into your classpath when you add the commodore dependency, but they should not be shaded into your plugins jar file.
