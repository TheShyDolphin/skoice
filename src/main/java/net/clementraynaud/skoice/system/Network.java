/*
 * Copyright 2020, 2021, 2022, 2023 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
 * Copyright 2016, 2017, 2018, 2019, 2020, 2021 Austin "Scarsz" Shapiro
 *
 * This file is part of Skoice.
 *
 * Skoice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Skoice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Skoice.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.clementraynaud.skoice.system;

import net.clementraynaud.skoice.Skoice;
import net.clementraynaud.skoice.storage.config.ConfigField;
import net.clementraynaud.skoice.util.DistanceUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Network {

    private static final double FALLOFF = 2.5;

    private static final Set<Network> networks = ConcurrentHashMap.newKeySet();

    private final Skoice plugin;
    private final Set<LinkedPlayer> players;
    private boolean initialized = false;
    private String channelId;

    public Network(Skoice plugin, String channelId) {
        this.plugin = plugin;
        this.players = Collections.emptySet();
        this.channelId = channelId;
    }

    public Network(Skoice plugin, Set<LinkedPlayer> players) {
        this.plugin = plugin;
        this.players = players;
    }

    public static Set<Network> getNetworks() {
        return Network.networks;
    }

    public void build() {
        Guild guild = this.plugin.getBot().getGuild();
        List<Permission> deniedPermissions = Arrays.asList(
                this.plugin.getConfigYamlFile().getBoolean(ConfigField.CHANNEL_VISIBILITY.toString())
                        ? Permission.VOICE_CONNECT
                        : Permission.VIEW_CHANNEL,
                Permission.VOICE_MOVE_OTHERS
        );
        this.plugin.getConfigYamlFile().getCategory().createVoiceChannel(UUID.randomUUID().toString())
                .addPermissionOverride(guild.getPublicRole(),
                        Arrays.asList(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD),
                        deniedPermissions)
                .addPermissionOverride(guild.getSelfMember(),
                        Arrays.asList(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS),
                        Collections.emptyList())
                .setBitrate(this.plugin.getConfigYamlFile().getVoiceChannel().getBitrate())
                .queue(voiceChannel -> {
                    this.channelId = voiceChannel.getId();
                    this.initialized = true;
                }, e -> Network.getNetworks().remove(this));
    }

    public boolean canPlayerBeAdded(LinkedPlayer player) {
        if (!player.isStateEligible()) {
            return false;
        }
        return this.players.stream()
                .filter(LinkedPlayer::isStateEligible)
                .map(LinkedPlayer::getBukkitPlayer)
                .filter(p -> !p.equals(player.getBukkitPlayer()))
                .filter(p -> p.getWorld().getName().equals(player.getBukkitPlayer().getWorld().getName()))
                .anyMatch(p -> DistanceUtil.getVerticalDistance(p.getLocation(), player.getBukkitPlayer().getLocation()) <= this.plugin.getConfigYamlFile()
                        .getInt(ConfigField.VERTICAL_RADIUS.toString())
                        && DistanceUtil.getHorizontalDistance(p.getLocation(), player.getBukkitPlayer().getLocation()) <= this.plugin.getConfigYamlFile()
                        .getInt(ConfigField.HORIZONTAL_RADIUS.toString()));
    }

    public boolean canPlayerStayConnected(LinkedPlayer player) {
        if (!player.isStateEligible()) {
            return false;
        }
        Set<Player> matches = this.players.stream()
                .filter(LinkedPlayer::isStateEligible)
                .map(LinkedPlayer::getBukkitPlayer)
                .filter(p -> p.getWorld().getName().equals(player.getBukkitPlayer().getWorld().getName()))
                .filter(p -> DistanceUtil.getVerticalDistance(p.getLocation(), player.getBukkitPlayer().getLocation()) <= this.plugin.getConfigYamlFile()
                        .getInt(ConfigField.VERTICAL_RADIUS.toString()) + Network.FALLOFF
                        && DistanceUtil.getHorizontalDistance(p.getLocation(), player.getBukkitPlayer().getLocation()) <= this.plugin.getConfigYamlFile()
                        .getInt(ConfigField.HORIZONTAL_RADIUS.toString()) + Network.FALLOFF)
                .collect(Collectors.toSet());
        if (this.players.size() > matches.size()) {
            Set<Player> otherPlayers = this.players.stream()
                    .filter(LinkedPlayer::isStateEligible)
                    .map(LinkedPlayer::getBukkitPlayer)
                    .filter(p -> p.getWorld().getName().equals(player.getBukkitPlayer().getWorld().getName()))
                    .filter(p -> !matches.contains(p))
                    .collect(Collectors.toSet());
            for (Player otherPlayer : otherPlayers) {
                if (matches.stream()
                        .anyMatch(p -> DistanceUtil.getVerticalDistance(p.getLocation(), otherPlayer.getLocation()) <= this.plugin.getConfigYamlFile()
                                .getInt(ConfigField.VERTICAL_RADIUS.toString()) + Network.FALLOFF
                                && DistanceUtil.getHorizontalDistance(p.getLocation(), otherPlayer.getLocation()) <= this.plugin.getConfigYamlFile()
                                .getInt(ConfigField.HORIZONTAL_RADIUS.toString()) + Network.FALLOFF)) {
                    return true;
                }
            }
            return false;
        }
        return matches.size() != 1;
    }

    public Network engulf(Network network) {
        this.players.addAll(network.players);
        network.players.clear();
        return this;
    }

    public void clear() {
        this.players.clear();
    }

    public void add(LinkedPlayer player) {
        this.players.add(player);
    }

    public void remove(LinkedPlayer player) {
        this.players.remove(player);
    }

    public void remove(Player player) {
        this.players.removeIf(p -> p.getBukkitPlayer().equals(player));
    }

    public boolean contains(LinkedPlayer player) {
        return this.players.contains(player);
    }

    public boolean contains(Player player) {
        return this.players.stream().anyMatch(p -> p.getBukkitPlayer().equals(player));
    }

    public int size() {
        return this.players.size();
    }

    public boolean isEmpty() {
        return this.players.isEmpty();
    }

    public VoiceChannel getChannel() {
        if (this.channelId == null || this.channelId.isEmpty()) {
            return null;
        }
        Guild guild = this.plugin.getBot().getGuild();
        if (guild != null) {
            return guild.getVoiceChannelById(this.channelId);
        }
        return null;
    }

    public boolean isInitialized() {
        return this.initialized;
    }
}
