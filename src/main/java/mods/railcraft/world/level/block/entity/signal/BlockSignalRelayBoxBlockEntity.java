package mods.railcraft.world.level.block.entity.signal;

import mods.railcraft.api.signal.BlockSignal;
import mods.railcraft.api.signal.SignalAspect;
import mods.railcraft.api.signal.SignalController;
import mods.railcraft.api.signal.SignalControllerProvider;
import mods.railcraft.api.signal.SimpleBlockSignalNetwork;
import mods.railcraft.api.signal.SimpleSignalController;
import mods.railcraft.util.PowerUtil;
import mods.railcraft.world.level.block.entity.RailcraftBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSignalRelayBoxBlockEntity extends ActionSignalBoxBlockEntity
    implements BlockSignal, SignalControllerProvider {

  private final SimpleSignalController signalController =
      new SimpleSignalController(1, this::syncToClient, this, false);
  private final SimpleBlockSignalNetwork blockSignal =
      new SimpleBlockSignalNetwork(2, this::syncToClient, this::signalAspectChanged, this);

  public BlockSignalRelayBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
    super(RailcraftBlockEntityTypes.BLOCK_SIGNAL_RELAY_BOX.get(), blockPos, blockState);
  }

  @Override
  public void setRemoved() {
    super.setRemoved();
    this.signalController.removed();
    this.blockSignal.removed();
  }

  @Override
  public void onLoad() {
    super.onLoad();
    this.signalController.refresh();
    this.blockSignal.refresh();
  }

  private void signalAspectChanged(SignalAspect signalAspect) {
    this.signalController.setSignalAspect(signalAspect);
    this.updateNeighborSignalBoxes(false);
  }

  @Override
  public int getRedstoneSignal(Direction side) {
    return this.isActionSignalAspect(this.blockSignal.getSignalAspect())
        ? PowerUtil.FULL_POWER
        : PowerUtil.NO_POWER;
  }

  @Override
  protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.put("blockSignal", this.blockSignal.serializeNBT());
    tag.put("signalController", this.signalController.serializeNBT());
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    this.blockSignal.deserializeNBT(tag.getCompound("blockSignal"));
    this.signalController.deserializeNBT(tag.getCompound("signalController"));
  }

  @Override
  public void writeSyncData(FriendlyByteBuf data) {
    super.writeSyncData(data);
    this.blockSignal.writeSyncData(data);
    this.signalController.writeSyncData(data);
  }

  @Override
  public void readSyncData(FriendlyByteBuf data) {
    super.readSyncData(data);
    this.blockSignal.readSyncData(data);
    this.signalController.readSyncData(data);
  }

  @Override
  public SimpleBlockSignalNetwork getSignalNetwork() {
    return this.blockSignal;
  }

  @Override
  public SignalAspect getSignalAspect(Direction direction) {
    return this.signalController.getSignalAspect();
  }

  @Override
  public SignalController getSignalController() {
    return this.signalController;
  }

  public static void clientTick(Level level, BlockPos blockPos, BlockState blockState,
      BlockSignalRelayBoxBlockEntity blockEntity) {
    blockEntity.signalController.spawnTuningAuraParticles();
  }

  public static void serverTick(Level level, BlockPos blockPos, BlockState blockState,
      BlockSignalRelayBoxBlockEntity blockEntity) {
    blockEntity.blockSignal.serverTick();
  }
}
