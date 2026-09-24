package com.example.phoenx.domain.genealogy

import com.example.phoenx.domain.model.ResolvedPerson
import org.junit.Assert.*
import org.junit.Test

class TreeAlgorithmTest {

    @Test
    fun testSimpleChainGenerations() {
        val grandparent = ResolvedPerson(
            id = "p1",
            firstName = "Grandparent",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = emptyList()
        )
        val parent = ResolvedPerson(
            id = "p2",
            firstName = "Parent",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = listOf("p1")
        )
        val child = ResolvedPerson(
            id = "p3",
            firstName = "Child",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = listOf("p2")
        )

        val layout = TreeAlgorithm.calculateLayout(listOf(grandparent, parent, child))

        assertEquals(3, layout.nodes.size)
        val gNode = layout.nodes.find { it.person.id == "p1" }!!
        val pNode = layout.nodes.find { it.person.id == "p2" }!!
        val cNode = layout.nodes.find { it.person.id == "p3" }!!

        assertEquals(0, gNode.generation)
        assertEquals(1, pNode.generation)
        assertEquals(2, cNode.generation)
    }

    @Test
    fun testCoParentsSameGeneration() {
        val father = ResolvedPerson(
            id = "p1",
            firstName = "Father",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = emptyList()
        )
        val mother = ResolvedPerson(
            id = "p2",
            firstName = "Mother",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = emptyList()
        )
        val child = ResolvedPerson(
            id = "p3",
            firstName = "Child",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = listOf("p1", "p2")
        )

        val layout = TreeAlgorithm.calculateLayout(listOf(father, mother, child))

        val fNode = layout.nodes.find { it.person.id == "p1" }!!
        val mNode = layout.nodes.find { it.person.id == "p2" }!!
        val cNode = layout.nodes.find { it.person.id == "p3" }!!

        assertEquals(fNode.generation, mNode.generation)
        assertEquals(0, fNode.generation)
        assertEquals(1, cNode.generation)
        assertTrue(
            layout.coupleConnections.contains("p1" to "p2") ||
            layout.coupleConnections.contains("p2" to "p1")
        )
    }

    @Test
    fun testKinshipCycleSafetyLimit() {
        val personA = ResolvedPerson(
            id = "p1",
            firstName = "PersonA",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = listOf("p2")
        )
        val personB = ResolvedPerson(
            id = "p2",
            firstName = "PersonB",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = listOf("p1")
        )

        val layout = TreeAlgorithm.calculateLayout(listOf(personA, personB))

        assertNotNull(layout)
        assertEquals(2, layout.nodes.size)
    }

    @Test
    fun testIsolatedPerson() {
        val person = ResolvedPerson(
            id = "p1",
            firstName = "Alone",
            lastName = "Doe",
            photoUrl = null,
            isDeceased = false,
            biography = "",
            parentIds = emptyList()
        )

        val layout = TreeAlgorithm.calculateLayout(listOf(person))

        assertEquals(1, layout.nodes.size)
        assertEquals(0, layout.nodes[0].generation)
    }
}
