package de.mctelemetry.core.ui.components

import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.mctelemetry.core.api.observations.IParameterizedObservationSource
import de.mctelemetry.core.commands.types.parse
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.core.Sizing
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.chat.ThrowingComponent
import net.minecraft.util.CommonColors

@Environment(EnvType.CLIENT)
class ParameterValidationTextBoxComponent<T>(private val parameter: IParameterizedObservationSource.Parameter<T>, horizontalSizing: Sizing) : TextBoxComponent(horizontalSizing) {

    init {
        textValue.observe {
            if (isValid(it)) {
                setTextColor(DEFAULT_TEXT_COLOR)
            } else {
                setTextColor(CommonColors.RED)
            }
        }
    }

    private fun isValid(value: String): Boolean {
        if (parameter.optional && value.isEmpty()) {
            return true
        }

        try {
            val parsed = parameter.argumentType.parse(value)
            parameter.validate(parsed)
            return true
        } catch (_: CommandSyntaxException) {
        } catch (_: IllegalArgumentException) {
        } catch (_: ThrowingComponent) {
        }

        return false;
    }
}
