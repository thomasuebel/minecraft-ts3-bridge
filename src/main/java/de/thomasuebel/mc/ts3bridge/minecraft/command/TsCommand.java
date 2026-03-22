package de.thomasuebel.mc.ts3bridge.minecraft.command;

import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;
import de.thomasuebel.mc.ts3bridge.teamspeak.TsClient;
import de.thomasuebel.mc.ts3bridge.teamspeak.TeamspeakService;
import de.thomasuebel.mc.ts3bridge.user.LinkResult;
import de.thomasuebel.mc.ts3bridge.user.MappingsRepository;
import de.thomasuebel.mc.ts3bridge.user.UnlinkResult;
import de.thomasuebel.mc.ts3bridge.user.UserLinkService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TsCommand implements CommandExecutor, TabCompleter {

    private final TeamspeakService teamspeakService;
    private final UserLinkService userLinkService;
    private final MappingsRepository mappingsRepository;
    private final Runnable reloadCallback;
    private final Function<UUID, String> playerNameResolver;
    private final Function<String, Optional<Player>> onlinePlayerFinder;
    private final Supplier<String> advertisementMessageSupplier;
    private final PluginConfig config;

    public TsCommand(TeamspeakService teamspeakService,
                     UserLinkService userLinkService,
                     MappingsRepository mappingsRepository,
                     Runnable reloadCallback,
                     Function<UUID, String> playerNameResolver,
                     Function<String, Optional<Player>> onlinePlayerFinder,
                     Supplier<String> advertisementMessageSupplier,
                     PluginConfig config) {
        this.teamspeakService = teamspeakService;
        this.userLinkService = userLinkService;
        this.mappingsRepository = mappingsRepository;
        this.reloadCallback = reloadCallback;
        this.playerNameResolver = playerNameResolver;
        this.onlinePlayerFinder = onlinePlayerFinder;
        this.advertisementMessageSupplier = advertisementMessageSupplier;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(advertisementMessageSupplier.get());
            sender.sendMessage("Usage: /ts <who|status|link|unlink|reload>");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "who" -> handleWho(sender);
            case "status" -> handleStatus(sender);
            case "link" -> handleLink(sender, args);
            case "unlink" -> handleUnlink(sender);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage("Unknown subcommand. Usage: /ts <who|status|link|unlink|reload>");
                yield true;
            }
        };
    }

    private boolean handleWho(CommandSender sender) {
        if (!hasPermission(sender, "mcts.command.who")) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }
        if (!teamspeakService.isConnected()) {
            sender.sendMessage("[TS] Not connected to TeamSpeak.");
            return true;
        }

        List<TsClient> clients = teamspeakService.getOnlineClients();
        if (clients.isEmpty()) {
            sender.sendMessage("[TS] Nobody is online in TeamSpeak.");
            return true;
        }

        sender.sendMessage("[TS] Online in TeamSpeak (" + clients.size() + "):");
        for (TsClient client : clients) {
            Optional<UUID> mcUuid = mappingsRepository.findMinecraftUuidByTsUid(client.uid());
            String line = mcUuid
                    .map(uuid -> "  - " + client.nickname() + " (linked to MC: " + playerNameResolver.apply(uuid) + ")")
                    .orElse("  - " + client.nickname() + " (not linked)");
            sender.sendMessage(line);
        }
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!hasPermission(sender, "mcts.admin.status")) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }

        boolean connected = teamspeakService.isConnected();
        sender.sendMessage(connected
                ? "[TS] Status: Connected"
                : "[TS] Status: Disconnected — use /ts reload to retry");

        // Host line
        String hostLine = "[TS] Host: " + config.getTsHost() + ":" + config.getTsQueryPort();
        if (config.getTsVirtualServerPort() > 0) {
            hostLine += " (virtual server port " + config.getTsVirtualServerPort() + ")";
        } else {
            hostLine += " (virtual server ID " + config.getTsVirtualServerId() + ")";
        }
        sender.sendMessage(hostLine);

        if (connected) {
            // Bot nickname
            sender.sendMessage("[TS] Bot nickname: " + config.getTsQueryNickname());

            // Bridge mode + chat toggle
            String bridgeMode = config.getTsBridgeChannelId() > 0
                    ? "channel #" + config.getTsBridgeChannelId()
                    : "server-wide";
            String chatStatus = config.isChatBridgeEnabled() ? "enabled" : "disabled";
            sender.sendMessage("[TS] Bridge: " + bridgeMode + "  |  Chat: " + chatStatus);

            // Online client count
            int count = teamspeakService.getOnlineClients().size();
            sender.sendMessage("[TS] Clients online: " + count);
        }

        return true;
    }

    private boolean handleLink(CommandSender sender, String[] args) {
        // Admin link: /ts link <mc-player> <ts-nickname>
        if (args.length == 3) {
            if (!hasPermission(sender, "mcts.admin.link")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }
            return handleAdminLink(sender, args[1], args[2]);
        }

        // Self-link: /ts link <ts-nickname>
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can link accounts.");
            return true;
        }
        if (!hasPermission(sender, "mcts.command.link")) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /ts link <teamspeak-name>");
            return true;
        }

        return handleSelfLink(player, args[1]);
    }

    private boolean handleSelfLink(Player player, String tsNickname) {
        if (!teamspeakService.isConnected()) {
            player.sendMessage("[TS] TeamSpeak is currently unavailable. Try again later.");
            return true;
        }

        List<TsClient> matches = teamspeakService.findOnlineClientsByNickname(tsNickname);

        if (matches.isEmpty()) {
            player.sendMessage("[TS] No TS user named '" + tsNickname + "' is currently online. "
                    + "Make sure you are connected to TeamSpeak.");
            return true;
        }
        if (matches.size() > 1) {
            String names = matches.stream().map(TsClient::nickname).collect(Collectors.joining(", "));
            player.sendMessage("[TS] Multiple TS users named '" + tsNickname + "' are online: " + names
                    + ". Ask an admin to link you with /ts link <your-mc-name> <ts-name>.");
            return true;
        }

        TsClient match = matches.get(0);
        LinkResult result = userLinkService.link(player.getUniqueId(), match.uid());
        player.sendMessage(switch (result) {
            case SUCCESS -> "[TS] Successfully linked your account to TeamSpeak user: " + match.nickname();
            case ALREADY_LINKED -> "[TS] Your account is already linked. Use /ts unlink first.";
            case TS_UID_NOT_ONLINE -> "[TS] That TeamSpeak user is no longer online. Try again.";
            case TEAMSPEAK_UNAVAILABLE -> "[TS] TeamSpeak is currently unavailable. Try again later.";
        });
        return true;
    }

    private boolean handleAdminLink(CommandSender sender, String mcPlayerName, String tsNickname) {
        Optional<Player> target = onlinePlayerFinder.apply(mcPlayerName);
        if (target.isEmpty()) {
            sender.sendMessage("[TS] Player '" + mcPlayerName + "' is not online.");
            return true;
        }

        if (!teamspeakService.isConnected()) {
            sender.sendMessage("[TS] TeamSpeak is currently unavailable. Try again later.");
            return true;
        }

        List<TsClient> matches = teamspeakService.findOnlineClientsByNickname(tsNickname);

        if (matches.isEmpty()) {
            sender.sendMessage("[TS] No TS user named '" + tsNickname + "' is currently online.");
            return true;
        }
        if (matches.size() > 1) {
            String names = matches.stream().map(TsClient::nickname).collect(Collectors.joining(", "));
            sender.sendMessage("[TS] Multiple TS users named '" + tsNickname + "' are online: " + names + ".");
            return true;
        }

        TsClient match = matches.get(0);
        LinkResult result = userLinkService.link(target.get().getUniqueId(), match.uid());
        sender.sendMessage(switch (result) {
            case SUCCESS -> "[TS] Linked " + target.get().getName() + " to TeamSpeak user: " + match.nickname();
            case ALREADY_LINKED -> "[TS] " + target.get().getName() + " is already linked. They must /ts unlink first.";
            case TS_UID_NOT_ONLINE -> "[TS] That TeamSpeak user is no longer online. Try again.";
            case TEAMSPEAK_UNAVAILABLE -> "[TS] TeamSpeak is currently unavailable. Try again later.";
        });
        return true;
    }

    private boolean handleUnlink(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can unlink accounts.");
            return true;
        }
        if (!hasPermission(sender, "mcts.command.unlink")) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }

        UnlinkResult result = userLinkService.unlink(player.getUniqueId());
        sender.sendMessage(switch (result) {
            case SUCCESS -> "[TS] Your TeamSpeak link has been removed.";
            case NOT_LINKED -> "[TS] You don't have a linked TeamSpeak account.";
        });
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasPermission(sender, "mcts.admin.reload")) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }
        reloadCallback.run();
        sender.sendMessage("[TS] Plugin reloaded successfully.");
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || (sender instanceof Player p && p.isOp());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("who", "status", "link", "unlink", "reload").stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
