package me.szabee.doubledoors.paper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import me.szabee.doubledoors.bukkit.DoubleDoors;
import org.bukkit.command.CommandSender;

/**
 * Paper loader entry point with Paper-only command registration.
 */
public final class PaperDoubleDoors extends DoubleDoors {

  @Override
  protected void registerPlatformCommands() {
    if (!registerPaperCommand()) {
      super.registerPlatformCommands();
    }
  }

  private boolean registerPaperCommand() {
    try {
      Class<?> basicCommandClass = Class.forName(
        "io.papermc.paper.command.brigadier.BasicCommand"
      );
      Class<?> sourceStackClass = Class.forName(
        "io.papermc.paper.command.brigadier.CommandSourceStack"
      );
      Method getSender = sourceStackClass.getMethod("getSender");
      Object basicCommand = Proxy.newProxyInstance(
        basicCommandClass.getClassLoader(),
        new Class<?>[]{ basicCommandClass },
        (proxy, method, args) -> switch (method.getName()) {
          case "execute" -> {
            CommandSender sender = (CommandSender) getSender.invoke(args[0]);
            handleCommand(sender, "doubledoors", (String[]) args[1]);
            yield null;
          }
          case "suggest" -> {
            CommandSender sender = (CommandSender) getSender.invoke(args[0]);
            yield handleTabComplete(sender, "doubledoors", (String[]) args[1]);
          }
          case "permission" -> null;
          case "canUse" -> true;
          case "toString" -> "DoubleDoors[PaperCommandProxy]";
          case "equals" -> proxy == args[0];
          case "hashCode" -> System.identityHashCode(proxy);
          default -> {
            try {
              yield method.invoke(proxy, args);
            } catch (InvocationTargetException exception) {
              throw exception.getCause();
            }
          }
        }
      );

      Method registerCommand = getClass().getMethod(
        "registerCommand",
        String.class,
        java.util.Collection.class,
        basicCommandClass
      );
      registerCommand.invoke(this, "doubledoors", java.util.List.of("dd"), basicCommand);
      return true;
    } catch (ReflectiveOperationException | RuntimeException exception) {
      getLogger().warning("Paper command registration failed; using Bukkit fallback.");
      return false;
    }
  }
}
