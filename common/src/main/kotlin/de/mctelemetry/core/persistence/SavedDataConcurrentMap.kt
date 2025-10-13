package de.mctelemetry.core.persistence

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.saveddata.SavedData
import java.util.concurrent.ConcurrentMap

abstract class SavedDataConcurrentMap<K : Any, V : Any> protected constructor(
    protected val backingCallbackMap: DirtyCallbackMutableMap.Concurrent<K, V>,
) : ConcurrentMap<K, V> by backingCallbackMap, SavedData() {

    protected abstract fun save(
        compoundTag: CompoundTag,
        provider: HolderLookup.Provider,
        data: Map<K, V>,
    ): CompoundTag

    override fun save(compoundTag: CompoundTag, provider: HolderLookup.Provider): CompoundTag {
        return save(compoundTag, provider, backingCallbackMap.toMap())
    }
}
