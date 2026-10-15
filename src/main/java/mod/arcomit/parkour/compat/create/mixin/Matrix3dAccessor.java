package mod.arcomit.parkour.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.collision.Matrix3d", remap = false)
public interface Matrix3dAccessor {
    @Accessor("m00") void parkour$setM00(double value);
    @Accessor("m01") void parkour$setM01(double value);
    @Accessor("m02") void parkour$setM02(double value);
    @Accessor("m10") void parkour$setM10(double value);
    @Accessor("m11") void parkour$setM11(double value);
    @Accessor("m12") void parkour$setM12(double value);
    @Accessor("m20") void parkour$setM20(double value);
    @Accessor("m21") void parkour$setM21(double value);
    @Accessor("m22") void parkour$setM22(double value);
}
