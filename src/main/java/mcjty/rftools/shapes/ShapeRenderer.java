package mcjty.rftools.shapes;

import mcjty.rftools.blocks.builder.BuilderSetup;
import mcjty.rftools.items.builder.ShapeCardItem;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class ShapeRenderer {

    private int prevX = -1;
    private int prevY = -1;

    private float scale = 3.0f;
    private float dx = 230.0f;
    private float dy = 100.0f;
    private float xangle = 25.0f;
    private float yangle = 25.0f;
    private float zangle = 0.0f;

    private int glList = -1;
    private long checksum = -1;
    private boolean prevShowMat = false;

    private static int waitForNewRequest = 0;
    private static Map<Long, IBlockState> positions = null;
    public static int shapeCount = 0;
    public static String previewMessage = "";

    public static void setRenderData(Map<Long, IBlockState> positions, int count, String msg) {
        ShapeRenderer.positions = positions;//new HashMap<>(positions);
        ShapeRenderer.shapeCount = count;
        ShapeRenderer.previewMessage = msg;
    }

    public void handleShapeDragging(int x, int y) {
        if (x >= 100 && y <= 120) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                if (prevX != -1 && Mouse.isButtonDown(0)) {
                    dx += (x - prevX);
                    dy += (y - prevY);
                }
            } else {
                if (prevX != -1 && Mouse.isButtonDown(0)) {
                    yangle -= (x - prevX);
                    xangle += (y - prevY);
                }
            }
            prevX = x;
            prevY = y;
        }

        if (Mouse.isButtonDown(2)) {
            xangle = 0.0f;
            yangle = 0.0f;
        }
    }

    public void handleMouseWheel() {
        int dwheel = Mouse.getDWheel();
        if (dwheel < 0) {
            scale *= .6;
            if (scale <= 0.1) {
                scale = .1f;
            }
        } else if (dwheel > 0) {
            scale *= 1.4;
        }
    }

    public void invalidateGlList() {
        if (glList != -1) {
            GLAllocation.deleteDisplayLists(glList);
        }
        glList = -1;
    }


    public void renderShape(IShapeParentGui gui, ItemStack stack, int x, int y, boolean showAxis, boolean showOuter, boolean showMat) {
        setupScissor(gui);

        GlStateManager.pushMatrix();

        GlStateManager.translate(dx, dy, 200);
        GlStateManager.rotate(180-xangle, 1f, 0, 0); //xangle += .16f;
        GlStateManager.rotate(yangle, 0, 1f, 0); //yangle += .09f;
        GlStateManager.rotate(zangle, 0, 0, 1f); //zangle += .31f;
        GlStateManager.scale(scale, scale, scale);

        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer buffer = tessellator.getBuffer();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        renderFaces(tessellator, buffer, stack, showMat);
        BlockPos dimension = ShapeCardItem.getDimension(stack);
        renderHelpers(tessellator, buffer, dimension.getX(), dimension.getY(), dimension.getZ(), showAxis, showOuter);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GlStateManager.popMatrix();

        GlStateManager.glLineWidth(3);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x-62, y+180, 0)  .color(1f, 0f, 0f, 1f).endVertex();
        buffer.pos(x-39, y+180, 0)  .color(1f, 0f, 0f, 1f).endVertex();
        buffer.pos(x-62, y+195, 0)  .color(0f, 0.8f, 0f, 1f).endVertex();
        buffer.pos(x-39, y+195, 0)  .color(0f, 0.8f, 0f, 1f).endVertex();
        buffer.pos(x-62, y+210, 0)  .color(0f, 0f, 1f, 1f).endVertex();
        buffer.pos(x-39, y+210, 0)  .color(0f, 0f, 1f, 1f).endVertex();
        tessellator.draw();


        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        RenderHelper.enableGUIStandardItemLighting();

        if (!previewMessage.isEmpty()) {
            Minecraft.getMinecraft().fontRenderer.drawString(previewMessage, gui.getPreviewLeft()+84, gui.getPreviewTop()+50, 0xffff0000);
            return;
        }

    }

    private void renderHelpers(Tessellator tessellator, VertexBuffer buffer, int xlen, int ylen, int zlen, boolean showAxis, boolean showOuter) {
        // X, Y, Z axis
        if (showAxis) {
            ShapeRenderer.renderAxis(tessellator, buffer, xlen/2, ylen/2, zlen/2);
        }

        if (showOuter) {
            ShapeRenderer.renderOuterBox(tessellator, buffer, xlen, ylen, zlen);
        }
    }



    private static final Quad[] QUADS = new Quad[] {
            new Quad(new Vt(0, 0, 0), new Vt(1, 0, 0), new Vt(1, 0, 1), new Vt(0, 0, 1)),       // DOWN
            new Quad(new Vt(0, 1, 1), new Vt(1, 1, 1), new Vt(1, 1, 0), new Vt(0, 1, 0)),       // UP
            new Quad(new Vt(1, 1, 0), new Vt(1, 0, 0), new Vt(0, 0, 0), new Vt(0, 1, 0)),       // NORTH
            new Quad(new Vt(1, 0, 1), new Vt(1, 1, 1), new Vt(0, 1, 1), new Vt(0, 0, 1)),       // SOUTH
            new Quad(new Vt(0, 0, 1), new Vt(0, 1, 1), new Vt(0, 1, 0), new Vt(0, 0, 0)),       // WEST
            new Quad(new Vt(1, 0, 0), new Vt(1, 1, 0), new Vt(1, 1, 1), new Vt(1, 0, 1)),       // EAST
    };

    static void renderOuterBox(Tessellator tessellator, VertexBuffer buffer, int xlen, int ylen, int zlen) {
        GlStateManager.glLineWidth(1.0f);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
//        buffer.setTranslation(0.5, 0.5, 0.5);
        int xleft = -xlen / 2;
        int xright = xlen / 2 + (xlen & 1);
        int ybot = -ylen / 2;
        int ytop = ylen / 2 + (ylen & 1);
        int zsouth = -zlen / 2;
        int znorth = zlen / 2 + (zlen & 1);

        buffer.pos(xleft, ybot, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ybot, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ybot, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ytop, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ybot, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ybot, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ytop, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ytop, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ytop, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ybot, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ytop, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ytop, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ybot, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ybot, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ybot, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ytop, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ytop, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ytop, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ytop, zsouth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ytop, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ytop, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ybot, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xleft, ybot, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(xright, ybot, znorth).color(1f, 1f, 1f, 1f).endVertex();
        buffer.setTranslation(0, 0, 0);
        tessellator.draw();
    }

    static void renderAxis(Tessellator tessellator, VertexBuffer buffer, int xlen, int ylen, int zlen) {
        GlStateManager.glLineWidth(2.5f);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
//        buffer.setTranslation(0.5, 0.5, 0.5);
        buffer.pos(0, 0, 0).color(1f, 0f, 0f, 1f).endVertex();
        buffer.pos(xlen, 0, 0).color(1f, 0f, 0f, 1f).endVertex();
        buffer.pos(0, 0, 0).color(0f, 1f, 0f, 1f).endVertex();
        buffer.pos(0, ylen, 0).color(0f, 1f, 0f, 1f).endVertex();
        buffer.pos(0, 0, 0).color(0f, 0f, 1f, 1f).endVertex();
        buffer.pos(0, 0, zlen).color(0f, 0f, 1f, 1f).endVertex();
        buffer.setTranslation(0, 0, 0);
        tessellator.draw();
    }

    private long calculateChecksum(ItemStack stack) {
        return ShapeCardItem.getCheck(stack);
    }

    private static class Col {
        private final float r;
        private final float g;
        private final float b;

        public Col(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public float getR() {
            return r;
        }

        public float getG() {
            return g;
        }

        public float getB() {
            return b;
        }
    }

    private static final Col COL_DEFAULT = new Col(.5f,.3f,.5f);
    private static final Col COL_MODDED = new Col(.1f,.8f,.8f);
    private static final Col COL_SNOW = new Col(.8f,.8f,.8f);
    private static final Col COL_DIRT = new Col(0x86/255.0f,0x60/255.0f,0x43/255.0f);
    private static final Col COL_GRASS = new Col(0x20/255.0f,0x90/255.0f,0x20/255.0f);
    private static final Col COL_FOLIAGE = new Col(0x20/255.0f,0x70/255.0f,0x20/255.0f);
    private static final Col COL_STONE = new Col(0x7d/255.0f,0x7d/255.0f,0x7d/255.0f);
    private static final Col COL_WATER = new Col(0x37/255.0f,0x49/255.0f,0xc6/255.0f);
    private static final Col COL_NETHERACK = new Col(0x6f/255.0f,0x36/255.0f,0x35/255.0f);
    private static final Col COL_ENDSTONE = new Col(0xdd/255.0f,0xe0/255.0f,0xa5/255.0f);
    private static final Col COL_SAND = new Col(0xdb/255.0f,0xd3/255.0f,0xa0/255.0f);
    private static final Col COL_GRAVEL = new Col(0x7f/255.0f,0x7c/255.0f,0x7b/255.0f);
    private static final Col COL_BEDROCK = new Col(0x54/255.0f,0x54/255.0f,0x54/255.0f);
    private static final Col COL_LAVA = new Col(0xd4/255.0f,0x5a/255.0f,0x12/255.0f);
    private static final Col COL_WOOD = new Col(0x66/255.0f,0x51/255.0f,0x32/255.0f);
    private static final Col COL_FLOWER = new Col(0xa0/255.0f,0x20/255.0f,0x20/255.0f);
    private static final Col COL_OBSIDIAN = new Col(0x14/255.0f,0x12/255.0f,0x1e/255.0f);
    private static final Col COL_QUARTZ = new Col(0xec/255.0f,0xe9/255.0f,0xe2/255.0f);
    private static final Col COL_SCANNER = new Col(0x00/255.0f,0x00/255.0f,0xe2/255.0f);

    private Col getColor(Map<IBlockState, Col> pallete, IBlockState state) {
        if (state == null) {
            return COL_DEFAULT;
        }
        if (pallete.containsKey(state)) {
            return pallete.get(state);
        }
        Col col;
        Block b = state.getBlock();
        if (b == Blocks.DIRT || b == Blocks.FARMLAND || b == Blocks.GRASS_PATH) {
            col = COL_DIRT;
        } else if (b == Blocks.GRASS) {
            col = COL_GRASS;
        } else if (b == Blocks.GRAVEL) {
            col = COL_GRAVEL;
        } else if (b == Blocks.BEDROCK) {
            col = COL_BEDROCK;
        } else if (b == Blocks.SAND || b == Blocks.SANDSTONE) {
            col = COL_SAND;
        } else if (b == Blocks.QUARTZ_BLOCK) {
            col = COL_QUARTZ;
        } else if (b == Blocks.NETHERRACK) {
            col = COL_NETHERACK;
        } else if (b == Blocks.OBSIDIAN) {
            col = COL_OBSIDIAN;
        } else if (b == Blocks.END_STONE) {
            col = COL_ENDSTONE;
        } else if (b == Blocks.LEAVES || b == Blocks.LEAVES2 || b == Blocks.REEDS || b == Blocks.SAPLING) {
            col = COL_FOLIAGE;
        } else if (b == Blocks.RED_FLOWER || b == Blocks.YELLOW_FLOWER) {
            col = COL_FLOWER;
        } else if (b == Blocks.SNOW || b == Blocks.SNOW_LAYER) {
            col = COL_SNOW;
        } else if (b == Blocks.STONE || b == Blocks.COBBLESTONE || b == Blocks.MOSSY_COBBLESTONE) {
            col = COL_STONE;
        } else if (b == Blocks.WATER || b == Blocks.FLOWING_WATER) {
            col = COL_WATER;
        } else if (b == Blocks.LAVA || b == Blocks.FLOWING_LAVA) {
            col = COL_LAVA;
        } else if (b == Blocks.PLANKS || b == Blocks.LOG || b == Blocks.LOG2) {
            col = COL_WOOD;
        } else if (b == BuilderSetup.scannerBlock) {
            col = COL_SCANNER;
        } else if (!"minecraft".equals(state.getBlock().getRegistryName().getResourceDomain())) {
            col = COL_MODDED;
        } else {
            col = COL_DEFAULT;
        }
        // CLAY, ICE, SOUL_SAND, STONEBRICK, NETHER_BRICK, PRISMARINE, PURPUR_BLOCK
        pallete.put(state, col);
        return col;
    }

    private void renderFaces(Tessellator tessellator, final VertexBuffer buffer,
                     ItemStack stack, boolean showMat) {

        if (ShapeRenderer.positions == null || waitForNewRequest > 0) {
            if (waitForNewRequest <= 0) {
                // No positions, send a new request
                RFToolsMessages.INSTANCE.sendToServer(new PacketRequestShapeData(stack));
                waitForNewRequest = 10;
                ShapeRenderer.positions = null;
            } else {
                waitForNewRequest--;
                if (ShapeRenderer.positions != null) {
                    // Positions have arrived, create displayList
                    // Data is received
                    waitForNewRequest = 0;
                    checksum = calculateChecksum(stack);
                    createDisplayList(tessellator, buffer, showMat);
                }
            }
            if (glList != -1) {
                // Render old data while we're waiting
                GlStateManager.callList(glList);
            }
            return;
        }

        long check = calculateChecksum(stack);
        if (glList == -1 || check != checksum || showMat != prevShowMat) {
            // Checksum failed, set positions to null
            ShapeRenderer.positions = null;
        }

        if (glList != -1) {
            GlStateManager.callList(glList);
        }
    }

    private void createDisplayList(Tessellator tessellator, VertexBuffer buffer, boolean showMat) {
        prevShowMat = showMat;
        invalidateGlList();
        glList = GLAllocation.generateDisplayLists(1);
        GlStateManager.glNewList(glList, GL11.GL_COMPILE);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
//        GlStateManager.enableBlend();
//        GlStateManager.enableAlpha();

//            Map<Long, IBlockState> stateMap = new HashMap<>();
//            TLongHashSet positions = ShapeCardItem.getPositions(stack, shape, false, new BlockPos(0, 0, 0), new BlockPos(0, 0, 0), stateMap);
        Map<IBlockState, Col> pallete = new HashMap<>();

        for (Map.Entry<Long, IBlockState> entry : positions.entrySet()) {
            long p = entry.getKey();
            BlockPos coordinate = BlockPos.fromLong(p);
            int x = coordinate.getX();
            int y = coordinate.getY();
            int z = coordinate.getZ();

            buffer.setTranslation(buffer.xOffset + x, buffer.yOffset + y, buffer.zOffset + z);
            if (showMat) {
                Col col = getColor(pallete, entry.getValue());
                float r = col.getR();
                float g = col.getG();
                float b = col.getB();
                if (!positions.containsKey(coordinate.up().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.UP.ordinal(), r * .8f, g * .8f, b * .8f);
                }
                if (!positions.containsKey(coordinate.down().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.DOWN.ordinal(), r * .8f, g * .8f, b * .8f);
                }
                if (!positions.containsKey(coordinate.north().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.NORTH.ordinal(), r * 1.2f, g * 1.2f, b * 1.2f);
                }
                if (!positions.containsKey(coordinate.south().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.SOUTH.ordinal(), r * 1.2f, g * 1.2f, b * 1.2f);
                }
                if (!positions.containsKey(coordinate.west().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.WEST.ordinal(), r, g, b);
                }
                if (!positions.containsKey(coordinate.east().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.EAST.ordinal(), r, g, b);
                }
            } else {
                float d = .2f;
                float l = ((x + y + z) & 1) == 1 ? .9f : .6f;
                if (!positions.containsKey(coordinate.up().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.UP.ordinal(), d, l, d);
                }
                if (!positions.containsKey(coordinate.down().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.DOWN.ordinal(), d, l, d);
                }
                if (!positions.containsKey(coordinate.north().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.NORTH.ordinal(), d, d, l);
                }
                if (!positions.containsKey(coordinate.south().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.SOUTH.ordinal(), d, d, l);
                }
                if (!positions.containsKey(coordinate.west().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.WEST.ordinal(), l, d, d);
                }
                if (!positions.containsKey(coordinate.east().toLong())) {
                    addSideFullTexture(buffer, EnumFacing.EAST.ordinal(), l, d, d);
                }
            }
            buffer.setTranslation(buffer.xOffset - x, buffer.yOffset - y, buffer.zOffset - z);
        }
        tessellator.draw();
        GlStateManager.glEndList();
    }

    private static void setupScissor(IShapeParentGui gui) {
        Minecraft mc = Minecraft.getMinecraft();

        final ScaledResolution scaledresolution = new ScaledResolution(mc);
        int xScale = scaledresolution.getScaledWidth();
        int yScale = scaledresolution.getScaledHeight();
        int sx = (gui.getPreviewLeft() + 84) * mc.displayWidth / xScale;
        int sy = (mc.displayHeight) - (gui.getPreviewTop() + 136) * mc.displayHeight / yScale;
        int sw = 161 * mc.displayWidth / xScale;
        int sh = 130 * mc.displayHeight / yScale;

        GL11.glScissor(sx, sy, sw, sh);
    }


    public static void addSideFullTexture(VertexBuffer buffer, int side, float r, float g, float b) {
        Quad quad = QUADS[side];
        float a = 0.5f;
        buffer.pos(quad.v1.x, quad.v1.y, quad.v1.z).color(r, g, b, a).endVertex();
        buffer.pos(quad.v2.x, quad.v2.y, quad.v2.z).color(r, g, b, a).endVertex();
        buffer.pos(quad.v3.x, quad.v3.y, quad.v3.z).color(r, g, b, a).endVertex();
        buffer.pos(quad.v4.x, quad.v4.y, quad.v4.z).color(r, g, b, a).endVertex();
    }

    private static class Vt {
        public final float x;
        public final float y;
        public final float z;

        public Vt(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class Quad {
        public final Vt v1;
        public final Vt v2;
        public final Vt v3;
        public final Vt v4;

        public Quad(Vt v1, Vt v2, Vt v3, Vt v4) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
        }
    }
}
