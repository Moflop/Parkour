package mod.arcomit.parkour.compat.create.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.sync.ClientMotionPacket;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider.CollisionResponse;
import com.simibubi.create.foundation.collision.OrientedBB;
import mod.arcomit.parkour.compat.create.CreateMovementCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.ContraptionCollider", remap = false)
public abstract class ContraptionColliderMixin {
    @WrapOperation(method = "collideEntities", at = @At(value = "NEW",
            target = "(Lnet/minecraft/world/phys/Vec3;ZF)Lcom/simibubi/create/content/contraptions/sync/ClientMotionPacket;"),
            remap = false)
    private static ClientMotionPacket parkour$syncCollisionState(Vec3 motion, boolean grounded,
            float limbSwing, Operation<ClientMotionPacket> original, @Local Entity entity) {
        if (CreateMovementCompat.isEnabled(entity)) {
            grounded = entity.onGround();
        }
        return original.call(motion, grounded, limbSwing);
    }

    @WrapOperation(method = "collideEntities", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider;collideMany(Lcom/simibubi/create/foundation/collision/CollisionList;Lcom/simibubi/create/foundation/collision/CollisionList;Lcom/simibubi/create/foundation/collision/OrientedBB;Lnet/minecraft/world/phys/Vec3;FZ)Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider$CollisionResponse;"), remap = false)
    private static CollisionResponse parkour$validatePlayerCollision(CollisionList shapes,
            CollisionList scratch, OrientedBB body, Vec3 motion, float step, boolean horizontalPass,
            Operation<CollisionResponse> original,
            @Local(argsOnly = true) AbstractContraptionEntity contraption,
            @Local Entity entity) {
        if (!CreateMovementCompat.isEnabled(entity)) {
            return original.call(shapes, scratch, body, motion, step, horizontalPass);
        }
        return CreateMovementCompat.collide((Player) entity, contraption, shapes,
                scratch, body, motion, horizontalPass);
    }
}
