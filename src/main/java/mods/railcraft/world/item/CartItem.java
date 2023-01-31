package mods.railcraft.world.item;

import mods.railcraft.api.item.MinecartFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

public class CartItem extends Item {

  private final MinecartFactory minecartFactory;

  public CartItem(MinecartFactory minecartFactory, Properties properties) {
    super(properties);
    this.minecartFactory = minecartFactory;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var player = context.getPlayer();
    var hand = context.getHand();
    var level = context.getLevel();
    var railPos = context.getClickedPos();
    var railState = level.getBlockState(railPos);
    var itemStack = player.getItemInHand(hand);
    if (!BaseRailBlock.isRail(level, railPos)) {
      return InteractionResult.FAIL;
    }
    if (level instanceof ServerLevel serverLevel) {
      var railShape = RailShape.NORTH_SOUTH;
      if (railState.getBlock() instanceof BaseRailBlock baseRailBlock) {
        railShape = baseRailBlock.getRailDirection(railState, level, railPos, null);
      }
      double d0 = railShape.isAscending() ? 0.5 : 0.0;

      var minecart = this.minecartFactory.createMinecart(itemStack,
          railPos.getX() + 0.5, railPos.getY() + d0 + 0.0625, railPos.getZ() + 0.5, serverLevel);
      if (minecart != null) {
        minecart.setYRot(context.getHorizontalDirection().toYRot());
        level.addFreshEntity(minecart);
        itemStack.shrink(1);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  public MinecartFactory getMinecartFactory() {
    return this.minecartFactory;
  }
}
