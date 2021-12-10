/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2020

 This work (the API) is licensed under the "MIT" License,
 see LICENSE.md for details.
 -----------------------------------------------------------------------------*/

package mods.railcraft.api.item;

import mods.railcraft.api.track.TrackType;
import net.minecraft.world.item.ItemStack;

public interface TrackTypeLike {

  TrackType getTrackType(ItemStack stack);
}
