/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2020

 This work (the API) is licensed under the "MIT" License,
 see LICENSE.md for details.
 -----------------------------------------------------------------------------*/
package mods.railcraft.api.signal;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 *
 * @author CovertJaguar <https://www.railcraft.info>
 */
public interface TuningAuraHelper {

  boolean isTuningAuraActive();

  void spawnTuningAuraParticles(BlockEntity start, BlockEntity dest);
}
