package de.mctelemetry.core.ui.components

import de.mctelemetry.core.api.attributes.IMappedAttributeKeySet
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import io.github.pixix4.kobserve.base.ObservableProperty
import io.github.pixix4.kobserve.base.ObservableValue
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import kotlin.collections.plus

class AttributeMappingComponent(
    val sourceAttributes: IMappedAttributeKeySet,
    instrumentAttributesObservable: ObservableValue<Map<String, MappedAttributeKeyInfo<*, *>>?>,
    mappingProperty: ObservableProperty<ObservationAttributeMapping>
) : FlowLayout(Sizing.content(), Sizing.content(), Algorithm.VERTICAL) {

    val instrumentAttributes by instrumentAttributesObservable
    var mapping by mappingProperty

    init {
        rebuild()

        instrumentAttributesObservable.onChange {
            rebuild()
        }

        mappingProperty.onChange {
            rebuild()
        }
    }

    private fun rebuild() {
        val observationSourceAttributes = sourceAttributes.attributeKeys
        val instrumentAttributes = instrumentAttributes?.values?.toList() ?: emptyList()

        val options = listOf(
            SelectBoxComponentEntry<MappedAttributeKeyInfo<*, *>>(
                null,
                "None"
            )
        ) + observationSourceAttributes.map { attribute ->
            SelectBoxComponentEntry(
                attribute,
                attribute.baseKey.key
            )
        } + SelectBoxComponentEntry<MappedAttributeKeyInfo<*, *>>(null, "Custom")

        clearChildren()

        for (instrumentationSourceAttribute in instrumentAttributes) {
            val row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
            row.verticalAlignment(VerticalAlignment.CENTER)
            row.padding(Insets.of(4))

            val nameComponent = buildComponent { +instrumentationSourceAttribute.baseKey.key }
            val attributeName = Components.label(nameComponent)
            attributeName.horizontalSizing(Sizing.fill(40))
            row.child(attributeName)

            val selected = mapping.mapping[instrumentationSourceAttribute]

            val valueCol = Containers.verticalFlow(Sizing.fill(60), Sizing.content())

            val attributeMapping =
                SelectBoxComponent(
                    nameComponent,
                    options,
                    options.firstOrNull { it.value == selected } ?: options.first()) { old, new ->
                    if (new.value != null) {
                        mapping = ObservationAttributeMapping(
                            mapping.mapping + (instrumentationSourceAttribute to new.value)
                        )
                    } else if (old.value != null) {
                        mapping = ObservationAttributeMapping(
                            mapping.mapping - instrumentationSourceAttribute
                        )
                    }
                    println(new)
                }
            valueCol.child(attributeMapping)

            val customInput = Components.textBox(Sizing.expand())
            valueCol.child(customInput as Component)

            row.child(valueCol)

            child(row)
        }
    }

}
