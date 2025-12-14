package de.mctelemetry.core.commands.types

import net.minecraft.util.StringRepresentable

enum class InstrumentExportType(private val _serializedName: String) : StringRepresentable {
    LONG("long"),
    DOUBLE("double"),
    ;

    override fun getSerializedName(): String {
        return _serializedName
    }
}
