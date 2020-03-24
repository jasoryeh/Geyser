package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.world.sound.CustomSound;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlaySoundPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.sound.SoundMap;

public class JavaPlayerPlaySoundTranslator extends PacketTranslator<ServerPlaySoundPacket> {

    @Override
    public void translate(ServerPlaySoundPacket packet, GeyserSession session) {
        String packetSound;
        boolean usePlaySoundPacket;
        if(packet.getSound() instanceof BuiltinSound) {
            packetSound = ((BuiltinSound) packet.getSound()).getName();
            usePlaySoundPacket = false;
        } else if(packet.getSound() instanceof CustomSound) {
            packetSound = ((CustomSound) packet.getSound()).getName();
            usePlaySoundPacket = true;
        } else {
            session.getConnector().getLogger().debug("Unknown sound packet, we were unable to map this. " + packet.toString());
            return;
        }

        SoundMap.SoundMapping soundMapping = SoundMap.get().fromJava(packetSound);
        session.getConnector().getLogger()
                .debug("Sound mapping " + packetSound + " -> " + soundMapping
                        + soundMapping + (soundMapping == null ? "[not found]" : "")
                        + " - " + packet.toString());

        if(usePlaySoundPacket) {
            PlaySoundPacket playSoundPacket = new PlaySoundPacket();
            playSoundPacket.setSound(packetSound);
            playSoundPacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
            playSoundPacket.setVolume(packet.getVolume());
            playSoundPacket.setPitch(packet.getPitch());

            session.getUpstream().sendPacket(playSoundPacket);
            session.getConnector().getLogger().debug("Packet sent - " + packet.toString() + " --> " + playSoundPacket);
        } else {
            LevelSoundEventPacket levelSoundEventPacket = new LevelSoundEventPacket();
            SoundEvent sound = SoundMap.toSoundEvent(soundMapping.getBedrock());
            if(sound == null) {
                sound = SoundMap.toSoundEvent(soundMapping.getNukkit());
                if(sound == null) {
                    sound = SoundMap.toSoundEvent(packetSound);
                    if(sound == null) {
                        session.getConnector().getLogger()
                                .debug("Sound for original " + packetSound + " to mappings " + packetSound
                                        + " was not a playable level sound, or has yet to be mapped to an enum in " +
                                        "NukkitX SoundEvent ");
                        return;
                    } else {
                        session.getConnector().getLogger()
                                .debug("Sound for original " + packetSound + " to mappings " + packetSound
                                        + " was not found in NukkitX SoundEvent, but original packet sound name was.");
                    }
                }
            }

            levelSoundEventPacket.setSound(sound);
            levelSoundEventPacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
            levelSoundEventPacket.setExtraData(-1);
            levelSoundEventPacket.setIdentifier(":"); // ???
            levelSoundEventPacket.setBabySound(false); // might need to adjust this in the future
            levelSoundEventPacket.setRelativeVolumeDisabled(false);
            session.getUpstream().sendPacket(levelSoundEventPacket);
            session.getConnector().getLogger().debug("Packet sent - " + packet.toString() + " --> " + levelSoundEventPacket.toString());
        }
    }
}
