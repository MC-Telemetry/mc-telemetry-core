package de.mctelemetry.core.ui.components

import de.mctelemetry.core.OTelCoreMod
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.parsing.UIParsing
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW

@Environment(EnvType.CLIENT)
class SuggestingTextBoxComponent(horizontalSizing: Sizing) : TextBoxComponent(horizontalSizing) {


    companion object {

        fun register() {
            UIParsing.registerFactory(
                ResourceLocation.fromNamespaceAndPath(
                    OTelCoreMod.MOD_ID,
                    "suggesting-text-box"
                )
            ) {
                @Suppress("CAST_NEVER_SUCCEEDS") // owo lib has broken type hierarchy, cast actually works
                SuggestingTextBoxComponent(Sizing.content()) as Component
            }
        }

        fun isTakeSuggestion(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            return keyCode == GLFW.GLFW_KEY_ENTER
        }

        private fun getSelectionIndexDelta(keyCode: Int, scanCode: Int, modifiers: Int): Int {
            return when (keyCode) {
                GLFW.GLFW_KEY_DOWN -> 1
                GLFW.GLFW_KEY_UP -> -1
                GLFW.GLFW_KEY_PAGE_DOWN -> {
                    if (modifiers and GLFW.GLFW_MOD_SHIFT != GLFW.GLFW_MOD_SHIFT)
                        10
                    else
                        Int.MAX_VALUE
                }
                GLFW.GLFW_KEY_PAGE_UP -> {
                    if (modifiers and GLFW.GLFW_MOD_SHIFT != GLFW.GLFW_MOD_SHIFT)
                        -10
                    else
                        Int.MIN_VALUE
                }
                else -> 0
            }
        }
    }

    private val suggestions: MutableList<String> = mutableListOf()
    private var suggestionCached: Boolean = false

    var replacementSuggestion: String? = null
        set(value) {
            if (field == value && suggestionCached) return
            field = value
            suggestionCached = true
            if (value == null) {
                super.setSuggestion(null)
                _suggestionIndex = -1
                return
            }
            val currentText = this.value
            val suggestionIndex = suggestions.indexOf(value)
            val suggestionIndexText: String
            if (suggestionIndex >= 0) {
                suggestionIndexText = if (suggestions.size > 1) " [${suggestionIndex + 1}/${suggestions.size}]" else ""
                _suggestionIndex = suggestionIndex
            } else {
                suggestionIndexText = ""
                suggestions.clear()
                suggestions.add(value)
                _suggestionIndex = 0
            }
            if (value.startsWith(currentText)) {
                super.setSuggestion(value.substring(currentText.length) + suggestionIndexText)
            } else {
                super.setSuggestion("    =$value$suggestionIndexText")
            }
        }
    private var _suggestionIndex = -1
    var suggestionIndex: Int
        get() = _suggestionIndex
        set(value) {
            suggestionCached = false
            if (value < 0) {
                _suggestionIndex = -1
                replacementSuggestion = null
            } else if (value > suggestions.lastIndex) {
                throw IndexOutOfBoundsException(value)
            } else {
                _suggestionIndex = value
                replacementSuggestion = suggestions[value]
            }
        }

    private fun updateReplacementText() {
        suggestionCached = false
        replacementSuggestion = replacementSuggestion
    }

    override fun setValue(string: String) {
        val previousSuggestion = replacementSuggestion
        val previousValue = this.value
        super.setValue(string)
        val newValue = this.value
        if (previousValue != newValue) {
            suggestionCached = false
            this.changedEvents.sink().onChanged(newValue)
        }
        val index = suggestions.indexOf(previousSuggestion)
        if (index == -1 || suggestionIndex == index || previousSuggestion != replacementSuggestion) {
            updateReplacementText()
            return
        }
        suggestionIndex = index
    }

    override fun setSuggestion(string: String?) {
        suggestionCached = false
        if (string == null) {
            this.replacementSuggestion = null
        } else {
            this.replacementSuggestion = value + string
        }
    }

    fun updateSuggestions(suggestions: Collection<String>) {
        val oldSuggestion = replacementSuggestion
        this.suggestions.clear()
        this.suggestions.addAll(suggestions)
        suggestionCached = false
        suggestionIndex = suggestions.indexOf(oldSuggestion) // also clears suggestions if not found in suggestions
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        if (isActive && isFocused && suggestions.isNotEmpty()) {
            val delta: Int = when {
                g > 0 -> 1
                g < 0 -> -1
                else -> return super.mouseScrolled(d, e, f, g)
            }
            if (modifySelectedIndex(delta))
                return true
        }
        return super.mouseScrolled(d, e, f, g)
    }

    private fun modifySelectedIndex(delta: Int): Boolean {
        val size = suggestions.size
        if (size == 0) return false
        val targetIndex = Math.clamp(suggestionIndex + delta.toLong(), 0, size - 1)
        if (targetIndex != suggestionIndex) {
            suggestionIndex = targetIndex
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (isActive && isFocused) {
            val suggestion = replacementSuggestion
            if (isTakeSuggestion(keyCode, scanCode, modifiers) && suggestion != null) {
                value = suggestion
                return true
            }
            if (suggestions.isNotEmpty()) {
                val indexDelta = getSelectionIndexDelta(keyCode, scanCode, modifiers)
                if (modifySelectedIndex(indexDelta)) {
                    return true
                }
            }
            if(keyCode == GLFW.GLFW_KEY_TAB) {
                return false // prevent tab-to-spaces hook, instead allow selecting next element with tab
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}
