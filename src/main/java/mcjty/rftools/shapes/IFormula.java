package mcjty.rftools.shapes;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public interface IFormula {

    void setup(BlockPos thisCoord, BlockPos dimension, BlockPos offset, NBTTagCompound card);

    boolean isInside(int x, int y, int z);

    /// Return the blockstate resulting from the last isInside test
    default IBlockState getLastState() { return null; }

    default boolean isBorder(int x, int y, int z) {
        if (!isInside(x - 1, y, z) || !isInside(x + 1, y, z) || !isInside(x, y, z - 1) ||
                !isInside(x, y, z + 1) || !isInside(x, y - 1, z) || !isInside(x, y + 1, z)) {
            return isInside(x, y, z);
        }
        return false;
    }

    default boolean isCustom() { return false; }

    default IFormula correctFormula(boolean solid) {
        if (solid) {
            return this;
        } else {
            return new IFormula() {
                @Override
                public void setup(BlockPos thisCoord, BlockPos dimension, BlockPos offset, NBTTagCompound card) {
                    IFormula.this.setup(thisCoord, dimension, offset, card);
                }

                @Override
                public IBlockState getLastState() {
                    return IFormula.this.getLastState();
                }

                @Override
                public boolean isInside(int x, int y, int z) {
                    return IFormula.this.isBorder(x, y, z);
                }

                @Override
                public boolean isBorder(int x, int y, int z) {
                    return IFormula.this.isBorder(x, y, z);
                }
            };
        }
    }
}
