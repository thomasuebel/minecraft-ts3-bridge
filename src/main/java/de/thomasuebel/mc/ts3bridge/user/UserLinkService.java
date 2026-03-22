package de.thomasuebel.mc.ts3bridge.user;

import de.thomasuebel.mc.ts3bridge.teamspeak.TeamspeakGateway;

import java.util.UUID;
import java.util.logging.Logger;

public class UserLinkService {

    private final MappingsRepository repository;
    private final TeamspeakGateway gateway;
    private final Logger logger;

    public UserLinkService(MappingsRepository repository, TeamspeakGateway gateway, Logger logger) {
        this.repository = repository;
        this.gateway = gateway;
        this.logger = logger;
    }

    public LinkResult link(UUID minecraftUuid, String tsUid) {
        if (!gateway.isConnected()) {
            logger.warning("Player " + minecraftUuid + " attempted to link but TeamSpeak is unavailable.");
            return LinkResult.TEAMSPEAK_UNAVAILABLE;
        }
        if (repository.isLinked(minecraftUuid)) {
            logger.info("Player " + minecraftUuid + " attempted to link but is already linked.");
            return LinkResult.ALREADY_LINKED;
        }
        boolean tsUidIsOnline = gateway.getOnlineClientUids().contains(tsUid);
        if (!tsUidIsOnline) {
            logger.info("Player " + minecraftUuid + " attempted to link to TS UID '" + tsUid
                    + "' but that UID is not currently online in TeamSpeak.");
            return LinkResult.TS_UID_NOT_ONLINE;
        }
        repository.link(minecraftUuid, tsUid);
        repository.save();
        logger.info("Player " + minecraftUuid + " linked to TeamSpeak UID '" + tsUid + "'.");
        return LinkResult.SUCCESS;
    }

    public UnlinkResult unlink(UUID minecraftUuid) {
        if (!repository.isLinked(minecraftUuid)) {
            logger.info("Player " + minecraftUuid + " attempted to unlink but has no linked TeamSpeak account.");
            return UnlinkResult.NOT_LINKED;
        }
        String previousUid = repository.findTsUidByMinecraftUuid(minecraftUuid).orElse("unknown");
        repository.unlink(minecraftUuid);
        repository.save();
        logger.info("Player " + minecraftUuid + " unlinked from TeamSpeak UID '" + previousUid + "'.");
        return UnlinkResult.SUCCESS;
    }
}
