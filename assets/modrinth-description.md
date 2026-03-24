# TS3Bridge — Modrinth Description

---

## English

**TS3Bridge** connects your Paper Minecraft server to a TeamSpeak 3 server via ServerQuery. It keeps your community in sync whether they're in-game or in voice chat.

**What it does**

- Relays Minecraft chat to TeamSpeak and TeamSpeak chat back to Minecraft in real time
- Announces when players join or leave either side (`[MC] Steve joined` / `[TS] Thomas left`)
- Sends new players a configurable join advertisement with your TS server address
- Lets players link their Minecraft account to their TeamSpeak identity with `/ts link <name>`
- `/ts who` shows which TS clients are online and whether they're linked to a Minecraft player
- Supports both server-wide and channel-scoped bridging
- Reconnects automatically after connection drops
- Works with hosted providers like 4netplayers and Nitrado via voice-port selection
- Supports both plain TCP (RAW) and encrypted SSH ServerQuery connections

**Why another TS3 plugin?**

TeamspeakIP and BukkitSpeak couldn't connect to a hosted 4netplayers TeamSpeak server out of the box, and BukkitSpeak had gone unmaintained. TS3Bridge was built specifically to handle hosted providers and modern Paper versions.

**Requirements:** Paper 1.21.1+, TeamSpeak 3 server with ServerQuery enabled

---

## Deutsch

**TS3Bridge** verbindet deinen Paper-Minecraft-Server über ServerQuery mit einem TeamSpeak 3-Server und hält deine Community synchron — egal ob im Spiel oder im Voice-Chat.

**Was das Plugin macht**

- Leitet Minecraft-Chat in Echtzeit an TeamSpeak weiter und umgekehrt
- Kündigt an, wenn Spieler auf einer der beiden Seiten beitreten oder gehen (`[MC] Steve joined` / `[TS] Thomas left`)
- Sendet neuen Spielern beim Betreten eine konfigurierbare Willkommensnachricht mit der TS-Serveradresse
- Spieler können ihren Minecraft-Account über `/ts link <Name>` mit ihrer TeamSpeak-Identität verknüpfen
- `/ts who` zeigt, welche TS-Clients online sind und ob sie mit einem Minecraft-Spieler verknüpft sind
- Unterstützt serverweites und kanalbasiertes Bridging
- Verbindet sich nach Verbindungsabbrüchen automatisch neu
- Funktioniert mit gehosteten Anbietern wie 4netplayers und Nitrado über Voice-Port-Auswahl
- Unterstützt unverschlüsselte (RAW) und verschlüsselte SSH-ServerQuery-Verbindungen

**Warum noch ein TS3-Plugin?**

TeamspeakIP und BukkitSpeak ließen sich mit einem gehosteten 4netplayers-TeamSpeak-Server nicht ohne Weiteres verbinden, und BukkitSpeak wurde schon länger nicht mehr gepflegt. TS3Bridge wurde gezielt für gehostete Anbieter und aktuelle Paper-Versionen entwickelt.

**Voraussetzungen:** Paper 1.21.1+, TeamSpeak 3-Server mit aktiviertem ServerQuery
