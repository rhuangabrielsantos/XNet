package mcjty.xnet.blocks.generic;

import mcjty.lib.compat.CompatBlock;
import mcjty.lib.compat.theoneprobe.TOPInfoProvider;
import mcjty.lib.compat.waila.WailaInfoProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import mcjty.xnet.XNet;
import mcjty.xnet.api.keys.ConsumerId;
import mcjty.xnet.api.keys.NetworkId;
import mcjty.xnet.blocks.cables.ConnectorType;
import mcjty.xnet.blocks.facade.FacadeBlockId;
import mcjty.xnet.blocks.facade.FacadeProperty;
import mcjty.xnet.blocks.facade.IFacadeSupport;
import mcjty.xnet.multiblock.BlobId;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public abstract class GenericCableBlock extends CompatBlock implements WailaInfoProvider, TOPInfoProvider {

    // Properties that indicate if there is the same block in a certain direction.
    public static final UnlistedPropertyBlockType NORTH = new UnlistedPropertyBlockType("north");
    public static final UnlistedPropertyBlockType SOUTH = new UnlistedPropertyBlockType("south");
    public static final UnlistedPropertyBlockType WEST = new UnlistedPropertyBlockType("west");
    public static final UnlistedPropertyBlockType EAST = new UnlistedPropertyBlockType("east");
    public static final UnlistedPropertyBlockType UP = new UnlistedPropertyBlockType("up");
    public static final UnlistedPropertyBlockType DOWN = new UnlistedPropertyBlockType("down");

    public static final FacadeProperty FACADEID = new FacadeProperty("facadeid");


    public static final ColorId STANDARD_COLOR = new ColorId(1);

    public static final AxisAlignedBB EMPTY = new AxisAlignedBB(0, 0, 0, 0, 0, 0);


    public GenericCableBlock(Material material, String name) {
        super(material);
        setHardness(1.0f);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        setUnlocalizedName(XNet.MODID + "." + name);
        setRegistryName(name);
        GameRegistry.register(this);
        GameRegistry.register(createItemBlock(), getRegistryName());
        setCreativeTab(XNet.tabXNet);
    }

    protected ItemBlock createItemBlock() {
        return new ItemBlock(this);
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    @Nullable
    protected IBlockState getMimicBlock(IBlockAccess blockAccess, BlockPos pos) {
        TileEntity te = blockAccess.getTileEntity(pos);
        if (te instanceof IFacadeSupport) {
            return ((IFacadeSupport) te).getMimicBlock();
        } else {
            return null;
        }
    }


    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        return EMPTY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);

        if (mode == ProbeMode.EXTENDED) {
            BlobId blobId = worldBlob.getBlobAt(data.getPos());
            if (blobId != null) {
                probeInfo.text(TextStyleClass.LABEL + "Blob: " + TextStyleClass.INFO + blobId.getId());
            }
        }

        Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
        if (networks != null && !networks.isEmpty()) {
            for (NetworkId network : networks) {
                probeInfo.text(TextStyleClass.LABEL + "Network: " + TextStyleClass.INFO + network.getId());
            }
        }

        ConsumerId consumerId = worldBlob.getConsumerAt(data.getPos());
        if (consumerId != null) {
            probeInfo.text(TextStyleClass.LABEL + "Consumer: " + TextStyleClass.INFO + consumerId.getId());
        }
    }

    public String getConnectorTexture() {
        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        originalOnBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            createCableSegment(world, pos, stack);
        }
    }

    protected void originalOnBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
    }

    public void createCableSegment(World world, BlockPos pos, ItemStack stack) {
        XNetBlobData blobData = XNetBlobData.getBlobData(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        worldBlob.createCableSegment(pos, STANDARD_COLOR);
        blobData.save(world);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            XNetBlobData blobData = XNetBlobData.getBlobData(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            worldBlob.removeCableSegment(pos);
            blobData.save(world);
        }

        originalBreakBlock(world, pos, state);
    }

    protected void originalBreakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public boolean isBlockNormalCube(IBlockState blockState) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState blockState) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        IProperty[] listedProperties = new IProperty[0]; // no listed properties
        IUnlistedProperty[] unlistedProperties = new IUnlistedProperty[] { NORTH, SOUTH, WEST, EAST, UP, DOWN,
            FACADEID};
        return new ExtendedBlockState(this, listedProperties, unlistedProperties);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;

        ConnectorType north = getConnectorType(world, pos.north());
        ConnectorType south = getConnectorType(world, pos.south());
        ConnectorType west = getConnectorType(world, pos.west());
        ConnectorType east = getConnectorType(world, pos.east());
        ConnectorType up = getConnectorType(world, pos.up());
        ConnectorType down = getConnectorType(world, pos.down());

        return extendedBlockState
                .withProperty(NORTH, north)
                .withProperty(SOUTH, south)
                .withProperty(WEST, west)
                .withProperty(EAST, east)
                .withProperty(UP, up)
                .withProperty(DOWN, down);
    }

    protected abstract ConnectorType getConnectorType(IBlockAccess world, BlockPos pos);
}
