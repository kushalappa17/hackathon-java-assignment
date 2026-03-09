package com.fulfilment.application.monolith.stores;


import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class StoreResourceTest {

    @InjectMock
    LegacyStoreManagerGateway legacyStoreManagerGateway;

    @Inject
    StoreCleaner storeCleaner;

    @BeforeEach
    void cleanDatabase() {
        storeCleaner.deleteAllStores();
    }

    @ApplicationScoped
    public static class StoreCleaner {
        @Transactional
        public void deleteAllStores() {
            Store.deleteAll();
        }
    }

    @Test
    void testGetAllStores_returnsOkWithList() {
        createStoreViaApi("Some Store", 10);

        given()
                .when().get("/store")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(List.class));
    }

    @Test
    void testGetAllStores_returnsEmptyList_whenNoStoresExist() {
        given()
                .when().get("/store")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    void testGetAllStores_returnsSortedByName() {
        createStoreViaApi("Zebra Store", 5);
        createStoreViaApi("Apple Store", 10);

        given()
                .when().get("/store")
                .then()
                .statusCode(200)
                .body("[0].name", is("Apple Store"))
                .body("[1].name", is("Zebra Store"));
    }

    @Test
    void testGetSingleStore_returnsStore_whenFound() {
        Store created = createStoreViaApi("Test Store", 10);

        given()
                .when().get("/store/" + created.id)
                .then()
                .statusCode(200)
                .body("id",   is(created.id.intValue()))
                .body("name", is("Test Store"));
    }

    @Test
    void testGetSingleStore_returns404_whenNotFound() {
        given()
                .when().get("/store/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testCreateStore_returns201_withPersistedStore() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"New Store\", \"quantityProductsInStock\": 5}")
                .when().post("/store")
                .then()
                .statusCode(201)
                .body("name",                    is("New Store"))
                .body("quantityProductsInStock", is(5))
                .body("id",                      notNullValue());
    }

    @Test
    void testCreateStore_returns422_whenIdIsProvided() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"id\": 1, \"name\": \"Should Fail\", \"quantityProductsInStock\": 5}")
                .when().post("/store")
                .then()
                .statusCode(422)
                .body("error", containsString("Id was invalidly set on request."));
    }

    @Test
    void testCreateStore_returns422_whenNameIsNull() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"quantityProductsInStock\": 5}")
                .when().post("/store")
                .then()
                .statusCode(422)
                .body("error", containsString("Store Name was not set on request."));
    }

    @Test
    void testCreateStore_returns422_whenNameIsDuplicate() {
        createStoreViaApi("Duplicate Store", 5);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Duplicate Store\", \"quantityProductsInStock\": 10}")
                .when().post("/store")
                .then()
                .statusCode(422)
                .body("error", containsString("Duplicate Store"))
                .body("error", containsString("already exists"));
    }

    @Test
    void testCreateStore_firesStoreCreatedEvent() {
        // 201 confirms the event path was reached
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Event Store\", \"quantityProductsInStock\": 1}")
                .when().post("/store")
                .then()
                .statusCode(201);
    }

    @Test
    void testUpdateStore_returns200_withUpdatedName() {
        Store created = createStoreViaApi("Original Name", 10);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Updated Name\", \"quantityProductsInStock\": 99}")
                .when().put("/store/" + created.id)
                .then()
                .statusCode(200)
                .body("name",                    is("Updated Name"))
                .body("quantityProductsInStock", is(99));
    }

    @Test
    void testUpdateStore_doesNotUpdateStock_whenQuantityIsZero() {
        Store created = createStoreViaApi("Stock Test", 10);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Stock Test Updated\", \"quantityProductsInStock\": 0}")
                .when().put("/store/" + created.id)
                .then()
                .statusCode(200)
                .body("name",                    is("Stock Test Updated"))
                .body("quantityProductsInStock", is(10)); // unchanged because 0 was passed
    }

    @Test
    void testUpdateStore_returns422_whenNameIsNull() {
        Store created = createStoreViaApi("Has Name", 5);

        given()
                .contentType(ContentType.JSON)
                .body("{\"quantityProductsInStock\": 5}")
                .when().put("/store/" + created.id)
                .then()
                .statusCode(422)
                .body("error", containsString("Store Name was not set on request."));
    }

    @Test
    void testUpdateStore_returns404_whenStoreNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Ghost Store\", \"quantityProductsInStock\": 1}")
                .when().put("/store/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testPatchStore_updatesName_whenNameIsProvided() {
        // entity.name is not null → name gets updated to updatedStore.name
        Store created = createStoreViaApi("Original Name", 10);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Patched Name\"}")
                .when().patch("/store/" + created.id)
                .then()
                .statusCode(200)
                .body("name", is("Patched Name"));
    }

    @Test
    void testPatchStore_updatesStock_whenQuantityIsNonZero() {
        Store created = createStoreViaApi("Patch Stock", 10);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Patch Stock\", \"quantityProductsInStock\": 42}")
                .when().patch("/store/" + created.id)
                .then()
                .statusCode(200)
                .body("quantityProductsInStock", is(42));
    }

    @Test
    void testPatchStore_doesNotUpdateStock_whenQuantityIsZero() {
        Store created = createStoreViaApi("Patch Zero Stock", 10);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Patch Zero Stock\", \"quantityProductsInStock\": 0}")
                .when().patch("/store/" + created.id)
                .then()
                .statusCode(200)
                .body("quantityProductsInStock", is(10)); // unchanged
    }

    @Test
    void testPatchStore_returns404_whenStoreNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Ghost Patch\"}")
                .when().patch("/store/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testPatchStore_withEmptyBody_returnsUnchangedStore() {
        // All fields optional in PATCH — empty body is a valid no-op
        Store created = createStoreViaApi("No Change", 5);

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().patch("/store/" + created.id)
                .then()
                .statusCode(200)
                .body("name",                    is("No Change"))
                .body("quantityProductsInStock", is(5));
    }

    @Test
    void testDeleteStore_returns204_whenDeleted() {
        Store created = createStoreViaApi("To Delete", 3);

        given()
                .when().delete("/store/" + created.id)
                .then()
                .statusCode(204);
    }

    @Test
    void testDeleteStore_returns404_whenNotFound() {
        given()
                .when().delete("/store/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testDeleteStore_returns404_afterAlreadyDeleted() {
        Store created = createStoreViaApi("Delete Twice", 3);

        given()
                .when().delete("/store/" + created.id)
                .then().statusCode(204);

        given()
                .when().delete("/store/" + created.id)
                .then()
                .statusCode(404)
                .body("error", containsString(String.valueOf(created.id)));
    }

    @Test
    void testDeleteStore_isNoLongerRetrievableAfterDeletion() {
        Store created = createStoreViaApi("Gone Soon", 3);

        given().when().delete("/store/" + created.id).then().statusCode(204);

        given()
                .when().get("/store/" + created.id)
                .then()
                .statusCode(404);
    }

    private Store createStoreViaApi(String name, int qty) {
        Store s = new Store();
        s.name = name;
        s.quantityProductsInStock = qty;

        return given()
                .contentType(ContentType.JSON)
                .body(s)
                .when().post("/store")
                .then()
                .statusCode(201)
                .extract().as(Store.class);
    }
}