package me.szabee.doubledoors.paper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import me.szabee.doubledoors.bukkit.DoubleDoors;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

/**
 * Paper loader entry point with Paper-only command registration.
 */
public final class PaperDoubleDoors extends DoubleDoors {

  /**
   * Registers permission defaults because Paper's plugin descriptor does not load plugin.yml permissions.
   */
  @Override
  public void onLoad() {
    registerPaperPermissions();
  }

  private void registerPaperPermissions() {
    PluginManager pluginManager = getServer().getPluginManager();
    registerPermission(pluginManager, "doubledoors.use", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.knock", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.knock.volume", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.autoclose", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.iron.manual", PermissionDefault.OP);
    registerPermission(pluginManager, "doubledoors.reload", PermissionDefault.OP);
    registerPermission(pluginManager, "doubledoors.toggle", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.toggle.autoclose", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.toggle.knock", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.locale", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.server-toggle", PermissionDefault.OP);
    registerPermission(pluginManager, "doubledoors.grief", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.debug", PermissionDefault.OP);
    registerPermission(pluginManager, "doubledoors.preview", PermissionDefault.TRUE);
    registerPermission(pluginManager, "doubledoors.update.notify", PermissionDefault.OP);
  }

  private void registerPermission(
    PluginManager pluginManager,
    String name,
    PermissionDefault defaultValue
  ) {
    if (pluginManager.getPermission(name) == null) {
      pluginManager.addPermission(new Permission(name, defaultValue));
    }
  }

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
