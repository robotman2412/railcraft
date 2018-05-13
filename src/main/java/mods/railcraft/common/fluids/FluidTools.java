/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2017
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.fluids;

import mods.railcraft.api.core.RailcraftConstantsAPI;
import mods.railcraft.client.particles.ParticleDrip;
import mods.railcraft.common.fluids.tanks.StandardTank;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.inventory.wrappers.InventoryMapper;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static mods.railcraft.common.util.inventory.InvTools.isEmpty;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class FluidTools {
    public static final int BUCKET_FILL_TIME = 8;
    public static final int NETWORK_UPDATE_INTERVAL = 128;
    public static final int BUCKET_VOLUME = 1000;
    public static final int PROCESS_VOLUME = BUCKET_VOLUME * 4;

    private FluidTools() {
    }

    @Contract("null -> null; !null -> !null")
    @Nullable
    public static @PolyNull FluidStack copy(@Nullable @PolyNull FluidStack fluidStack) {
        return fluidStack == null ? null : fluidStack.copy();
    }

    public static String toString(@Nullable FluidStack fluidStack) {
        if (fluidStack == null)
            return "null";
        return fluidStack.amount + "x" + fluidStack.getFluid().getName();
    }

    /**
     * Forge is too picking here. So no {@link InvTools#isEmpty(ItemStack)} here.
     *
     * @param stack The stack to check
     * @return True if the liquid failed to drains/fill
     */
    public static boolean isFailed(@Nullable ItemStack stack) {
        return stack == null;
    }

    @Nullable
    public static net.minecraftforge.fluids.capability.IFluidHandler getFluidHandler(@Nullable EnumFacing side, ICapabilityProvider object) {
        return object.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
    }

    public static boolean hasFluidHandler(@Nullable EnumFacing side, ICapabilityProvider object) {
        return object.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
    }

    /**
     * Use {@link #interactWithFluidHandler(EntityPlayer, EnumHand, IFluidHandler)} instead.
     *
     * @deprecated Obsolete!!!
     */
    @Deprecated
    public static boolean interactWithFluidHandler(ItemStack heldItem, @Nullable IFluidHandler fluidHandler, EntityPlayer player) {
        Game.log(Level.ERROR, "Calling deprecated method interactWithFluidHandler");
        if (Game.isHost(player.world))
            return FluidUtil.interactWithFluidHandler(player, EnumHand.MAIN_HAND, fluidHandler);
        return FluidItemHelper.isContainer(heldItem);
    }

    public static boolean interactWithFluidHandler(EntityPlayer player, EnumHand hand, IFluidHandler fluidHandler) {
        if (Game.isHost(player.world))
            return FluidUtil.interactWithFluidHandler(player, hand, fluidHandler);
        return FluidItemHelper.isContainer(player.getHeldItem(hand));
    }

    public enum ProcessState {
        FILLING,
        DRAINING,
        RESET
    }

    private static void sendToProcessing(IInventory inv) {
        InvTools.moveOneItem(InventoryMapper.make(inv, 0, 1), InventoryMapper.make(inv, 1, 1, false));
    }

    private static void sendToOutput(IInventory inv) {
        InvTools.moveOneItem(InventoryMapper.make(inv, 1, 1), InventoryMapper.make(inv, 2, 1, false));
    }

    private static ProcessState tryFill(IInventory inv, StandardTank tank, ItemStack container) {
        FluidActionResult filled = FluidUtil.tryFillContainer(container, tank, Fluid.BUCKET_VOLUME, null, true);
        if (!filled.isSuccess()) {
            sendToOutput(inv);
            return ProcessState.RESET;
        }
        inv.setInventorySlotContents(1, InvTools.makeSafe(filled.getResult()));
        return ProcessState.FILLING;
    }

    private static ProcessState tryDrain(IInventory inv, StandardTank tank, ItemStack container) {
        FluidActionResult drained = FluidUtil.tryEmptyContainer(container, tank, Fluid.BUCKET_VOLUME, null, true);
        if (!drained.isSuccess()) {
            sendToOutput(inv);
            return ProcessState.RESET;
        }
        inv.setInventorySlotContents(1, InvTools.makeSafe(drained.getResult()));
        return ProcessState.DRAINING;
    }

    /**
     * Expects a three slot inventory, with input as slot 0, processing as slot 1, and output as slot 2.
     * Will handle moving an item through all stages from input to output for either filling or draining.
     */
    public static ProcessState processContainer(IInventory inv, StandardTank tank, boolean defaultToFill, ProcessState state) {
        ItemStack container = inv.getStackInSlot(1);
        if (isEmpty(container) || FluidUtil.getFluidHandler(container) == null) {
            sendToProcessing(inv);
            return ProcessState.RESET;
        }
        if (state == ProcessState.RESET) {
            if (defaultToFill) {
                return tryFill(inv, tank, container);
            } else {
                return tryDrain(inv, tank, container);
            }
        }
        if (state == ProcessState.FILLING)
            return tryFill(inv, tank, container);
        if (state == ProcessState.DRAINING)
            return tryDrain(inv, tank, container);
        return state;
    }

    /**
     * Process containers in input/output slot like tha in the tank cart.
     *
     * @param tank       Fluid tank
     * @param inv        The inventory that contains input/output slots
     * @param inputSlot  The input slot number
     * @param outputSlot The output slot number
     * @return {@code true} if changes have been done to the tank
     */
    public static boolean processContainers(StandardTank tank, IInventory inv, int inputSlot, int outputSlot) {
        return processContainers(tank, inv, inputSlot, outputSlot, tank.getFluidType(), true, true);
    }

    public static boolean processContainers(StandardTank tank, IInventory inv, int inputSlot, int outputSlot, @Nullable Fluid fluidToFill, boolean processFilled, boolean processEmpty) {
        TankManager tankManger = new TankManager();
        tankManger.add(tank);
        return processContainers(tankManger, inv, inputSlot, outputSlot, fluidToFill, processFilled, processEmpty);
    }

    public static boolean processContainers(TankManager tank, IInventory inv, int inputSlot, int outputSlot, @Nullable Fluid fluidToFill) {
        return processContainers(tank, inv, inputSlot, outputSlot, fluidToFill, true, true);
    }

    public static boolean processContainers(IFluidHandler fluidHandler, IInventory inv, int inputSlot, int outputSlot, @Nullable Fluid fluidToFill, boolean processFilled, boolean processEmpty) {
        ItemStack input = inv.getStackInSlot(inputSlot);

        if (isEmpty(input))
            return false;

        if (processFilled && drainContainers(fluidHandler, inv, inputSlot, outputSlot))
            return true;

        if (processEmpty && fluidToFill != null)
            return fillContainers(fluidHandler, inv, inputSlot, outputSlot, fluidToFill);
        return false;
    }

    public static boolean fillContainers(IFluidHandler source, IInventory inv, int inputSlot, int outputSlot, @Nullable Fluid fluidToFill) {
        ItemStack input = inv.getStackInSlot(inputSlot);
        //need an empty container
        if (isEmpty(input))
            return false;
        ItemStack output = inv.getStackInSlot(outputSlot);
        FluidActionResult container = FluidUtil.tryFillContainer(input, source, BUCKET_VOLUME, null, false);
        //check failure
        if (!container.isSuccess())
            return false;
        //check filled fluid type
        if (fluidToFill != null && !isEmpty(container.getResult())) {
            FluidStack fluidStack = FluidUtil.getFluidContained(container.getResult());
            if (fluidStack != null && fluidStack.getFluid() != fluidToFill)
                return false;
        }
        //check place for container
        if (!hasPlaceToPutContainer(output, container.getResult()))
            return false;
        //do actual things here
        container = FluidUtil.tryFillContainer(input, source, BUCKET_VOLUME, null, true);
        storeContainer(inv, inputSlot, outputSlot, container.getResult());
        return true;
    }

    public static boolean drainContainers(IFluidHandler dest, IInventory inv, int inputSlot, int outputSlot) {
        ItemStack input = inv.getStackInSlot(inputSlot);
        //need a valid container
        if (isEmpty(input))
            return false;
        ItemStack output = inv.getStackInSlot(outputSlot);
        FluidActionResult container = FluidUtil.tryEmptyContainer(input, dest, BUCKET_VOLUME, null, false);
        //check failure
        if (!container.isSuccess())
            return false;
        //check place for container
        if (!hasPlaceToPutContainer(output, container.getResult()))
            return false;
        //do actual things here
        container = FluidUtil.tryEmptyContainer(input, dest, BUCKET_VOLUME, null, true);
        storeContainer(inv, inputSlot, outputSlot, container.getResult());
        return true;
    }

    private static boolean hasPlaceToPutContainer(ItemStack output, ItemStack container) {
        return isEmpty(output) || isEmpty(container) || output.getCount() < output.getMaxStackSize() && InvTools.isItemEqual(container, output);
    }

    /**
     * We can assume that if null is passed for the container that the container
     * was consumed by the process and we should just remove the input container.
     */
    private static void storeContainer(IInventory inv, int inputSlot, int outputSlot, @Nullable ItemStack container) {
        if (isEmpty(container)) {
            inv.decrStackSize(inputSlot, 1);
            return;
        }
        ItemStack output = inv.getStackInSlot(outputSlot);
        if (isEmpty(output))
            inv.setInventorySlotContents(outputSlot, container);
        else
            InvTools.inc(output);
        inv.decrStackSize(inputSlot, 1);
    }

    public static void initWaterBottle(boolean nerf) {
        WaterBottleEventHandler.INSTANCE.amount = nerf ? 333 : 1000;
        MinecraftForge.EVENT_BUS.register(WaterBottleEventHandler.INSTANCE);
    }

    @Nullable
    public static FluidStack drainBlock(World world, BlockPos pos, boolean doDrain) {
        return drainBlock(WorldPlugin.getBlockState(world, pos), world, pos, doDrain);
    }

    @Nullable
    public static FluidStack drainBlock(IBlockState state, World world, BlockPos pos, boolean doDrain) {
        FluidStack fluid;
        if ((fluid = drainForgeFluid(state, world, pos, doDrain)) != null)
            return fluid;
        else if ((fluid = drainVanillaFluid(state, world, pos, doDrain, Fluids.WATER, Blocks.WATER, Blocks.FLOWING_WATER)) != null)
            return fluid;
        else if ((fluid = drainVanillaFluid(state, world, pos, doDrain, Fluids.LAVA, Blocks.LAVA, Blocks.FLOWING_LAVA)) != null)
            return fluid;
        return null;
    }

    @Nullable
    private static FluidStack drainForgeFluid(IBlockState state, World world, BlockPos pos, boolean doDrain) {
        if (state.getBlock() instanceof IFluidBlock) {
            IFluidBlock fluidBlock = (IFluidBlock) state.getBlock();
            if (fluidBlock.canDrain(world, pos))
                return fluidBlock.drain(world, pos, doDrain);
        }
        return null;
    }

    @Nullable
    private static FluidStack drainVanillaFluid(IBlockState state, World world, BlockPos pos, boolean doDrain, Fluids fluid, Block... blocks) {
        boolean matches = false;
        for (Block block : blocks) {
            if (state.getBlock() == block)
                matches = true;
        }
        if (!matches)
            return null;
        if (!(state.getBlock() instanceof BlockLiquid))
            return null;
        int level = state.getValue(BlockLiquid.LEVEL);
        if (level != 0)
            return null;
        if (doDrain)
            WorldPlugin.isBlockAir(world, pos);
        return fluid.getBucket();
    }

    public static boolean isFullFluidBlock(World world, BlockPos pos) {
        return isFullFluidBlock(WorldPlugin.getBlockState(world, pos), world, pos);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean isFullFluidBlock(IBlockState state, World world, BlockPos pos) {
        if (state.getBlock() instanceof BlockLiquid)
            return state.getValue(BlockLiquid.LEVEL) == 0;
        if (state.getBlock() instanceof IFluidBlock)
            return Math.abs(((IFluidBlock) state.getBlock()).getFilledPercentage(world, pos)) == 1.0;
        return false;
    }

    @Nullable
    public static Fluid getFluid(Block block) {
        if (block instanceof IFluidBlock)
            return ((IFluidBlock) block).getFluid();
        else if (block == Blocks.WATER || block == Blocks.FLOWING_WATER)
            return FluidRegistry.WATER;
        else if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA)
            return FluidRegistry.LAVA;
        return null;
    }

    @Nullable
    public static Fluid getFluid(IBlockState state) {
        return getFluid(state.getBlock());
    }

    @SideOnly(Side.CLIENT)
    public static void drip(World world, BlockPos pos, IBlockState state, Random rand, float particleRed, float particleGreen, float particleBlue) {
        if (rand.nextInt(10) == 0 && world.isSideSolid(pos.down(), EnumFacing.UP) && !WorldPlugin.getBlockMaterial(world, pos.down(2)).blocksMovement()) {
            double px = (double) ((float) pos.getX() + rand.nextFloat());
            double py = (double) pos.getY() - 1.05D;
            double pz = (double) ((float) pos.getZ() + rand.nextFloat());

            Particle fx = new ParticleDrip(world, new Vec3d(px, py, pz), particleRed, particleGreen, particleBlue);
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
        }
    }

    public static boolean testProperties(boolean all, @Nullable IFluidHandler fluidHandler, Predicate<IFluidTankProperties> test) {
        if (fluidHandler == null)
            return false;
        IFluidTankProperties[] properties = fluidHandler.getTankProperties();
        if (all)
            return Arrays.stream(properties).allMatch(test);
        return Arrays.stream(properties).anyMatch(test);
    }

    static final class WaterBottleEventHandler {
        static final WaterBottleEventHandler INSTANCE = new WaterBottleEventHandler();
        int amount;

        private WaterBottleEventHandler() {
        }

        public void onAttachCapability(AttachCapabilitiesEvent<ItemStack> event) {
            if (event.getObject().getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(event.getObject()) == PotionTypes.WATER) {
                event.addCapability(RailcraftConstantsAPI.locationOf("water_bottle_container"), new WaterBottleCapabilityDispatcer(event.getObject()));
            }
        }
    }

    //TODO fix crap
    private static final class WaterBottleCapabilityDispatcer extends FluidBucketWrapper {
        private WaterBottleCapabilityDispatcer(ItemStack container) {
            super(container);
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        protected void setFluid(@Nullable Fluid fluid) {
            if (fluid == null) {
                container.deserializeNBT(new ItemStack(Items.GLASS_BOTTLE).serializeNBT());
            } else {
                super.setFluid(fluid);
            }
        }

        @Override
        protected void setFluid(@Nullable FluidStack fluidStack) {
            super.setFluid(fluidStack);
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (InvTools.sizeOf(container) != 1 || resource == null || resource.amount < WaterBottleEventHandler.INSTANCE.amount)
            {
                return null;
            }

            FluidStack fluidStack = getFluid();
            if (fluidStack != null && fluidStack.isFluidEqual(resource))
            {
                if (doDrain)
                {
                    setFluid((FluidStack) null);
                }
                return fluidStack;
            }

            return null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (container.getCount() != 1 || maxDrain < WaterBottleEventHandler.INSTANCE.amount)
            {
                return null;
            }

            FluidStack fluidStack = getFluid();
            if (fluidStack != null)
            {
                if (doDrain)
                {
                    setFluid((FluidStack) null);
                }
                return fluidStack;
            }

            return null;
        }

        @Override
        public FluidStack getFluid() {
            return new FluidStack(FluidRegistry.WATER, WaterBottleEventHandler.INSTANCE.amount);
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new FluidTankProperties[]{new FluidTankProperties(getFluid(), WaterBottleEventHandler.INSTANCE.amount)};
        }
    }
}
