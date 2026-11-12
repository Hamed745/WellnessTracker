package com.example.welnesstracker.network

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PlacesServiceTest {
    @Test fun categoryQueriesIncludeExpectedFilters() {
        val gyms = PlacesService.buildQuery(56.0, 92.0, PlaceCategory.GYMS)
        val parks = PlacesService.buildQuery(56.0, 92.0, PlaceCategory.PARKS)
        val tracks = PlacesService.buildQuery(56.0, 92.0, PlaceCategory.TRACKS)
        assertTrue(gyms.contains("fitness_centre"))
        assertFalse(gyms.contains("stadium"))
        assertTrue(parks.contains("[\"leisure\"=\"park\"]"))
        assertTrue(tracks.contains("athletics"))
        assertTrue(tracks.contains("stadium"))
        assertTrue(parks.contains("(around:8000,56.0,92.0)"))
        assertTrue(parks.contains("out center 150;"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidCoordinates() {
        PlacesService.buildQuery(200.0, 92.0, PlaceCategory.GYMS)
    }
}
