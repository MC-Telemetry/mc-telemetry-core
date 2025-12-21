package de.mctelemetry.core.ui.components

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.StringType.convertValueToString
import de.mctelemetry.core.api.attributes.canConvertFrom
import de.mctelemetry.core.api.attributes.convertFrom
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import io.github.pixix4.kobserve.base.ObservableProperty
import io.github.pixix4.kobserve.base.ObservableValue
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.gui.components.EditBox
import net.minecraft.util.CommonColors
import kotlin.collections.plus

class AttributeMappingComponent(
    val sourceAttributes: IAttributeDateSourceReferenceSet,
    instrumentAttributesObservable: ObservableValue<Map<String, MappedAttributeKeyInfo<*, *>>?>,
    mappingProperty: ObservableProperty<ObservationAttributeMapping>,
) : FlowLayout(Sizing.content(), Sizing.content(), Algorithm.VERTICAL) {

    val instrumentAttributes by instrumentAttributesObservable
    var mapping by mappingProperty

    init {
        rebuild()

        instrumentAttributesObservable.onChange {
            rebuild()
        }
    }

    sealed class AttributeMappingSources {
        abstract val type: IAttributeKeyTypeTemplate<*,*>?
        object None : AttributeMappingSources() {
            override val type: IAttributeKeyTypeTemplate<*,*>? = null
        }
        class Reference(val reference: AttributeDataSource.ObservationSourceAttributeReference<*>) :
            AttributeMappingSources() {
            override val type: IAttributeKeyTypeTemplate<*, *> = reference.type
        }

        object Custom : AttributeMappingSources() {
            override val type: IAttributeKeyTypeTemplate<*, *> = NativeAttributeKeyTypes.StringType
        }
    }

    private fun makeCustomConstantValue(
        text: String,
        targetType: IAttributeKeyTypeTemplate<*, *>,
        input: TextBoxComponent? = null,
    ): AttributeDataSource.ConstantAttributeData<*> {
        val stringConstantData = AttributeDataSource.ConstantAttributeData(
            NativeAttributeKeyTypes.StringType,
            text,
        )
        val convertedConstantData = targetType.convertFrom(
            stringConstantData
        )
        return if (convertedConstantData == null) {
            OTelCoreMod.logger.info(
                "Failed to convert \"{}\" to {}",
                text,
                targetType
            )
            input?.setTextColor(CommonColors.RED)
            stringConstantData
        } else {
            input?.setTextColor(EditBox.DEFAULT_TEXT_COLOR)
            convertedConstantData
        }
    }

    private fun rebuild() {
        val observationSourceAttributes = sourceAttributes.references
        val instrumentAttributes = instrumentAttributes?.values?.toList() ?: emptyList()

        val noneOption = SelectBoxComponentEntry<AttributeMappingSources>(
            AttributeMappingSources.None,
            "None"
        )
        val customOption = SelectBoxComponentEntry<AttributeMappingSources>(AttributeMappingSources.Custom, "Custom")
        val options = listOf(
            noneOption
        ) + observationSourceAttributes.map { reference ->
            SelectBoxComponentEntry<AttributeMappingSources>(
                AttributeMappingSources.Reference(reference),
                reference.info.baseKey.key
            )
        } + customOption

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

            val customInput = Components.textBox(Sizing.expand())
            customInput.setEditable(false)

            val selectedOption: SelectBoxComponentEntry<AttributeMappingSources> = when (selected) {
                null -> noneOption
                is AttributeDataSource.ObservationSourceAttributeReference<*> -> options.firstOrNull {
                    (it.value as? AttributeMappingSources.Reference)?.reference == selected
                } ?: noneOption

                is AttributeDataSource.ConstantAttributeData<*> -> {
                    customInput.text(selected.convertValueToString())
                    customInput.setEditable(true)
                    customOption
                }
            }

            val attributeMapping =
                SelectBoxComponent(
                    nameComponent,
                    options.filter {
                        val optionType = it.value.type ?: return@filter true
                        instrumentationSourceAttribute.templateType.canConvertFrom(optionType)
                    },
                    selectedOption,
                ) { old, new ->
                    println(new)

                    customInput.setEditable(new.value == AttributeMappingSources.Custom)
                    val newValue: AttributeDataSource<*> = when (new.value) {
                        AttributeMappingSources.Custom -> {
                            makeCustomConstantValue(
                                customInput.value,
                                instrumentationSourceAttribute.templateType,
                                customInput,
                            )
                        }

                        is AttributeMappingSources.Reference -> new.value.reference
                        AttributeMappingSources.None -> {
                            if (old.value != AttributeMappingSources.None) {
                                mapping -= instrumentationSourceAttribute
                            }
                            return@SelectBoxComponent
                        }
                    }

                    mapping += instrumentationSourceAttribute to newValue
                }
            valueCol.child(attributeMapping)

            valueCol.child(customInput as Component)

            // Initial coloring
            makeCustomConstantValue(
                customInput.value,
                instrumentationSourceAttribute.templateType,
                customInput,
            )

            // order is relevant! Do not place above selectedOption construction (StackOverflow between onChanged and setText?)!
            customInput.onChanged().subscribe {
                mapping += instrumentationSourceAttribute to makeCustomConstantValue(
                    it,
                    instrumentationSourceAttribute.templateType,
                    customInput,
                )
            }

            row.child(valueCol)

            child(row)
        }
    }
}
