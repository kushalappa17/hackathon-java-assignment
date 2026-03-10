package com.fulfilment.application.monolith.stores;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class StoreTransactionIntegrationTest {

  @InjectMock
  LegacyStoreManagerGateway legacyGateway;

  @Test
  public void testLegacySystemNotNotifiedOnFailedStoreCreation() {
    Mockito.reset(legacyGateway);

    String uniqueName = "IntegrationTest_" + System.currentTimeMillis();

    // First create should succeed
    given()
            .contentType("application/json")
            .body("{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 5}")
            .when().post("/store")
            .then()
            .statusCode(201);

    // Legacy system should be notified for the successful creation
    verify(legacyGateway, times(1)).createStoreOnLegacySystem(any(Store.class));

    // Reset for next assertion
    Mockito.reset(legacyGateway);

    // Second create with same name should fail with 422 — duplicate name
    // is caught explicitly before hitting the DB constraint
    given()
            .contentType("application/json")
            .body("{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 10}")
            .when().post("/store")
            .then()
            .statusCode(422);

    // Legacy system should NOT be notified for the failed creation
    verify(legacyGateway, never()).createStoreOnLegacySystem(any(Store.class));
  }
}