package com.fulfilment.application.monolith.warehouses.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class WareHouseSearchRequestTest {

    @Test
    void constructor_setsLocation_whenProvided() {
        var req = new WareHouseSearchRequest("AMSTERDAM-001", null, null, null, null, null, null);
        assertEquals("AMSTERDAM-001", req.location);
    }

    @Test
    void constructor_setsLocation_toNull_whenNotProvided() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, null);
        assertNull(req.location);
    }

    // -------------------------------------------------------------------------
    // minCapacity / maxCapacity — passed through as-is
    // -------------------------------------------------------------------------

    @Test
    void constructor_setsMinAndMaxCapacity_whenProvided() {
        var req = new WareHouseSearchRequest(null, 100, 500, null, null, null, null);
        assertEquals(100, req.minCapacity);
        assertEquals(500, req.maxCapacity);
    }

    @Test
    void constructor_allowsMinCapacityEqualToMaxCapacity() {
        var req = new WareHouseSearchRequest(null, 200, 200, null, null, null, null);
        assertEquals(200, req.minCapacity);
        assertEquals(200, req.maxCapacity);
    }

    @Test
    void constructor_throwsIllegalArgument_whenMinCapacityExceedsMaxCapacity() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new WareHouseSearchRequest(null, 500, 100, null, null, null, null));
        assertTrue(ex.getMessage().contains("minCapacity (500)"));
        assertTrue(ex.getMessage().contains("maxCapacity (100)"));
    }

    @Test
    void constructor_allowsOnlyMinCapacity_withNoMaxCapacity() {
        assertDoesNotThrow(() -> new WareHouseSearchRequest(null, 100, null, null, null, null, null));
    }

    @Test
    void constructor_allowsOnlyMaxCapacity_withNoMinCapacity() {
        assertDoesNotThrow(() -> new WareHouseSearchRequest(null, null, 500, null, null, null, null));
    }

    // -------------------------------------------------------------------------
    // sortBy — defaults to "createdAt", only "capacity" is overridden
    // -------------------------------------------------------------------------

    @Test
    void constructor_setsSortBy_toCapacity_whenCapacityProvided() {
        var req = new WareHouseSearchRequest(null, null, null, "capacity", null, null, null);
        assertEquals("capacity", req.sortBy);
    }

    @Test
    void constructor_setsSortBy_toCreatedAt_whenNullProvided() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, null);
        assertEquals("createdAt", req.sortBy);
    }

    @ParameterizedTest
    @ValueSource(strings = {"createdAt", "invalid", "CAPACITY", "date", ""})
    void constructor_setsSortBy_toCreatedAt_forAnyNonCapacityValue(String sortBy) {
        var req = new WareHouseSearchRequest(null, null, null, sortBy, null, null, null);
        assertEquals("createdAt", req.sortBy);
    }

    // -------------------------------------------------------------------------
    // sortOrder — defaults to "asc", only "desc" (case-insensitive) overrides
    // -------------------------------------------------------------------------

    @Test
    void constructor_setsSortOrder_toDesc_whenDescProvided() {
        var req = new WareHouseSearchRequest(null, null, null, null, "desc", null, null);
        assertEquals("desc", req.sortOrder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DESC", "Desc", "dEsC"})
    void constructor_setsSortOrder_toDesc_caseInsensitive(String sortOrder) {
        var req = new WareHouseSearchRequest(null, null, null, null, sortOrder, null, null);
        assertEquals("desc", req.sortOrder);
    }

    @Test
    void constructor_setsSortOrder_toAsc_whenNullProvided() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, null);
        assertEquals("asc", req.sortOrder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "ASC", "invalid", ""})
    void constructor_setsSortOrder_toAsc_forAnyNonDescValue(String sortOrder) {
        var req = new WareHouseSearchRequest(null, null, null, null, sortOrder, null, null);
        assertEquals("asc", req.sortOrder);
    }

    // -------------------------------------------------------------------------
    // page — defaults to 0, must be >= 0
    // -------------------------------------------------------------------------

    @Test
    void constructor_setsPage_toProvidedValue() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, 3, null);
        assertEquals(3, req.page);
    }

    @Test
    void constructor_setsPage_toZero_whenNullProvided() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, null);
        assertEquals(0, req.page);
    }

    @Test
    void constructor_allowsPageZero() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, 0, null);
        assertEquals(0, req.page);
    }

    @Test
    void constructor_throwsIllegalArgument_whenPageIsNegative() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new WareHouseSearchRequest(null, null, null, null, null, -1, null));
        assertTrue(ex.getMessage().contains("page must be >= 0"));
    }

    // -------------------------------------------------------------------------
    // pageSize — defaults to 10, must be > 0, capped at 100
    // -------------------------------------------------------------------------

    @Test
    void constructor_setsPageSize_toProvidedValue() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, 25);
        assertEquals(25, req.pageSize);
    }

    @Test
    void constructor_setsPageSize_toDefault10_whenNullProvided() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, null);
        assertEquals(10, req.pageSize);
    }

    @Test
    void constructor_capsPageSize_at100_whenExceeded() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, 200);
        assertEquals(100, req.pageSize);
    }

    @Test
    void constructor_allowsPageSizeExactly100() {
        var req = new WareHouseSearchRequest(null, null, null, null, null, null, 100);
        assertEquals(100, req.pageSize);
    }

    @Test
    void constructor_throwsIllegalArgument_whenPageSizeIsZero() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new WareHouseSearchRequest(null, null, null, null, null, null, 0));
        assertTrue(ex.getMessage().contains("pageSize must be > 0"));
    }

    @Test
    void constructor_throwsIllegalArgument_whenPageSizeIsNegative() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new WareHouseSearchRequest(null, null, null, null, null, null, -5));
        assertTrue(ex.getMessage().contains("pageSize must be > 0"));
    }

    // -------------------------------------------------------------------------
    // combined — all params provided
    // -------------------------------------------------------------------------

    @Test
    void constructor_setsAllFields_whenAllProvided() {
        var req = new WareHouseSearchRequest("LONDON-001", 50, 300, "capacity", "desc", 2, 20);

        assertEquals("LONDON-001", req.location);
        assertEquals(50,           req.minCapacity);
        assertEquals(300,          req.maxCapacity);
        assertEquals("capacity",   req.sortBy);
        assertEquals("desc",       req.sortOrder);
        assertEquals(2,            req.page);
        assertEquals(20,           req.pageSize);
    }

}