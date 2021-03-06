package xyz.przemyk.simpleplanes.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import xyz.przemyk.simpleplanes.entities.PlaneEntity;
import xyz.przemyk.simpleplanes.setup.SimplePlanesUpgrades;
import xyz.przemyk.simpleplanes.upgrades.Upgrade;
import xyz.przemyk.simpleplanes.upgrades.UpgradeType;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class PlaneItem extends Item {

    private static final Predicate<Entity> ENTITY_PREDICATE = EntityPredicates.NOT_SPECTATING.and(Entity::canBeCollidedWith);
    private final Function<World, PlaneEntity> planeSupplier;

    public PlaneItem(Properties properties, Function<World, PlaneEntity> planeSupplier) {
        super(properties.maxStackSize(1));
        this.planeSupplier = planeSupplier;
    }

    @Override
    public ITextComponent getName() {
        return super.getName();
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return super.hasEffect(stack) || stack.getChildTag("EntityTag") != null;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, RayTraceContext.FluidMode.ANY);
        if (raytraceresult.getType() == RayTraceResult.Type.MISS) {
            return ActionResult.resultPass(itemstack);
        } else {
            Vector3d vec3d = playerIn.getLook(1.0F);
            List<Entity> list = worldIn.getEntitiesInAABBexcluding(playerIn, playerIn.getBoundingBox().expand(vec3d.scale(5.0D)).grow(1.0D), ENTITY_PREDICATE);
            if (!list.isEmpty()) {
                Vector3d vec3d1 = playerIn.getEyePosition(1.0F);

                for (Entity entity : list) {
                    AxisAlignedBB axisalignedbb = entity.getBoundingBox().grow(entity.getCollisionBorderSize());
                    if (axisalignedbb.contains(vec3d1)) {
                        return ActionResult.resultPass(itemstack);
                    }
                }
            }

            if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
                PlaneEntity planeEntity = planeSupplier.apply(worldIn);
                UpgradeType coalEngine = SimplePlanesUpgrades.COAL_ENGINE.get();
                Upgrade upgrade = coalEngine.instanceSupplier.apply(planeEntity);
                if (itemstack.getChildTag("Used")==null){
                    planeEntity.upgrades.put(coalEngine.getRegistryName(), upgrade);
                    planeEntity.upgradeChanged();
                }
                planeEntity.setPosition(raytraceresult.getHitVec().getX(), raytraceresult.getHitVec().getY(), raytraceresult.getHitVec().getZ());
                planeEntity.rotationYaw = playerIn.rotationYaw;
                planeEntity.prevRotationYaw = playerIn.prevRotationYaw;
                planeEntity.setCustomName(itemstack.getDisplayName());
                CompoundNBT entityTag = itemstack.getChildTag("EntityTag");
                if (entityTag != null) {
                    planeEntity.readAdditional(entityTag);
                }
                if (!worldIn.hasNoCollisions(planeEntity, planeEntity.getBoundingBox().grow(-0.1D))) {
                    return ActionResult.resultFail(itemstack);
                } else {
                    if (!worldIn.isRemote) {
                        worldIn.addEntity(planeEntity);
                        if (!playerIn.abilities.isCreativeMode) {
                            itemstack.shrink(1);
                        }
                    }
                    playerIn.addStat(Stats.ITEM_USED.get(this));
                    return ActionResult.resultSuccess(itemstack);
                }
            } else {
                return ActionResult.resultPass(itemstack);
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey(stack);
    }

}
