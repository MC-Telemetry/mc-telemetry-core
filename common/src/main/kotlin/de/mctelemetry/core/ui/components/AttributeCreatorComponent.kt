package de.mctelemetry.core.ui.components

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IMappedAttributeKeyType
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import io.github.pixix4.kobserve.base.ObservableMutableList
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.Minecraft

class AttributeCreatorEntry(var name: String, var type: IMappedAttributeKeyType<*, *>)

class AttributeCreatorComponent(
    val attributes: ObservableMutableList<AttributeCreatorEntry>,
    val types: List<IMappedAttributeKeyType<*, *>> = Minecraft.getInstance().level!!.registryAccess().registryOrThrow(
        OTelCoreModAPI.AttributeTypeMappings
    ).toList()
) : FlowLayout(Sizing.content(), Sizing.content(), Algorithm.VERTICAL) {

    init {
        rebuild()

        attributes.onChange {
            rebuild()
        }
    }

    private fun rebuild() {
        val options = types.map { t ->
            SelectBoxComponentEntry(
                t,
                t.id.location().path
            )
        }

        clearChildren()

        for (attributeEntry in attributes) {
            val row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
            row.verticalAlignment(VerticalAlignment.CENTER)
            row.padding(Insets.of(4))

            val valueCol = Containers.verticalFlow(Sizing.fill(80), Sizing.content())
            val nameInput = Components.textBox(Sizing.expand(), attributeEntry.name)
            nameInput.onChanged().subscribe {
                attributeEntry.name = it
            }
            valueCol.child(nameInput as Component)

            val attributeType =
                SelectBoxComponent(
                    buildComponent("Types"),
                    options,
                    options.firstOrNull { it.value == attributeEntry.type } ?: options.first()
                ) { _, new ->
                    attributeEntry.type = new.value ?: return@SelectBoxComponent
                }
            valueCol.child(attributeType)
            row.child(valueCol)

            val deleteButton = Components.button(buildComponent("Delete")) {
                attributes -= attributeEntry
            } as Component
            deleteButton.horizontalSizing(Sizing.fill(16))
            deleteButton.margins(Insets.left(4))
            row.child(deleteButton)

            child(row)
        }

        val createButton = Components.button(buildComponent("Add attribute")) {
            attributes += AttributeCreatorEntry("", types.first())
        } as Component
        createButton.margins(Insets.of(4))
        child(createButton)
    }
}
