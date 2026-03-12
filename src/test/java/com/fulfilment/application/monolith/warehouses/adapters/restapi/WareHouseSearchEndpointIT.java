package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for GET /warehouse/search.
 *
 * Seed data from import.sql:
 *   MWH.001 | ZWOLLE-001     | capacity 100 | stock 10 | createdAt 2024-07-01
 *   MWH.012 | AMSTERDAM-001  | capacity  50 | stock  5 | createdAt 2023-07-01
 *   MWH.023 | TILBURG-001    | capacity  30 | stock 27 | createdAt 2021-02-01
 *
 * Run with: ./mvnw verify
 * Skip with: ./mvnw verify -Dci.skip.integration=true
 */
@QuarkusIntegrationTest
@DisabledIfSystemProperty(named = "ci.skip.integration", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WareHouseSearchEndpointIT {

    private static final String PATH = "warehouse/search";

    // -------------------------------------------------------------------------
    // No filters — all 3 seed warehouses returned
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_withNoFilters_returnsAllThreeWarehouses() {
        given()
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",                    is(3))
                .body(containsString("MWH.001"))
                .body(containsString("MWH.012"))
                .body(containsString("MWH.023"));
    }

    @Test
    @Order(1)
    public void testSearch_withNoFilters_returnsAllKnownLocations() {
        given()
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body(containsString("ZWOLLE-001"))
                .body(containsString("AMSTERDAM-001"))
                .body(containsString("TILBURG-001"));
    }

    // -------------------------------------------------------------------------
    // Filter by location
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_filterByLocation_returnsOnlyAmsterdam() {
        given()
                .queryParam("location", "AMSTERDAM-001")
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",             is(1))
                .body("[0].businessUnitCode", is("MWH.012"))
                .body("[0].location",         is("AMSTERDAM-001"))
                .body(not(containsString("ZWOLLE-001")))
                .body(not(containsString("TILBURG-001")));
    }

    @Test
    @Order(1)
    public void testSearch_filterByLocation_returnsEmptyList_whenNoMatch() {
        given()
                .queryParam("location", "NOWHERE-999")
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    // -------------------------------------------------------------------------
    // Filter by minCapacity
    // Seed: TILBURG-001=30, AMSTERDAM-001=50, ZWOLLE-001=100
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_filterByMinCapacity50_returnsTwoWarehouses() {
        // capacity >= 50 → AMSTERDAM-001 (50) and ZWOLLE-001 (100)
        given()
                .queryParam("minCapacity", 50)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",             is(2))
                .body(containsString("AMSTERDAM-001"))
                .body(containsString("ZWOLLE-001"))
                .body(not(containsString("TILBURG-001")));
    }

    @Test
    @Order(1)
    public void testSearch_filterByMinCapacity_boundaryIncludesExactMatch() {
        // capacity >= 30 → all three
        given()
                .queryParam("minCapacity", 30)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(3));
    }

    @Test
    @Order(1)
    public void testSearch_filterByMinCapacity_returnsEmptyList_whenAboveAll() {
        // capacity >= 101 → none
        given()
                .queryParam("minCapacity", 101)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    // -------------------------------------------------------------------------
    // Filter by maxCapacity
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_filterByMaxCapacity50_returnsTwoWarehouses() {
        // capacity <= 50 → TILBURG-001 (30) and AMSTERDAM-001 (50)
        given()
                .queryParam("maxCapacity", 50)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",             is(2))
                .body(containsString("TILBURG-001"))
                .body(containsString("AMSTERDAM-001"))
                .body(not(containsString("ZWOLLE-001")));
    }

    @Test
    @Order(1)
    public void testSearch_filterByMaxCapacity_boundaryIncludesExactMatch() {
        // capacity <= 100 → all three
        given()
                .queryParam("maxCapacity", 100)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(3));
    }

    @Test
    @Order(1)
    public void testSearch_filterByMaxCapacity_returnsEmptyList_whenBelowAll() {
        // capacity <= 29 → none
        given()
                .queryParam("maxCapacity", 29)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    // -------------------------------------------------------------------------
    // Filter by capacity range (min + max combined)
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_filterByCapacityRange_returnsOnlyAmsterdam() {
        // 40 <= capacity <= 60 → only AMSTERDAM-001 (50)
        given()
                .queryParam("minCapacity", 40)
                .queryParam("maxCapacity", 60)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",             is(1))
                .body("[0].businessUnitCode", is("MWH.012"));
    }

    @Test
    @Order(1)
    public void testSearch_filterByCapacityRange_exactBoundariesMatchAll() {
        // 30 <= capacity <= 100 → all three
        given()
                .queryParam("minCapacity", 30)
                .queryParam("maxCapacity", 100)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(3));
    }

    // -------------------------------------------------------------------------
    // Sorting by capacity
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_sortByCapacityAsc_returnsInAscendingOrder() {
        // Expected: TILBURG-001(30), AMSTERDAM-001(50), ZWOLLE-001(100)
        given()
                .queryParam("sortBy",    "capacity")
                .queryParam("sortOrder", "asc")
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",      is(3))
                .body("[0].capacity",  is(30))
                .body("[0].location",  is("TILBURG-001"))
                .body("[1].capacity",  is(50))
                .body("[1].location",  is("AMSTERDAM-001"))
                .body("[2].capacity",  is(100))
                .body("[2].location",  is("ZWOLLE-001"));
    }

    @Test
    @Order(1)
    public void testSearch_sortByCapacityDesc_returnsInDescendingOrder() {
        // Expected: ZWOLLE-001(100), AMSTERDAM-001(50), TILBURG-001(30)
        given()
                .queryParam("sortBy",    "capacity")
                .queryParam("sortOrder", "desc")
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",      is(3))
                .body("[0].capacity",  is(100))
                .body("[0].location",  is("ZWOLLE-001"))
                .body("[1].capacity",  is(50))
                .body("[1].location",  is("AMSTERDAM-001"))
                .body("[2].capacity",  is(30))
                .body("[2].location",  is("TILBURG-001"));
    }

    // -------------------------------------------------------------------------
    // Sorting by createdAt
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_sortByCreatedAtAsc_returnsOldestFirst() {
        // Expected: TILBURG-001(2021), AMSTERDAM-001(2023), ZWOLLE-001(2024)
        given()
                .queryParam("sortBy",    "createdAt")
                .queryParam("sortOrder", "asc")
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",     is(3))
                .body("[0].location", is("TILBURG-001"))
                .body("[1].location", is("AMSTERDAM-001"))
                .body("[2].location", is("ZWOLLE-001"));
    }

    @Test
    @Order(1)
    public void testSearch_sortByCreatedAtDesc_returnsNewestFirst() {
        // Expected: ZWOLLE-001(2024), AMSTERDAM-001(2023), TILBURG-001(2021)
        given()
                .queryParam("sortBy",    "createdAt")
                .queryParam("sortOrder", "desc")
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()",     is(3))
                .body("[0].location", is("ZWOLLE-001"))
                .body("[1].location", is("AMSTERDAM-001"))
                .body("[2].location", is("TILBURG-001"));
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_firstPage_withPageSizeTwo_returnsTwoResults() {
        given()
                .queryParam("page",     0)
                .queryParam("pageSize", 2)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(2));
    }

    @Test
    @Order(1)
    public void testSearch_pageZero_withPageSizeTwo_returnsFirstTwoResults() {
        // page=0 (no offset), pageSize=2 → take first 2 of 3 seed records
        given()
                .queryParam("page",     0)
                .queryParam("pageSize", 2)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(2));
    }

    @Test
    @Order(1)
    public void testSearch_pageAsOffset_skipsAllRows_whenOffsetEqualsTotal() {
        // page=3, pageSize=10 → skip 3 rows → 0 remaining from 3 seed records
        given()
                .queryParam("page",     3)
                .queryParam("pageSize", 10)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    @Order(1)
    public void testSearch_beyondLastPage_returnsEmptyList() {
        given()
                .queryParam("page",     99)
                .queryParam("pageSize", 10)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    @Order(1)
    public void testSearch_pageSizeExceeding100_isCappedAt100() {
        // pageSize 200 is capped to 100 by WareHouseSearchRequest — still returns all 3
        given()
                .queryParam("pageSize", 200)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body("$.size()", is(3));
    }

    // -------------------------------------------------------------------------
    // Invalid parameters — 400
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    public void testSearch_returns400_whenMinCapacityExceedsMaxCapacity() {
        given()
                .queryParam("minCapacity", 500)
                .queryParam("maxCapacity", 100)
                .when().get(PATH)
                .then()
                .statusCode(400)
                .body(containsString("minCapacity (500)"))
                .body(containsString("maxCapacity (100)"));
    }

    @Test
    @Order(1)
    public void testSearch_returns400_whenPageIsNegative() {
        given()
                .queryParam("page", -1)
                .when().get(PATH)
                .then()
                .statusCode(400)
                .body(containsString("page must be >= 0"));
    }

    @Test
    @Order(1)
    public void testSearch_returns400_whenPageSizeIsZero() {
        given()
                .queryParam("pageSize", 0)
                .when().get(PATH)
                .then()
                .statusCode(400)
                .body(containsString("pageSize must be > 0"));
    }
}