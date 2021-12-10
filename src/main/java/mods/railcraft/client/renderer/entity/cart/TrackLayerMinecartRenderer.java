package mods.railcraft.client.renderer.entity.cart;

import mods.railcraft.Railcraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TrackLayerMinecartRenderer extends MaintenanceMinecartRenderer {

  public TrackLayerMinecartRenderer(EntityRendererProvider.Context context) {
    super(context,
        new ResourceLocation(Railcraft.ID, "textures/entity/minecart/track_layer_contents.png"));
  }
}
