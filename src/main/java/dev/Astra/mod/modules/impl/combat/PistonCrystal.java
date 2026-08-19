package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.player.MovementUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.ClientSetting;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.FacingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.Item;

public class PistonCrystal extends Module {
   public static PistonCrystal INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting noEating = this.add(new BooleanSetting("NoEating", true));
   private final BooleanSetting eatingBreak = this.add(new BooleanSetting("EatingBreak", false));
   private final BooleanSetting aggressive = this.add(new BooleanSetting("Aggressive", false));
   private final SliderSetting burst;
   private final BooleanSetting forceBreak;
   private final SliderSetting placeRange;
   private final SliderSetting range;
   private final BooleanSetting fire;
   private final BooleanSetting switchPos;
   private final BooleanSetting onlyGround;
   private final BooleanSetting onlyStatic;
   private final SliderSetting updateDelay;
   private final SliderSetting posUpdateDelay;
   private final SliderSetting stageSetting;
   private final SliderSetting pistonStage;
   private final SliderSetting pistonMaxStage;
   private final SliderSetting powerStage;
   private final SliderSetting powerMaxStage;
   private final SliderSetting crystalStage;
   private final SliderSetting crystalMaxStage;
   private final SliderSetting fireStage;
   private final SliderSetting fireMaxStage;
   private final BooleanSetting inventory;
   private final BooleanSetting debug;
   private final Timer timer;
   private final Timer crystalTimer;
   public BlockPos bestPos;
   public BlockPos bestOPos;
   public Direction bestFacing;
   public double getDistance;
   private double bestDistanceSq;
   public boolean getPos;
   public int stage;
   private PlayerEntity target;
   private boolean isPiston;
   private int cachedPistonSlot;
   private int cachedPowerSlot;
   private int cachedCrystalSlot;
   private int cachedFireSlot;
   private int slotCacheTick;

