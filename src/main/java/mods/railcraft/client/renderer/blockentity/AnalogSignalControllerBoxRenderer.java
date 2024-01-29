package mods.railcraft.client.renderer.blockentity;

import mods.railcraft.api.core.RailcraftConstants;
import net.minecraft.resources.ResourceLocation;

public class AnalogSignalControllerBoxRenderer extends AbstractSignalBoxRenderer {

  public static final ResourceLocation TEXTURE_LOCATION =
      RailcraftConstants.rl("entity/signal_box/analog_signal_controller_box");

  @Override
  protected ResourceLocation getTopTextureLocation() {
    return TEXTURE_LOCATION;
  }
}
