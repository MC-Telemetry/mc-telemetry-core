package de.mctelemetry.core.api.attributes

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.persistence.RegistryIdFieldCodec
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.UUIDUtil
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluid
import java.util.UUID

object BuiltinAttributeKeyTypes {

    val ALL: List<IAttributeKeyTypeInstance.InstanceType<*, *, *>> = listOf(
        BlockPosType,
        GlobalPosType,
        ItemType,
        FluidType,
        DirectionType,
        UUIDType,
    )

    object BlockPosType : IAttributeKeyTypeInstance.InstanceType<BlockPos, List<Long>, BlockPosType> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "block_position")
        )

        override val baseType: GenericAttributeType<List<Long>> = GenericAttributeType.LONG_ARRAY
        override val valueCodec: Codec<BlockPos> = BlockPos.CODEC
        override val valueStreamCodec: StreamCodec<ByteBuf, BlockPos> = BlockPos.STREAM_CODEC
        override val valueType: Class<BlockPos> = BlockPos::class.java

        override fun format(value: BlockPos): List<Long> {
            return listOf(value.x.toLong(), value.y.toLong(), value.z.toLong())
        }

        fun format(x: Long, y: Long, z: Long): List<Long> {
            return listOf(x, y, z)
        }

        override fun canConvertDirectlyTo(supertype: IAttributeKeyTypeTemplate<*, *, *>): Boolean {
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> true
                else -> false
            }
        }

        override fun <R : Any> convertDirectlyTo(supertype: IAttributeKeyTypeTemplate<R, *, *>, value: BlockPos): R? {
            @Suppress("UNCHECKED_CAST") // known values from when-matching
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> "${value.x},${value.y},${value.z}" as R
                NativeAttributeKeyTypes.StringArrayType -> listOf(
                    value.x.toString(),
                    value.y.toString(),
                    value.z.toString()
                ) as R

                else -> null
            }
        }
    }

    object GlobalPosType : IAttributeKeyTypeInstance.InstanceType<GlobalPos, List<String>, GlobalPosType> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "global_position")
        )

        override val baseType: GenericAttributeType<List<String>> = GenericAttributeType.STRING_ARRAY
        override val valueCodec: Codec<GlobalPos> = GlobalPos.CODEC
        override val valueStreamCodec: StreamCodec<ByteBuf, GlobalPos> = GlobalPos.STREAM_CODEC
        override val valueType: Class<GlobalPos> = GlobalPos::class.java

        override fun format(value: GlobalPos): List<String> {
            val pos = value.pos
            return listOf(value.dimension.location().toString(), pos.x.toString(), pos.y.toString(), pos.z.toString())
        }

        fun format(dimension: ResourceKey<Level>, pos: BlockPos): List<String> {
            return listOf(dimension.location().toString(), pos.x.toString(), pos.y.toString(), pos.z.toString())
        }

        fun format(dimension: ResourceKey<Level>, x: Long, y: Long, z: Long): List<String> {
            return listOf(dimension.location().toString(), x.toString(), y.toString(), z.toString())
        }

        override fun canConvertDirectlyTo(supertype: IAttributeKeyTypeTemplate<*, *, *>): Boolean {
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> true
                BlockPosType -> true
                else -> false
            }
        }

        override fun <R : Any> convertDirectlyTo(supertype: IAttributeKeyTypeTemplate<R, *, *>, value: GlobalPos): R? {
            @Suppress("UNCHECKED_CAST") // known values from when-matching
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> "${value.dimension.location()}@${value.pos.x},${value.pos.y},${value.pos.z}" as R
                BlockPosType -> value.pos as R
                else -> null
            }
        }
    }

    object DirectionType : IAttributeKeyTypeInstance.InstanceType<Direction, String, DirectionType> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "direction")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueCodec: Codec<Direction> = Direction.CODEC
        override val valueStreamCodec: StreamCodec<ByteBuf, Direction> = Direction.STREAM_CODEC
        override val valueType: Class<Direction> = Direction::class.java

        override fun format(value: Direction): String {
            return value.toString()
        }
    }

    object ItemType : IAttributeKeyTypeInstance.InstanceType<Item, String, ItemType> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "item")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueCodec: Codec<Item> =
            RegistryIdFieldCodec(Registries.ITEM) { it.`arch$holder`().unwrapKey().orElseThrow().location() }
        override val valueStreamCodec: StreamCodec<RegistryFriendlyByteBuf, Item> =
            ByteBufCodecs.registry(Registries.ITEM)
        override val valueType: Class<Item> = Item::class.java

        override fun format(value: Item): String {
            val key = value.`arch$holder`().unwrapKey().orElseThrow()
            return key.location().toString()
        }
    }

    object FluidType : IAttributeKeyTypeInstance.InstanceType<Fluid, String, FluidType> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "fluid")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueCodec: Codec<Fluid> =
            RegistryIdFieldCodec(Registries.FLUID) { it.`arch$holder`().unwrapKey().orElseThrow().location() }
        override val valueStreamCodec: StreamCodec<RegistryFriendlyByteBuf, Fluid> =
            ByteBufCodecs.registry(Registries.FLUID)
        override val valueType: Class<Fluid> = Fluid::class.java

        override fun format(value: Fluid): String {
            val key = value.`arch$holder`().unwrapKey().orElseThrow()
            return key.location().toString()
        }
    }

    object UUIDType : IAttributeKeyTypeInstance.InstanceType<UUID, String, UUIDType> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "uuid")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueCodec: Codec<UUID> = UUIDUtil.CODEC
        override val valueStreamCodec: StreamCodec<ByteBuf, UUID> = UUIDUtil.STREAM_CODEC
        override val valueType: Class<UUID> = UUID::class.java


        override fun format(value: UUID): String {
            return value.toString()
        }
    }
}