   public PistonCrystal() {
      super("PistonCrystal", Module.Category.Combat);
      BooleanSetting var10008 = this.aggressive;
      Objects.requireNonNull(var10008);
      this.burst = this.add(new SliderSetting("Burst", 2, 1, 5, var10008::getValue));
      BooleanSetting var10006 = this.aggressive;
      Objects.requireNonNull(var10006);
      this.forceBreak = this.add(new BooleanSetting("ForceBreak", true, var10006::getValue));
      this.placeRange = this.add(new SliderSetting("PlaceRange", (double)5.0F, (double)1.0F, (double)8.0F));
      this.range = this.add(new SliderSetting("Range", (double)4.0F, (double)1.0F, (double)8.0F));
      this.fire = this.add(new BooleanSetting("Fire", true));
      this.switchPos = this.add(new BooleanSetting("Switch", false));
      this.onlyGround = this.add(new BooleanSetting("SelfGround", true));
      this.onlyStatic = this.add(new BooleanSetting("MovingPause", true));
      this.updateDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 500));
      this.posUpdateDelay = this.add(new SliderSetting("PosUpdateDelay", 500, 0, 1000));
      this.stageSetting = this.add(new SliderSetting("Stage", 4, 1, 10));
      this.pistonStage = this.add(new SliderSetting("PistonStage", 1, 1, 10));
      this.pistonMaxStage = this.add(new SliderSetting("PistonMaxStage", 1, 1, 10));
      this.powerStage = this.add(new SliderSetting("PowerStage", 3, 1, 10));
      this.powerMaxStage = this.add(new SliderSetting("PowerMaxStage", 3, 1, 10));
      this.crystalStage = this.add(new SliderSetting("CrystalStage", 4, 1, 10));
      this.crystalMaxStage = this.add(new SliderSetting("CrystalMaxStage", 4, 1, 10));
      this.fireStage = this.add(new SliderSetting("FireStage", 2, 1, 10));
      this.fireMaxStage = this.add(new SliderSetting("FireMaxStage", 2, 1, 10));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true));
      this.debug = this.add(new BooleanSetting("Debug", false));
      this.timer = new Timer();
      this.crystalTimer = new Timer();
      this.bestPos = null;
      this.bestOPos = null;
      this.bestFacing = null;
      this.getDistance = (double)100.0F;
      this.bestDistanceSq = Double.POSITIVE_INFINITY;
      this.getPos = false;
      this.stage = 1;
      this.target = null;
      this.isPiston = false;
      this.cachedPistonSlot = -1;
      this.cachedPowerSlot = -1;
      this.cachedCrystalSlot = -1;
      this.cachedFireSlot = -1;
      this.slotCacheTick = -1;
      this.setChinese("活塞水晶");
      INSTANCE = this;
   }

   private static boolean canFire(BlockPos pos) {
      if (BlockUtil.canReplace(pos.down())) {
         return false;
      } else if (!mc.world.isAir(pos)) {
         return false;
      } else {
         return !BlockUtil.canClick(pos.offset(Direction.DOWN)) ? false : BlockUtil.isStrictDirection(pos.down(), Direction.UP);
      }
   }

   public void onTick() {
      if (this.pistonStage.getValue() > this.stageSetting.getValue()) {
         this.pistonStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireStage.getValue() > this.stageSetting.getValue()) {
         this.fireStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerStage.getValue() > this.stageSetting.getValue()) {
         this.powerStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalStage.getValue() > this.stageSetting.getValue()) {
         this.crystalStage.setValue(this.stageSetting.getValue());
      }

      if (this.pistonMaxStage.getValue() > this.stageSetting.getValue()) {
         this.pistonMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireMaxStage.getValue() > this.stageSetting.getValue()) {
         this.fireMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerMaxStage.getValue() > this.stageSetting.getValue()) {
         this.powerMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() > this.stageSetting.getValue()) {
         this.crystalMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() < this.crystalStage.getValue()) {
         this.crystalStage.setValue(this.crystalMaxStage.getValue());
      }

      if (this.powerMaxStage.getValue() < this.powerStage.getValue()) {
         this.powerStage.setValue(this.powerMaxStage.getValue());
      }

      if (this.pistonMaxStage.getValue() < this.pistonStage.getValue()) {
         this.pistonStage.setValue(this.pistonMaxStage.getValue());
      }

      if (this.fireMaxStage.getValue() < this.fireStage.getValue()) {
         this.fireStage.setValue(this.fireMaxStage.getValue());
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (ForceEat.INSTANCE.isEating()) {
         return;
      }
      this.onTick();
      this.target = CombatUtil.getClosestEnemy(this.range.getValue());
      if (this.target != null) {
         boolean aggressive = this.aggressive.getValue();
         if (!this.noEating.getValue() || !mc.player.isUsingItem() || aggressive) {
            this.refreshSlots();
            if (!this.check(this.onlyStatic.getValue(), !mc.player.isOnGround(), this.onlyGround.getValue())) {
               BlockPos pos = EntityUtil.getEntityPos(this.target, true);
               if (!mc.player.isUsingItem() || this.eatingBreak.getValue() || aggressive) {
                  if (this.checkCrystal(pos.up(0))) {
                     CombatUtil.attackCrystal(pos.up(0), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.up(1))) {
                     CombatUtil.attackCrystal(pos.up(1), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.up(2))) {
                     CombatUtil.attackCrystal(pos.up(2), this.rotate.getValue(), true);
                  }
               }

               if (this.bestPos != null && mc.world.getBlockState(this.bestPos).getBlock() instanceof PistonBlock) {
                  this.isPiston = true;
               } else if (this.isPiston) {
                  this.isPiston = false;
                  this.crystalTimer.reset();
                  this.bestPos = null;
                  this.bestOPos = null;
                  this.bestFacing = null;
                  this.getPos = false;
                  this.getDistance = (double)100.0F;
                  this.bestDistanceSq = Double.POSITIVE_INFINITY;
               }

               if (aggressive || this.crystalTimer.passedMs((double)this.posUpdateDelay.getValueInt())) {
                  this.stage = 0;
                  this.getDistance = (double)100.0F;
                  this.bestDistanceSq = Double.POSITIVE_INFINITY;
                  this.getPos = false;
                  this.getBestPos(pos.up(2));
                  this.getBestPos(pos.up());
               }

               if (aggressive || this.timer.passedMs((double)this.updateDelay.getValueInt())) {
                  if (this.getPos && this.bestPos != null) {
                     this.timer.reset();
                     if (this.debug.getValue()) {
                        String var10001 = String.valueOf(this.bestPos);
                        this.sendMessage("[Debug] PistonPos:" + var10001 + " Facing:" + String.valueOf(this.bestFacing) + " CrystalPos:" + String.valueOf(this.bestOPos.offset(this.bestFacing)));
                     }

                     if (aggressive) {
                        int burst = this.burst.getValueInt();

                        for(int i = 0; i < burst; ++i) {
                           this.doPistonAuraAggressive(this.bestPos, this.bestFacing, this.bestOPos);
                        }
                     } else {
                        this.doPistonAura(this.bestPos, this.bestFacing, this.bestOPos);
                     }
                  }

               }
            }
         }
      }
   }

   public boolean check(boolean onlyStatic, boolean onGround, boolean onlyGround) {
      if (MovementUtil.isMoving() && onlyStatic) {
         return true;
      } else if (onGround && onlyGround) {
         return true;
      } else if (this.cachedPowerSlot == -1) {
         return true;
      } else if (this.cachedPistonSlot == -1) {
         return true;
      } else {
         return this.cachedCrystalSlot == -1;
      }
   }

   private boolean checkCrystal(BlockPos pos) {
      if (this.aggressive.getValue() && this.forceBreak.getValue()) {
         return !BlockUtil.getEndCrystals(new Box(pos)).isEmpty();
      } else {
         for(EndCrystalEntity entity : BlockUtil.getEndCrystals(new Box(pos))) {
            if (AutoCrystal.INSTANCE.calculateDamage(entity.getPos(), this.target, this.target) > 7.0F) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean checkCrystal2(BlockPos pos) {
      for(EndCrystalEntity entity : BlockUtil.getEndCrystals(new Box(pos))) {
         if (EntityUtil.getEntityPos(entity).equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private void getBestPos(BlockPos pos) {
      for(Direction i : Direction.values()) {
         if (i != Direction.DOWN && i != Direction.UP) {
            this.getPos(pos, i);
         }
      }

   }

   private void getPos(BlockPos pos, Direction i) {
      BlockPos crystalPos = pos.offset(i);
      if (this.checkCrystal2(crystalPos) || BlockUtil.canPlaceCrystal(crystalPos)) {
         int sideX = i.getOffsetZ();
         int sideZ = i.getOffsetX();
         BlockPos base3 = pos.offset(i, 3);
         BlockPos base2 = pos.offset(i, 2);
         this.getPos(base3, i, pos);
         this.getPos(base3.up(), i, pos);
         this.getPos(base3.add(sideX, 0, sideZ), i, pos);
         this.getPos(base3.add(-sideX, 0, -sideZ), i, pos);
         this.getPos(base3.add(sideX, 1, sideZ), i, pos);
         this.getPos(base3.add(-sideX, 1, -sideZ), i, pos);
         this.getPos(base2, i, pos);
         this.getPos(base2.up(), i, pos);
         this.getPos(base2.add(sideX, 0, sideZ), i, pos);
         this.getPos(base2.add(-sideX, 0, -sideZ), i, pos);
         this.getPos(base2.add(sideX, 1, sideZ), i, pos);
         this.getPos(base2.add(-sideX, 1, -sideZ), i, pos);
      }
   }

   private void getPos(BlockPos pos, Direction facing, BlockPos oPos) {
      if (!this.switchPos.getValue() || this.bestPos == null || !this.bestPos.equals(pos) || !mc.world.isAir(this.bestPos)) {
         if (this.cachedPistonSlot != -1) {
            double eyeX = mc.player.getX();
            double eyeY = mc.player.getEyeY();
            double eyeZ = mc.player.getZ();
            double dx = (double)pos.getX() + (double)0.5F - eyeX;
            double dy = (double)pos.getY() + (double)0.5F - eyeY;
            double dz = (double)pos.getZ() + (double)0.5F - eyeZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (!this.getPos || !(distSq >= this.bestDistanceSq)) {
               Block block = this.getBlock(pos);
               boolean canPlace = !(block instanceof PistonBlock) && BlockUtil.canPlace(pos, this.placeRange.getValue());
               if (canPlace || block instanceof PistonBlock) {
                  if (!ClientSetting.INSTANCE.lowVersion.getValue() || block instanceof PistonBlock || !(mc.player.getY() - (double)pos.getY() <= (double)-2.0F) && !(mc.player.getY() - (double)pos.getY() >= (double)3.0F) || !(BlockUtil.distanceToXZ((double)pos.getX() + (double)0.5F, (double)pos.getZ() + (double)0.5F) < 2.6)) {
                     BlockPos backPos = pos.offset(facing.getOpposite());
                     Block backBlock = this.getBlock(backPos);
                     if (mc.world.isAir(backPos) || backBlock == Blocks.FIRE || backBlock == Blocks.MOVING_PISTON) {
                        if (backBlock != Blocks.MOVING_PISTON || this.checkCrystal2(backPos)) {
                           if (canPlace || this.isPiston(pos, facing)) {
                              this.bestPos = pos;
                              this.bestOPos = oPos;
                              this.bestFacing = facing;
                              this.bestDistanceSq = distSq;
                              this.getDistance = (double)MathHelper.sqrt((float)distSq);
                              this.getPos = true;
                              this.crystalTimer.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doPistonAura(BlockPos pos, Direction facing, BlockPos oPos) {
      if ((double)this.stage >= this.stageSetting.getValue()) {
         this.stage = 0;
      }

      ++this.stage;
      if (mc.world.isAir(pos)) {
         if (!BlockUtil.canPlace(pos)) {
            return;
         }

         if ((double)this.stage >= this.pistonStage.getValue() && (double)this.stage <= this.pistonMaxStage.getValue()) {
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            int old = mc.player.getInventory().selectedSlot;
            AutoPush.pistonFacing(facing);
            int piston = this.cachedPistonSlot;
            if (piston == -1) {
               return;
            }

            this.doSwap(piston);
            BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(piston);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

            BlockPos neighbour = pos.offset(side);
            Direction opposite = side.getOpposite();
            if (this.rotate.getValue()) {
               Astra.ROTATION.lookAt(neighbour, opposite);
            }
         }
      }

      if ((double)this.stage >= this.powerStage.getValue() && (double)this.stage <= this.powerMaxStage.getValue()) {
         this.doRedStone(pos, facing, oPos.offset(facing));
      }

      if ((double)this.stage >= this.crystalStage.getValue() && (double)this.stage <= this.crystalMaxStage.getValue()) {
         this.placeCrystal(oPos, facing);
      }

      if ((double)this.stage >= this.fireStage.getValue() && (double)this.stage <= this.fireMaxStage.getValue()) {
         this.doFire(oPos, facing);
      }

   }

   private void doPistonAuraAggressive(BlockPos pos, Direction facing, BlockPos oPos) {
      if (mc.world.isAir(pos)) {
         if (!BlockUtil.canPlace(pos)) {
            return;
         }

         Direction side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return;
         }

         int old = mc.player.getInventory().selectedSlot;
         AutoPush.pistonFacing(facing);
         int piston = this.cachedPistonSlot;
         if (piston == -1) {
            return;
         }

         this.doSwap(piston);
         BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
         if (this.inventory.getValue()) {
            this.doSwap(piston);
            EntityUtil.syncInventory();
         } else {
            this.doSwap(old);
         }

         BlockPos neighbour = pos.offset(side);
         Direction opposite = side.getOpposite();
         if (this.rotate.getValue()) {
            Astra.ROTATION.lookAt(neighbour, opposite);
         }

         if (mc.world.isAir(pos)) {
            return;
         }
      }

      this.doRedStone(pos, facing, oPos.offset(facing));
      this.placeCrystal(oPos, facing);
      this.doFire(oPos, facing);
   }

   private void placeCrystal(BlockPos pos, Direction facing) {
      if (BlockUtil.canPlaceCrystal(pos.offset(facing))) {
         int crystal = this.cachedCrystalSlot;
         if (crystal != -1) {
            int old = mc.player.getInventory().selectedSlot;
            this.doSwap(crystal);
            BlockUtil.placeCrystal(pos.offset(facing), true);
            if (this.inventory.getValue()) {
               this.doSwap(crystal);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

         }
      }
   }

   private boolean isPiston(BlockPos pos, Direction facing) {
      if (!(mc.world.getBlockState(pos).getBlock() instanceof PistonBlock)) {
         return false;
      } else if (((Direction)mc.world.getBlockState(pos).get(FacingBlock.FACING)).getOpposite() != facing) {
         return false;
      } else {
         return mc.world.isAir(pos.offset(facing, -1)) || this.getBlock(pos.offset(facing, -1)) == Blocks.FIRE || this.getBlock(pos.offset(facing.getOpposite())) == Blocks.MOVING_PISTON;
      }
   }

   private void doFire(BlockPos pos, Direction facing) {
      if (this.fire.getValue()) {
         int fire = this.cachedFireSlot;
         if (fire != -1) {
            int old = mc.player.getInventory().selectedSlot;
            int[] xOffset = new int[]{0, facing.getOffsetZ(), -facing.getOffsetZ()};
            int[] yOffset = new int[]{0, 1};
            int[] zOffset = new int[]{0, facing.getOffsetX(), -facing.getOffsetX()};

            for(int x : xOffset) {
               for(int y : yOffset) {
                  for(int z : zOffset) {
                     if (this.getBlock(pos.add(x, y, z)) == Blocks.FIRE) {
                        return;
                     }
                  }
               }
            }

            for(int x : xOffset) {
               for(int y : yOffset) {
                  for(int z : zOffset) {
                     if (canFire(pos.add(x, y, z))) {
                        this.doSwap(fire);
                        this.placeFire(pos.add(x, y, z));
                        if (this.inventory.getValue()) {
                           this.doSwap(fire);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        return;
                     }
                  }
               }
            }

         }
      }
   }

   public void placeFire(BlockPos pos) {
      BlockPos neighbour = pos.offset(Direction.DOWN);
      BlockUtil.clickBlock(neighbour, Direction.UP, this.rotate.getValue());
   }

   private void doRedStone(BlockPos pos, Direction facing, BlockPos crystalPos) {
      BlockPos backPos = pos.offset(facing.getOpposite());
      Block backBlock = this.getBlock(backPos);
      if (mc.world.isAir(backPos) || backBlock == Blocks.FIRE || backBlock == Blocks.MOVING_PISTON) {
         for(Direction i : Direction.values()) {
            if (this.getBlock(pos.offset(i)) == Blocks.REDSTONE_BLOCK) {
               return;
            }
         }

         int power = this.cachedPowerSlot;
         if (power != -1) {
            if (backBlock != Blocks.MOVING_PISTON || this.checkCrystal2(backPos)) {
               int old = mc.player.getInventory().selectedSlot;
               Direction bestNeighboring = BlockUtil.getBestNeighboring(pos, facing);
               if (bestNeighboring != null && bestNeighboring != facing.getOpposite() && BlockUtil.canPlace(pos.offset(bestNeighboring), this.placeRange.getValue()) && !pos.offset(bestNeighboring).equals(crystalPos)) {
                  this.doSwap(power);
                  BlockUtil.placeBlock(pos.offset(bestNeighboring), this.rotate.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(power);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(old);
                  }

               } else {
                  for(Direction i : Direction.values()) {
                     if (BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue()) && !pos.offset(i).equals(crystalPos) && i != facing.getOpposite()) {
                        this.doSwap(power);
                        BlockUtil.placeBlock(pos.offset(i), this.rotate.getValue());
                        if (this.inventory.getValue()) {
                           this.doSwap(power);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        return;
                     }
                  }

               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private void refreshSlots() {
      if (mc.player != null) {
         int tick = mc.player.age;
         if (this.slotCacheTick != tick) {
            this.slotCacheTick = tick;
            this.cachedPistonSlot = this.findClass(PistonBlock.class);
            this.cachedPowerSlot = this.findBlock(Blocks.REDSTONE_BLOCK);
            this.cachedCrystalSlot = this.findItem(Items.END_CRYSTAL);
            this.cachedFireSlot = this.findItem(Items.FLINT_AND_STEEL);
         }
      }
   }

   public int findItem(Item itemIn) {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(itemIn) : InventoryUtil.findItem(itemIn);
   }

   public int findBlock(Block blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }
}