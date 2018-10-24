/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.util.crafting;

import mods.railcraft.common.items.RailcraftItems;
import mods.railcraft.common.util.inventory.InvTools;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class RotorRepairRecipe extends BaseRecipe {

    public RotorRepairRecipe() {
        super("rotor_repair");
    }

    private static final int REPAIR_PER_BLADE = 2500;
    private final ItemStack ROTOR = RailcraftItems.TURBINE_ROTOR.getStack();
    private final ItemStack BLADE = RailcraftItems.TURBINE_BLADE.getStack();

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        boolean hasRotor = false;
        boolean hasBlade = false;
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack stack = grid.getStackInSlot(slot);
            if (InvTools.isItemEqual(stack, ROTOR)) {
                hasRotor = true;
            } else if (InvTools.isItemEqual(stack, BLADE)) {
                hasBlade = true;
            }
        }
        return hasBlade && hasRotor;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        ItemStack rotor = InvTools.emptyStack();
        int numBlades = 0;
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack stack = grid.getStackInSlot(slot);
            if (InvTools.isItemEqual(stack, ROTOR)) {
                rotor = stack.copy();
            } else if (InvTools.isItemEqual(stack, BLADE)) {
                numBlades++;
            }
        }
        if (InvTools.isEmpty(rotor)) {
            return InvTools.emptyStack();
        }
        int damage = rotor.getItemDamage();
        damage -= REPAIR_PER_BLADE * numBlades;
        if (damage < 0) {
            damage = 0;
        }
        rotor.setItemDamage(damage);
        return rotor;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return InvTools.emptyStack();
    }
}
