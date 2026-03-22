package de.thomasuebel.mc.ts3bridge.teamspeak;

/**
 * Represents a non-query TeamSpeak client currently online on the server.
 */
public record TsClient(String uid, String nickname) {}
