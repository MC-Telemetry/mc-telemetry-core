package de.mctelemetry.core.api.attributes

import de.mctelemetry.core.api.OTelCoreModAPI
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.UUIDUtil
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import java.util.UUID

object BuiltinAttributeKeyTypes {

    val ALL: List<IAttributeKeyTypeInstance.InstanceType<*, *>> = listOf(
        BlockPosType,
        GlobalPosType,
        ItemType,
        DirectionType,
        UUIDType,
    )

    object BlockPosType : IAttributeKeyTypeInstance.InstanceType<BlockPos, List<Long>> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "block_position")
        )

        override val baseType: GenericAttributeType<List<Long>> = GenericAttributeType.LONG_ARRAY
        override val valueStreamCodec: StreamCodec<ByteBuf, BlockPos> = BlockPos.STREAM_CODEC
        override val valueType: Class<BlockPos> = BlockPos::class.java

        override fun format(value: BlockPos): List<Long> {
            return listOf(value.x.toLong(), value.y.toLong(), value.z.toLong())
        }

        fun format(x: Long, y: Long, z: Long): List<Long> {
            return listOf(x, y, z)
        }

        override fun canConvertDirectlyTo(supertype: IAttributeKeyTypeTemplate<*, *>): Boolean {
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> true
                else -> false
            }
        }

        override fun <R : Any> convertDirectlyTo(supertype: IAttributeKeyTypeTemplate<R, *>, value: BlockPos): R? {
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

        override fun fromNbt(tag: Tag, lookupProvider: HolderLookup.Provider): BlockPos {
            val data = (tag as IntArrayTag).asIntArray
            return BlockPos(data[0], data[1], data[2])
        }

        override fun toNbt(value: BlockPos): IntArrayTag {
            return IntArrayTag(intArrayOf(value.x, value.y, value.z))
        }
    }

    object GlobalPosType : IAttributeKeyTypeInstance.InstanceType<GlobalPos, List<String>> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "global_position")
        )

        override val baseType: GenericAttributeType<List<String>> = GenericAttributeType.STRING_ARRAY
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

        override fun canConvertDirectlyTo(supertype: IAttributeKeyTypeTemplate<*, *>): Boolean {
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> true
                BlockPosType -> true
                else -> false
            }
        }

        override fun <R : Any> convertDirectlyTo(supertype: IAttributeKeyTypeTemplate<R, *>, value: GlobalPos): R? {
            @Suppress("UNCHECKED_CAST") // known values from when-matching
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> "${value.dimension.location()}@${value.pos.x},${value.pos.y},${value.pos.z}" as R
                BlockPosType -> value.pos as R
                else -> null
            }
        }

        override fun fromNbt(tag: Tag, lookupProvider: HolderLookup.Provider): GlobalPos {
            tag as CompoundTag
            val dimension = tag.getString("dimension")
            val posArray = tag.getIntArray("position")
            return GlobalPos(
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)),
                BlockPos(posArray[0], posArray[1], posArray[2])
            )
        }

        override fun toNbt(value: GlobalPos): Tag {
            return CompoundTag().apply {
                putString("dimension", value.dimension.location().toString())
                val pos = value.pos
                putIntArray("position", intArrayOf(pos.x, pos.y, pos.z))
            }
        }
    }

    object DirectionType : IAttributeKeyTypeInstance.InstanceType<Direction, String> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "direction")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueStreamCodec: StreamCodec<ByteBuf, Direction> = Direction.STREAM_CODEC
        override val valueType: Class<Direction> = Direction::class.java

        override fun format(value: Direction): String {
            return value.toString()
        }

        override fun fromNbt(tag: Tag, lookupProvider: HolderLookup.Provider): Direction {
            return Direction.from3DDataValue((tag as ByteTag).asByte.toInt())
        }

        override fun toNbt(value: Direction): Tag {
            return ByteTag.valueOf(value.get3DDataValue().toByte())
        }
    }

    object ItemType : IAttributeKeyTypeInstance.InstanceType<Item, String> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "item")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueStreamCodec: StreamCodec<RegistryFriendlyByteBuf, Item> =
            ByteBufCodecs.registry(Registries.ITEM)
        override val valueType: Class<Item> = Item::class.java

        override fun format(value: Item): String {
            val key = value.`arch$holder`().unwrapKey().orElseThrow()
            return key.location().toString()
        }

        override fun fromNbt(tag: Tag, lookupProvider: HolderLookup.Provider): Item {
            val resourceLocation = (tag as StringTag).asString

            return lookupProvider.lookupOrThrow(Registries.ITEM).getOrThrow(
                ResourceKey.create(Registries.ITEM, ResourceLocation.parse(resourceLocation))
            ).value()
        }

        override fun toNbt(value: Item): Tag {
            return StringTag.valueOf(
                value
                    .`arch$holder`()
                    .unwrapKey()
                    .orElseThrow()
                    .location()
                    .toString()
            )
        }
    }

    object UUIDType : IAttributeKeyTypeInstance.InstanceType<UUID, String> {

        override val id: ResourceKey<IAttributeKeyTypeTemplate<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "uuid")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueStreamCodec: StreamCodec<ByteBuf, UUID> = UUIDUtil.STREAM_CODEC
        override val valueType: Class<UUID> = UUID::class.java


        override fun format(value: UUID): String {
            return value.toString()
        }

        override fun fromNbt(tag: Tag, lookupProvider: HolderLookup.Provider): UUID {
            val data = (tag as IntArrayTag).asIntArray
            return UUID(
                (data[0].toLong() shl 32) or (data[1].toLong()),
                (data[2].toLong() shl 32) or (data[3].toLong()),
            )
        }

        override fun toNbt(value: UUID): Tag {
            return IntArrayTag(
                intArrayOf(
                    (value.mostSignificantBits shr 32).toInt(),
                    value.mostSignificantBits.toInt(),
                    (value.leastSignificantBits shr 32).toInt(),
                    value.leastSignificantBits.toInt(),
                )
            )
        }
    }
}
