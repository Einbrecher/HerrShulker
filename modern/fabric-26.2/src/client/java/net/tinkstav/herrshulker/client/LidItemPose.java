package net.tinkstav.herrshulker.client;

import org.joml.Matrix4f;

/**
 * Builds the lid-item placement matrix for the ITEM render path (held + GUI), where the lid item is
 * appended as a sibling layer of the shulker-box render state and shares the box layer's display
 * transform (see {@code MixinSpecialModelWrapper}).
 *
 * <p><b>Why this is separate from the world path.</b> The world render
 * ({@code MixinShulkerBoxRenderer}) is facing-parametric (any block orientation + lid-open progress),
 * {@code PoseStack}-based, and draws the lid via {@link net.minecraft.world.item.ItemDisplayContext#FIXED}
 * — whose display transform applies its own ~0.5x scale. The item path is the fixed "closed, up-facing"
 * special case, {@link Matrix4f}-based, and shares the box layer's display transform (no FIXED scale),
 * so its scale factors and seats are tuned independently to land the same on-screen size. The shared
 * basis between the two is only the clocking rotation (pitch {@value #CLOCK_PITCH_DEG}, yaw
 * {@value #CLOCK_YAW_DEG}) and the config {@code "block"} scale. They are intentionally NOT merged into
 * one helper: unifying two different mechanisms would risk the always-working world render for little
 * gain. Centralising the item-path constants here keeps the pose auditable and drift-resistant.
 */
public final class LidItemPose {
    private LidItemPose() {}

    /** Lay the item flat on the lid — matches the world path's XP(-90). */
    private static final float CLOCK_PITCH_DEG = -90.0F;
    /** Base yaw aligning the lid item with the world render ("clocking"). */
    private static final float CLOCK_YAW_DEG = 180.0F;
    /** Extra in-plane spin on the lid surface (final user-tuned orientation). */
    private static final float SPIN_DEG = -90.0F;

    /** 3D-block lid scale, relative to the world {@code "block"} config scale. */
    private static final float BLOCK_SCALE_FACTOR = 0.6875F;
    /** Flat-sprite lid scale — larger, because a 2D sprite reads smaller when laid flat. */
    private static final float FLAT_SCALE_FACTOR = 1.125F;

    /** A 3D block is centred on the box top (y=1.0) so its middle intersects the lid, like the world render. */
    private static final float BLOCK_SEAT_Y = 1.0F;
    /** A flat sprite (≈no thickness) seats just above the top surface. */
    private static final float FLAT_SEAT_Y = 1.01F;

    /**
     * Placement matrix for a 3D-block lid item.
     *
     * @param baseScale the world {@code "block"} config scale (HerrShulkerClient.Config.getLidItemScale("block"))
     */
    public static Matrix4f forBlock(float baseScale) {
        return build(BLOCK_SEAT_Y, baseScale * BLOCK_SCALE_FACTOR);
    }

    /**
     * Placement matrix for a flat-sprite lid item.
     *
     * @param baseScale the world {@code "block"} config scale
     */
    public static Matrix4f forFlat(float baseScale) {
        return build(FLAT_SEAT_Y, baseScale * FLAT_SCALE_FACTOR);
    }

    /**
     * Compose (read top→bottom = outer→inner): seat the item on the lid, spin it in-plane, scale it,
     * lay it flat (pitch), clock its yaw, then re-centre the unit-cube model on its origin. The result
     * is expressed in box MODEL space; the shared box display transform then projects it onto the box's
     * top face in whatever view (isometric GUI, or the hand pose).
     */
    private static Matrix4f build(float seatY, float scale) {
        return new Matrix4f()
                .translate(0.5F, seatY, 0.5F)
                .rotateY((float) Math.toRadians(SPIN_DEG))
                .scale(scale)
                .rotateX((float) Math.toRadians(CLOCK_PITCH_DEG))
                .rotateY((float) Math.toRadians(CLOCK_YAW_DEG))
                .translate(-0.5F, -0.5F, -0.5F);
    }
}
