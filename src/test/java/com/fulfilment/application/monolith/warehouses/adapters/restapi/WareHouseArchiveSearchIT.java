package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@Disabled("Destructive test — archives MWH.023 which cannot be restored via API. Run in isolation: ./mvnw verify -Dit.test=WarehouseArchiveSearchIT")
@QuarkusIntegrationTest
@DisabledIfSystemProperty(named = "ci.skip.integration", matches = "true")
public class WareHouseArchiveSearchIT {

    @Test
    public void testSearch_doesNotReturn_archivedWarehouse() {
        // Verify MWH.023 / TILBURG-001 is present before archiving
        given()
                .when().get("warehouse/search")
                .then()
                .statusCode(200)
                .body(containsString("TILBURG-001"));

        // Archive MWH.023
        given()
                .when().delete("warehouse/MWH.023")
                .then().statusCode(204);

        // TILBURG-001 must no longer appear in search results
        given()
                .when().get("warehouse/search")
                .then()
                .statusCode(200)
                .body("$.size()",             is(2))
                .body(not(containsString("TILBURG-001")))
                .body(containsString("ZWOLLE-001"))
                .body(containsString("AMSTERDAM-001"));
    }
}
