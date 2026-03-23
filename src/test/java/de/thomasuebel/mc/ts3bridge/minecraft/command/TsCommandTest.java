package de.thomasuebel.mc.ts3bridge.minecraft.command;

import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;
import de.thomasuebel.mc.ts3bridge.teamspeak.FakeTeamspeakGateway;
import de.thomasuebel.mc.ts3bridge.teamspeak.TeamspeakService;
import de.thomasuebel.mc.ts3bridge.user.MappingsRepository;
import de.thomasuebel.mc.ts3bridge.user.UserLinkService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TsCommandTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir Path tempDir;
    @Mock CommandSender sender;
    @Mock Player player;
    @Mock Player targetPlayer;

    private FakeTeamspeakGateway gateway;
    private TeamspeakService teamspeakService;
    private MappingsRepository mappingsRepository;
    private UserLinkService userLinkService;
    private AtomicBoolean reloadCalled;
    private TsCommand tsCommand;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(tempDir.resolve("mappings.json"), "{}");
        gateway = new FakeTeamspeakGateway();
        gateway.setConnected(true);
        teamspeakService = new TeamspeakService(gateway);
        mappingsRepository = new MappingsRepository(tempDir, LOGGER);
        mappingsRepository.load();
        userLinkService = new UserLinkService(mappingsRepository, gateway, LOGGER);
        reloadCalled = new AtomicBoolean(false);
        tsCommand = new TsCommand(
                teamspeakService,
                userLinkService,
                mappingsRepository,
                () -> reloadCalled.set(true),
                uuid -> "MCPlayer_" + uuid.toString().substring(0, 4),
                name -> Optional.empty(),
                name -> Optional.empty(),
                () -> "Join our TeamSpeak!",
                new PluginConfig()
        );
    }

    // --- reload ---

    @Test
    void reloadCallsCallbackWhenSenderHasPermission() {
        when(sender.hasPermission("mcts.admin.reload")).thenReturn(true);

        tsCommand.onCommand(sender, null, "ts", new String[]{"reload"});

        assertTrue(reloadCalled.get());
    }

    @Test
    void reloadIsBlockedWithoutPermission() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"reload"});

        assertFalse(reloadCalled.get());
        verify(sender).sendMessage("You don't have permission to use this command.");
    }

    @Test
    void reloadSendsConfirmationMessage() {
        when(sender.hasPermission("mcts.admin.reload")).thenReturn(true);

        tsCommand.onCommand(sender, null, "ts", new String[]{"reload"});

        verify(sender).sendMessage("[TS] Plugin reloaded successfully.");
    }

    @Test
    void reloadIsIncludedInTabCompletion() {
        var completions = tsCommand.onTabComplete(sender, null, "ts", new String[]{"re"});

        assertNotNull(completions);
        assertTrue(completions.contains("reload"));
    }

    // --- /ts status ---

    @Test
    void statusShowsConnectedAndDetails() {
        when(sender.hasPermission("mcts.admin.status")).thenReturn(true);
        gateway.addOnlineClient("uid-1", "Alice");
        gateway.addOnlineClient("uid-2", "Bob");

        tsCommand.onCommand(sender, null, "ts", new String[]{"status"});

        verify(sender).sendMessage("[TS] Status: Connected");
        verify(sender).sendMessage(contains("localhost:10011"));
        verify(sender).sendMessage(contains("TS3Bridge"));
        verify(sender).sendMessage(contains("server-wide"));
        verify(sender).sendMessage(contains("enabled"));
        verify(sender).sendMessage(contains("Clients online: 2"));
    }

    @Test
    void statusShowsDisconnectedWithRetryHint() {
        gateway.setConnected(false);
        when(sender.hasPermission("mcts.admin.status")).thenReturn(true);

        tsCommand.onCommand(sender, null, "ts", new String[]{"status"});

        verify(sender).sendMessage(contains("Disconnected"));
        verify(sender).sendMessage(contains("/ts reload"));
        // host line still shown when disconnected
        verify(sender).sendMessage(contains("localhost"));
    }

    @Test
    void statusShowsChannelModeWhenConfigured() {
        when(sender.hasPermission("mcts.admin.status")).thenReturn(true);
        PluginConfig channelConfig = new PluginConfig();
        channelConfig.setTsBridgeChannelId(42);
        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> {}, uuid -> "x", name -> Optional.empty(), name -> Optional.empty(), () -> "", channelConfig
        );

        cmd.onCommand(sender, null, "ts", new String[]{"status"});

        verify(sender).sendMessage(contains("channel #42"));
    }

    @Test
    void statusShowsVirtualServerPortWhenConfigured() {
        when(sender.hasPermission("mcts.admin.status")).thenReturn(true);
        PluginConfig portConfig = new PluginConfig();
        portConfig.setTsVirtualServerPort(10006);
        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> {}, uuid -> "x", name -> Optional.empty(), name -> Optional.empty(), () -> "", portConfig
        );

        cmd.onCommand(sender, null, "ts", new String[]{"status"});

        verify(sender).sendMessage(contains("virtual server port 10006"));
    }

    // --- /ts who ---

    @Test
    void whoShowsTsNicknamesNotUids() {
        when(sender.hasPermission("mcts.command.who")).thenReturn(true);
        gateway.addOnlineClient("uid-abc", "TheRealAlice");

        tsCommand.onCommand(sender, null, "ts", new String[]{"who"});

        verify(sender).sendMessage(contains("TheRealAlice"));
        verify(sender, never()).sendMessage(contains("uid-abc"));
    }

    @Test
    void whoShowsMcPlayerNameForLinkedClient() {
        when(sender.hasPermission("mcts.command.who")).thenReturn(true);
        UUID mcUuid = UUID.randomUUID();
        gateway.addOnlineClient("uid-abc", "TsNick");
        mappingsRepository.link(mcUuid, "uid-abc");
        // playerNameResolver returns "MCPlayer_<first 4 chars of uuid>"
        String expectedMcName = "MCPlayer_" + mcUuid.toString().substring(0, 4);

        tsCommand.onCommand(sender, null, "ts", new String[]{"who"});

        verify(sender).sendMessage(contains(expectedMcName));
    }

    @Test
    void whoShowsNotLinkedForUnlinkedClient() {
        when(sender.hasPermission("mcts.command.who")).thenReturn(true);
        gateway.addOnlineClient("uid-xyz", "UnlinkedPerson");

        tsCommand.onCommand(sender, null, "ts", new String[]{"who"});

        verify(sender).sendMessage(contains("not linked"));
    }

    // --- /ts link <ts-nickname> (self-link by nickname) ---

    @Test
    void selfLinkByNicknameSucceeds() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission("mcts.command.link")).thenReturn(true);
        gateway.addOnlineClient("uid-ts", "MyTsName");

        tsCommand.onCommand(player, null, "ts", new String[]{"link", "MyTsName"});

        assertTrue(mappingsRepository.isLinked(uuid));
        verify(player).sendMessage(contains("MyTsName"));
    }

    @Test
    void selfLinkByNicknameNotFoundSendsHelpfulMessage() {
        when(player.hasPermission("mcts.command.link")).thenReturn(true);
        gateway.addOnlineClient("uid-ts", "SomeoneElse");

        tsCommand.onCommand(player, null, "ts", new String[]{"link", "Ghost"});

        verify(player).sendMessage(contains("Ghost"));
        verify(player).sendMessage(contains("online"));
    }

    @Test
    void selfLinkByNicknameAmbiguousListsMatches() {
        when(player.hasPermission("mcts.command.link")).thenReturn(true);
        gateway.addOnlineClient("uid-1", "Steve");
        gateway.addOnlineClient("uid-2", "steve");

        tsCommand.onCommand(player, null, "ts", new String[]{"link", "Steve"});

        verify(player).sendMessage(contains("Multiple"));
    }

    // --- /ts link <mc-player> <ts-nickname> (admin link) ---

    @Test
    void adminLinkLinksTargetPlayerToTsNickname() {
        UUID targetUuid = UUID.randomUUID();
        when(targetPlayer.getUniqueId()).thenReturn(targetUuid);
        when(targetPlayer.getName()).thenReturn("TargetMC");
        when(sender.hasPermission("mcts.admin.link")).thenReturn(true);
        gateway.addOnlineClient("uid-ts", "TheirTsName");

        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> {}, uuid -> "x", name -> Optional.of(targetPlayer), name -> Optional.empty(), () -> "", new PluginConfig()
        );

        cmd.onCommand(sender, null, "ts", new String[]{"link", "TargetMC", "TheirTsName"});

        assertTrue(mappingsRepository.isLinked(targetUuid));
        verify(sender).sendMessage(contains("TheirTsName"));
        verify(sender).sendMessage(contains("TargetMC"));
    }

    @Test
    void adminLinkBlockedWithoutPermission() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"link", "SomeMCPlayer", "SomeTsName"});

        verify(sender).sendMessage("You don't have permission to use this command.");
    }

    @Test
    void adminLinkFailsWhenMcPlayerNotOnline() {
        when(sender.hasPermission("mcts.admin.link")).thenReturn(true);
        // onlinePlayerFinder returns empty by default

        tsCommand.onCommand(sender, null, "ts", new String[]{"link", "OfflinePlayer", "SomeTsName"});

        verify(sender).sendMessage(contains("OfflinePlayer"));
        verify(sender).sendMessage(contains("not online"));
    }

    @Test
    void adminLinkFailsWhenTsNicknameNotFound() {
        when(sender.hasPermission("mcts.admin.link")).thenReturn(true);
        // onlinePlayerFinder returns targetPlayer, but TS lookup fails first — getName/getUniqueId not needed
        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> {}, uuid -> "x", name -> Optional.of(targetPlayer), name -> Optional.empty(), () -> "", new PluginConfig()
        );
        // no TS clients added — nobody online in TS

        cmd.onCommand(sender, null, "ts", new String[]{"link", "OnlinePlayer", "NoTsMatch"});

        verify(sender).sendMessage(contains("NoTsMatch"));
        verify(sender).sendMessage(contains("online"));
    }

    // --- /ts unlink <player> (admin unlink) ---

    @Test
    void adminUnlinkSucceedsForLinkedPlayer() {
        UUID uuid = UUID.randomUUID();
        mappingsRepository.link(uuid, "ts-uid-xyz");

        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> reloadCalled.set(true),
                u -> "MCPlayer",
                name -> Optional.empty(),
                name -> "admin-target".equals(name) ? Optional.of(uuid) : Optional.empty(),
                () -> "Join TeamSpeak!",
                new PluginConfig()
        );

        when(sender.hasPermission("mcts.admin.unlink")).thenReturn(true);
        cmd.onCommand(sender, null, "ts", new String[]{"unlink", "admin-target"});

        assertFalse(mappingsRepository.isLinked(uuid));
        verify(sender).sendMessage(contains("Removed TeamSpeak link for admin-target"));
    }

    @Test
    void adminUnlinkFailsForUnknownPlayer() {
        when(sender.hasPermission("mcts.admin.unlink")).thenReturn(true);
        tsCommand.onCommand(sender, null, "ts", new String[]{"unlink", "nobody"});
        verify(sender).sendMessage(contains("not found"));
    }

    @Test
    void adminUnlinkFailsWithoutPermission() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"unlink", "someone"});
        verify(sender).sendMessage(contains("don't have permission"));
    }

    @Test
    void selfUnlinkStillWorksAfterAdminUnlinkAdded() {
        UUID uuid = UUID.randomUUID();
        mappingsRepository.link(uuid, "ts-uid-abc");
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission("mcts.command.unlink")).thenReturn(true);

        tsCommand.onCommand(player, null, "ts", new String[]{"unlink"});

        assertFalse(mappingsRepository.isLinked(uuid));
    }

    // --- /ts (no args) ---

    @Test
    void noArgsPrintsAdvertisementAndUsage() {
        tsCommand.onCommand(sender, null, "ts", new String[]{});

        verify(sender).sendMessage("Join our TeamSpeak!");
        verify(sender).sendMessage(contains("Usage:"));
    }

    // --- unknown subcommand ---

    @Test
    void unknownSubcommandPrintsHelpMessage() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"doesnotexist"});

        verify(sender).sendMessage(contains("Unknown subcommand"));
    }

    // --- /ts who ---

    @Test
    void whoIsBlockedWithoutPermission() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"who"});

        verify(sender).sendMessage("You don't have permission to use this command.");
    }

    @Test
    void whoReportsNobodyOnlineWhenTsIsEmpty() {
        when(sender.hasPermission("mcts.command.who")).thenReturn(true);
        // no clients added

        tsCommand.onCommand(sender, null, "ts", new String[]{"who"});

        verify(sender).sendMessage(contains("Nobody is online"));
    }

    @Test
    void whoReportsNotConnectedWhenDisconnected() {
        gateway.setConnected(false);
        when(sender.hasPermission("mcts.command.who")).thenReturn(true);

        tsCommand.onCommand(sender, null, "ts", new String[]{"who"});

        verify(sender).sendMessage(contains("Not connected"));
    }

    // --- /ts status ---

    @Test
    void statusIsBlockedWithoutPermission() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"status"});

        verify(sender).sendMessage("You don't have permission to use this command.");
    }

    // --- /ts link self-link edge cases ---

    @Test
    void selfLinkFromNonPlayerSenderPrintsError() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"link", "SomeTsName"});

        verify(sender).sendMessage(contains("Only players"));
    }

    @Test
    void selfLinkWithMissingTsNamePrintsUsage() {
        when(player.hasPermission("mcts.command.link")).thenReturn(true);

        tsCommand.onCommand(player, null, "ts", new String[]{"link"});

        verify(player).sendMessage(contains("Usage:"));
    }

    @Test
    void selfLinkWhenTsDisconnectedPrintsError() {
        gateway.setConnected(false);
        when(player.hasPermission("mcts.command.link")).thenReturn(true);

        tsCommand.onCommand(player, null, "ts", new String[]{"link", "SomeTs"});

        verify(player).sendMessage(contains("unavailable"));
    }

    @Test
    void selfLinkAlreadyLinkedPrintsHint() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission("mcts.command.link")).thenReturn(true);
        gateway.addOnlineClient("uid-ts", "MyTs");
        mappingsRepository.link(uuid, "uid-ts");   // pre-link

        tsCommand.onCommand(player, null, "ts", new String[]{"link", "MyTs"});

        verify(player).sendMessage(contains("already linked"));
    }

    // --- /ts link admin-link edge cases ---

    @Test
    void adminLinkWhenTsDisconnectedPrintsError() {
        gateway.setConnected(false);
        when(sender.hasPermission("mcts.admin.link")).thenReturn(true);
        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> {}, uuid -> "x", name -> Optional.of(targetPlayer), name -> Optional.empty(), () -> "", new PluginConfig()
        );

        cmd.onCommand(sender, null, "ts", new String[]{"link", "OnlineMC", "SomeTs"});

        verify(sender).sendMessage(contains("unavailable"));
    }

    @Test
    void adminLinkAmbiguousTsNamePrintsMatches() {
        when(sender.hasPermission("mcts.admin.link")).thenReturn(true);
        gateway.addOnlineClient("uid-1", "Steve");
        gateway.addOnlineClient("uid-2", "steve");
        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> {}, uuid -> "x", name -> Optional.of(targetPlayer), name -> Optional.empty(), () -> "", new PluginConfig()
        );

        cmd.onCommand(sender, null, "ts", new String[]{"link", "OnlineMC", "Steve"});

        verify(sender).sendMessage(contains("Multiple"));
    }

    // --- /ts unlink ---

    @Test
    void selfUnlinkFromNonPlayerSenderPrintsError() {
        tsCommand.onCommand(sender, null, "ts", new String[]{"unlink"});

        verify(sender).sendMessage(contains("Only players"));
    }

    @Test
    void selfUnlinkWithoutPermissionPrintsError() {
        tsCommand.onCommand(player, null, "ts", new String[]{"unlink"});

        verify(player).sendMessage("You don't have permission to use this command.");
    }

    @Test
    void selfUnlinkWhenNotLinkedPrintsMessage() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission("mcts.command.unlink")).thenReturn(true);

        tsCommand.onCommand(player, null, "ts", new String[]{"unlink"});

        verify(player).sendMessage(contains("don't have a linked"));
    }

    @Test
    void adminUnlinkNotLinkedPlayerPrintsMessage() {
        UUID uuid = UUID.randomUUID();
        when(sender.hasPermission("mcts.admin.unlink")).thenReturn(true);
        TsCommand cmd = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                () -> {}, uuid2 -> "x", name -> Optional.empty(),
                name -> "unlinked".equals(name) ? Optional.of(uuid) : Optional.empty(),
                () -> "", new PluginConfig()
        );

        cmd.onCommand(sender, null, "ts", new String[]{"unlink", "unlinked"});

        verify(sender).sendMessage(contains("does not have a linked"));
    }

    // --- tab completion ---

    @Test
    void tabCompletionFiltersSubcommandsByPrefix() {
        var completions = tsCommand.onTabComplete(sender, null, "ts", new String[]{"s"});

        assertNotNull(completions);
        assertTrue(completions.contains("status"));
        assertFalse(completions.contains("who"));
    }

    @Test
    void tabCompletionForUnlinkWithAdminPermissionReturnsEmptyList() {
        when(sender.hasPermission("mcts.admin.unlink")).thenReturn(true);
        var completions = tsCommand.onTabComplete(sender, null, "ts", new String[]{"unlink", ""});

        assertNotNull(completions);
        assertTrue(completions.isEmpty());
    }
}
