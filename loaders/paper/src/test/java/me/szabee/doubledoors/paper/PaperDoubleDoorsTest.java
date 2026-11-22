package me.szabee.doubledoors.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

class PaperDoubleDoorsTest {

  @Test
  void onLoadRegistersPermissionDefaults() {
    PaperDoubleDoors plugin = mock(PaperDoubleDoors.class);
    PluginManager pluginManager = mock(PluginManager.class);
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    when(server.getPluginManager()).thenReturn(pluginManager);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("PaperDoubleDoorsTest"));
    when(pluginManager.getPermission(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
    doCallRealMethod().when(plugin).onLoad();

    plugin.onLoad();

    Map<String, PermissionDefault> defaults = Map.ofEntries(
      Map.entry("doubledoors.use", PermissionDefault.TRUE),
      Map.entry("doubledoors.knock", PermissionDefault.TRUE),
      Map.entry("doubledoors.knock.volume", PermissionDefault.TRUE),
      Map.entry("doubledoors.autoclose", PermissionDefault.TRUE),
      Map.entry("doubledoors.iron.manual", PermissionDefault.OP),
      Map.entry("doubledoors.reload", PermissionDefault.OP),
      Map.entry("doubledoors.toggle", PermissionDefault.TRUE),
      Map.entry("doubledoors.toggle.autoclose", PermissionDefault.TRUE),
      Map.entry("doubledoors.toggle.knock", PermissionDefault.TRUE),
      Map.entry("doubledoors.locale", PermissionDefault.TRUE),
      Map.entry("doubledoors.server-toggle", PermissionDefault.OP),
      Map.entry("doubledoors.grief", PermissionDefault.TRUE),
      Map.entry("doubledoors.debug", PermissionDefault.OP),
      Map.entry("doubledoors.preview", PermissionDefault.TRUE),
      Map.entry("doubledoors.update.notify", PermissionDefault.OP)
    );
    assertEquals(15, defaults.size());
    defaults.forEach((name, value) ->
      verify(pluginManager).addPermission(org.mockito.ArgumentMatchers.argThat(permission ->
        permission.getName().equals(name) && permission.getDefault() == value
      ))
    );
  }

  @Test
  void existingPermissionsAreNotReplaced() {
    PaperDoubleDoors plugin = mock(PaperDoubleDoors.class);
    PluginManager pluginManager = mock(PluginManager.class);
    Server server = mock(Server.class);
    Permission existing = new Permission("doubledoors.use", PermissionDefault.FALSE);
    when(plugin.getServer()).thenReturn(server);
    when(server.getPluginManager()).thenReturn(pluginManager);
    when(pluginManager.getPermission("doubledoors.use")).thenReturn(existing);
    when(pluginManager.getPermission(org.mockito.ArgumentMatchers.argThat(name -> !name.equals("doubledoors.use"))))
      .thenReturn(null);
    doCallRealMethod().when(plugin).onLoad();

    plugin.onLoad();

    verify(pluginManager, org.mockito.Mockito.never()).addPermission(existing);
  }
}
