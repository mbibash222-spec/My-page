package com.example

import com.example.ui.auth.AgentAuthConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AgentAuthConfigTest {

    @Test
    fun `valid demo agent credentials pass validation`() {
        assertNull(AgentAuthConfig.validateAgentCredentials("AGT-104", AgentAuthConfig.DEMO_AGENT_PIN))
        assertNull(AgentAuthConfig.validateAgentCredentials("  agt-208 ", "2026 "))
    }

    @Test
    fun `short or blank badge id fails validation`() {
        val blankError = AgentAuthConfig.validateAgentCredentials("  ", AgentAuthConfig.DEMO_AGENT_PIN)
        val shortError = AgentAuthConfig.validateAgentCredentials("AG", AgentAuthConfig.DEMO_AGENT_PIN)

        assertNotNull(blankError)
        assertNotNull(shortError)
        assertEquals(blankError, shortError)
    }

    @Test
    fun `invalid pin fails validation`() {
        val wrongPinError = AgentAuthConfig.validateAgentCredentials("AGT-104", "0000")
        val emptyPinError = AgentAuthConfig.validateAgentCredentials("AGT-104", "")

        assertNotNull(wrongPinError)
        assertNotNull(emptyPinError)
        assertEquals(wrongPinError, emptyPinError)
    }
}
