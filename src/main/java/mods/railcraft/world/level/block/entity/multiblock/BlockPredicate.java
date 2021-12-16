package mods.railcraft.world.level.block.entity.multiblock;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public interface BlockPredicate {

  BlockPredicate AIR = (level, blockPos) -> level.getBlockState(blockPos).isAir();

  boolean test(ServerLevel level, BlockPos blockPos);

  default BlockPredicate or(BlockPredicate other) {
    Objects.requireNonNull(other);
    return (level, blockPos) -> this.test(level, blockPos) || other.test(level, blockPos);
  }
  
  static BlockPredicate ofFluidTag(Tag<Fluid> tag) {
    return (level, blockPos) -> level.getFluidState(blockPos).is(tag);
  }

  static BlockPredicate ofFluid(Fluid fluid) {
    return ofFluid(() -> fluid);
  }

  static BlockPredicate ofFluid(Supplier<Fluid> fluid) {
    return (level, blockPos) -> level.getFluidState(blockPos).is(fluid.get());
  }

  static BlockPredicate ofTag(Tag<Block> tag) {
    return (level, blockPos) -> level.getBlockState(blockPos).is(tag);
  }

  static BlockPredicate of(Block block) {
    return of(block);
  }

  static BlockPredicate of(Supplier<Block> block) {
    return (level, blockPos) -> level.getBlockState(blockPos).is(block.get());
  }
}
