package xhman.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigValidatorTest {
    @Test
    fun `wrap rotation clamps to 0-360`() {
        assertEquals(10f, ConfigValidator.wrapRotation(370f))
        assertEquals(350f, ConfigValidator.wrapRotation(-10f))
    }

    @Test
    fun `normalize clamps scale and opacity`() {
        val cfg = Config(scale = 10f, opacity = 0.01f, glowWidth = -1f, outlineWidth = -2f)
        val normalized = ConfigValidator.normalize(cfg)
        assertEquals(ConfigValidator.MAX_SCALE, normalized.scale)
        assertEquals(ConfigValidator.MIN_OPACITY, normalized.opacity)
        assertTrue(normalized.glowWidth >= 0f)
        assertTrue(normalized.outlineWidth >= 0f)
    }
}
