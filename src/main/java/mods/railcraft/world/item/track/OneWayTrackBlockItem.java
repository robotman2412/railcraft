package mods.railcraft.world.item.track;

import java.util.List;
import mods.railcraft.Translations.Tips;
import mods.railcraft.world.level.block.track.outfitted.OneWayTrackBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class OneWayTrackBlockItem extends BlockItem {

    public OneWayTrackBlockItem(OneWayTrackBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
        TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable(Tips.ONE_WAY_TRACK).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(Tips.HIT_CROWBAR_TO_CHANGE_DIRECTION).withStyle(ChatFormatting.BLUE));
    }
}
