package mcjty.rftools.shapes;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.rftools.items.builder.ShapeCardItem;
import mcjty.rftools.network.RFToolsMessages;
import mcjty.rftools.varia.RLE;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Map;

public class PacketRequestShapeData implements IMessage {
    private ItemStack card;

    @Override
    public void fromBytes(ByteBuf buf) {
        card = NetworkTools.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writeItemStack(buf, card);
    }

    public PacketRequestShapeData() {
    }

    public PacketRequestShapeData(ItemStack card) {
        this.card = card;
    }

    public static class Handler implements IMessageHandler<PacketRequestShapeData, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestShapeData message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
            return null;
        }

        private void handle(PacketRequestShapeData message, MessageContext ctx) {

            Shape shape = ShapeCardItem.getShape(message.card);
            boolean solid = ShapeCardItem.isSolid(message.card);
            BlockPos dimension = ShapeCardItem.getDimension(message.card);

            if (dimension.getX()*dimension.getY()*dimension.getZ() > (200 * 200 * 200)) {
                RFToolsMessages.INSTANCE.sendTo(new PacketReturnShapeData(null, null, dimension, 0, "Too large for preview!"), ctx.getServerHandler().player);
                return;
            }

            RLE positions = new RLE();
            StatePalette statePalette = new StatePalette();
            int cnt = ShapeCardItem.getRenderPositions(message.card, shape, solid, positions, statePalette);

            RFToolsMessages.INSTANCE.sendTo(new PacketReturnShapeData(positions, statePalette, dimension, cnt, ""), ctx.getServerHandler().player);
        }
    }

    static boolean isPositionEnclosed(Map<Long, IBlockState> positions, BlockPos coordinate) {
        return positions.containsKey(coordinate.up().toLong()) &&
                positions.containsKey(coordinate.down().toLong()) &&
                positions.containsKey(coordinate.east().toLong()) &&
                positions.containsKey(coordinate.west().toLong()) &&
                positions.containsKey(coordinate.south().toLong()) &&
                positions.containsKey(coordinate.north().toLong());
    }

}
