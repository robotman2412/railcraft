package mods.railcraft.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * @author CovertJaguar <https://www.railcraft.info>
 */
public final class PowerUtil {

  public static final int NO_POWER = 0;
  public static final int FULL_POWER = 15;

  public static boolean hasRepeaterSignal(Level world, BlockPos pos, Direction from) {
    Block block = world.getBlockState(pos.relative(from)).getBlock();
    return block == Blocks.REPEATER && world.hasSignal(pos.relative(from), from);
  }

  public static boolean hasRepeaterSignal(Level world, BlockPos pos) {
    return Direction.Plane.HORIZONTAL.stream()
        .anyMatch(side -> hasRepeaterSignal(world, pos, side));
  }
}
