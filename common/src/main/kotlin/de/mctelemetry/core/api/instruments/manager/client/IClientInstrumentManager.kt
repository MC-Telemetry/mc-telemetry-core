package de.mctelemetry.core.api.instruments.manager.client

import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
interface IClientInstrumentManager : IInstrumentManager {
    val reservedNames: Set<String>

    override fun nameAvailable(name: String): Boolean {
        return name.lowercase() !in reservedNames && super.nameAvailable(name)
    }

    interface IClientInstrumentDefinition : IInstrumentDefinition
}
