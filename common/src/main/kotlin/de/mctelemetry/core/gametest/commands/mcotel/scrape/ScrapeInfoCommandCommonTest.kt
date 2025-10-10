package de.mctelemetry.core.gametest.commands.mcotel.scrape

import de.mctelemetry.core.utils.assertCommandCannotParse
import de.mctelemetry.core.utils.assertThrows
import de.mctelemetry.core.utils.runCommand
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

object ScrapeInfoCommandCommonTest {

    private val permissionLevel = 2

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun permission0Fails(helper: GameTestHelper) {
        helper.assertCommandCannotParse(
            "mcotel scrape info",
            permissionLevel = 0,
        )
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun permission0WithMetricFails(helper: GameTestHelper) {
        helper.assertCommandCannotParse(
            "mcotel scrape info jvm.class.loaded",
            permissionLevel = 0,
        )
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun permission1Fails(helper: GameTestHelper) {
        helper.assertCommandCannotParse(
            "mcotel scrape info",
            permissionLevel = 1,
        )
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun permission1WithMetricFails(helper: GameTestHelper) {
        helper.assertCommandCannotParse(
            "mcotel scrape info jvm.class.loaded",
            permissionLevel = 1,
        )
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun normalSucceeds(helper: GameTestHelper) {
        helper.runCommand(
            "mcotel scrape info",
            permissionLevel = permissionLevel,
            requiredSuccess = true
        ).let { commandResult ->
            helper.assertValueEqual(commandResult.results.size, 1, "results.size")
            val (success, result) = commandResult.results.single()
            helper.assertTrue(
                success,
                "Expected success be true"
            )
            helper.assertTrue(
                commandResult.messages.isNotEmpty(),
                "Expected at least one message"
            )
            helper.assertTrue(
                result > 10,
                "Expected a result of at least 10 but was $result"
            )
        }
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun filterForMetricReturns1(helper: GameTestHelper) {
        helper.runCommand(
            "mcotel scrape info jvm.class.loaded",
            permissionLevel = permissionLevel,
            requiredSuccess = true
        ).let { commandResult ->
            helper.assertValueEqual(commandResult.results.size, 1, "results.size")
            val (_, result) = commandResult.results.single()
            helper.assertValueEqual(
                commandResult.messages.size,
                1,
                "messages.size"
            )
            val messageString = commandResult.messages.single().string
            val matches = Regex("""\s{2}-\s([\w._]+)(?:\s?\([^)]+\))?: """).findAll(messageString)
                .map { it.groupValues[1] }
                .toList()
            helper.assertValueEqual(matches, listOf("jvm.class.loaded"), "messages[0]")
            helper.assertValueEqual(
                result,
                1,
                "result"
            )
        }
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun filterForMissingMetricFails(helper: GameTestHelper) {
        helper.assertThrows<NoSuchElementException> {
            helper.runCommand(
                "mcotel scrape info missing.metric",
                permissionLevel = permissionLevel,
                requiredSuccess = false
            )
        }
        helper.succeed()
    }
}
