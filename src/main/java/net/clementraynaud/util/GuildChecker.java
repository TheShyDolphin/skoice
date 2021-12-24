/*
 * Copyright 2020, 2021 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
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

package net.clementraynaud.util;

import net.dv8tion.jda.api.entities.Guild;

import static net.clementraynaud.Skoice.getPlugin;
import static net.clementraynaud.util.DataGetters.*;

public class GuildChecker {

    public boolean isGuildSelected(Guild guild) {
        if (getPlugin().getConfigFile().getString("lobby-id") == null
                && getVerticalRadius() == 0
                && getHorizontalRadius() == 0) {
            return true;
        }
        return getGuild() == guild;
    }
}
