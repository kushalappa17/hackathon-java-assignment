package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@QuarkusTest
public class ProductResourceTest {

    @Inject
    ProductCleaner productCleaner;

    @BeforeEach
    void cleanDatabase() {
        productCleaner.deleteAllProducts();
    }

    @ApplicationScoped
    public static class ProductCleaner {
        @Inject
        ProductRepository productRepository;

        @Transactional
        public void deleteAllProducts() {
            productRepository.deleteAll();
        }
    }

    @Test
    void testGetAllProducts_returnsOkWithList() {
        createProductViaApi("Widget", "A widget", new BigDecimal("9.99"), 100);

        given()
                .when().get("/product")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(List.class));
    }

    @Test
    void testGetAllProducts_returnsEmptyList_whenNoProductsExist() {
        given()
                .when().get("/product")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    void testGetAllProducts_returnsSortedByName() {
        createProductViaApi("Zebra Toy", "Z product", new BigDecimal("5.00"), 10);
        createProductViaApi("Apple Toy", "A product", new BigDecimal("3.00"), 20);

        given()
                .when().get("/product")
                .then()
                .statusCode(200)
                .body("[0].name", is("Apple Toy"))
                .body("[1].name", is("Zebra Toy"));
    }

    // -------------------------------------------------------------------------
    // GET /product/{id} — get single product
    // -------------------------------------------------------------------------

    @Test
    void testGetSingleProduct_returnsProduct_whenFound() {
        Product created = createProductViaApi("Single Product", "desc", new BigDecimal("19.99"), 5);

        given()
                .when().get("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("id",   is(created.id.intValue()))
                .body("name", is("Single Product"));
    }

    @Test
    void testGetSingleProduct_returns404_whenNotFound() {
        given()
                .when().get("/product/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testCreateProduct_returns201_withPersistedProduct() {
        given()
                .contentType(ContentType.JSON)
                .body(buildProduct("New Product", "A new one", new BigDecimal("12.50"), 30))
                .when().post("/product")
                .then()
                .statusCode(201)
                .body("name",        is("New Product"))
                .body("description", is("A new one"))
                .body("stock",       is(30))
                .body("id",          notNullValue());

        // Extract price as String and compare via BigDecimal to avoid Float scale issues
        String price = given()
                .when().get("/product")
                .then()
                .statusCode(200)
                .extract().path("[0].price").toString();

        assertEquals(0, new BigDecimal("12.50").compareTo(new BigDecimal(price)));
    }

    @Test
    void testCreateProduct_returns422_whenIdIsProvided() {
        Product p = buildProduct("Bad Product", "desc", new BigDecimal("1.00"), 1);
        p.id = 99L;

        given()
                .contentType(ContentType.JSON)
                .body(p)
                .when().post("/product")
                .then()
                .statusCode(422)
                .body("error", containsString("Id was invalidly set on request."));
    }

    @Test
    void testCreateProduct_withZeroStock_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body(buildProduct("Out of Stock Item", "none left", new BigDecimal("5.00"), 0))
                .when().post("/product")
                .then()
                .statusCode(201)
                .body("stock", is(0));
    }

    @Test
    void testCreateProduct_withNullDescription_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body(buildProduct("No Desc", null, new BigDecimal("8.00"), 10))
                .when().post("/product")
                .then()
                .statusCode(201)
                .body("name", is("No Desc"));
    }

    @Test
    void testUpdateProduct_returns200_withUpdatedFields() {
        Product created = createProductViaApi("Old Name", "Old desc", new BigDecimal("1.00"), 1);

        given()
                .contentType(ContentType.JSON)
                .body(buildProduct("New Name", "New desc", new BigDecimal("99.99"), 50))
                .when().put("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("name",        is("New Name"))
                .body("description", is("New desc"))
                .body("stock",       is(50));

        String price = given()
                .when().get("/product/" + created.id)
                .then().extract().path("price").toString();

        assertEquals(0, new BigDecimal("99.99").compareTo(new BigDecimal(price)));
    }

    @Test
    void testUpdateProduct_returns404_whenNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(buildProduct("Ghost", "desc", new BigDecimal("1.00"), 1))
                .when().put("/product/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testUpdateProduct_returns422_whenNameIsNull() {
        Product created = createProductViaApi("Has Name", "desc", new BigDecimal("5.00"), 5);

        given()
                .contentType(ContentType.JSON)
                .body(buildProduct(null, "desc", new BigDecimal("5.00"), 5))
                .when().put("/product/" + created.id)
                .then()
                .statusCode(422)
                .body("error", containsString("Product Name was not set on request."));
    }

    @Test
    void testUpdateProduct_updatesAllFields() {
        Product created = createProductViaApi("Original", "Original desc", new BigDecimal("1.00"), 1);

        given()
                .contentType(ContentType.JSON)
                .body(buildProduct("Updated", "Updated desc", new BigDecimal("55.55"), 99))
                .when().put("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("name",        is("Updated"))
                .body("description", is("Updated desc"))
                .body("stock",       is(99));

        String price = given()
                .when().get("/product/" + created.id)
                .then().extract().path("price").toString();

        assertEquals(0, new BigDecimal("55.55").compareTo(new BigDecimal(price)));
    }

    @Test
    void testPatchProduct_updatesOnlyName() {
        Product created = createProductViaApi("Original", "Original desc", new BigDecimal("10.00"), 5);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Patched Name\"}")
                .when().patch("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("name",        is("Patched Name"))
                .body("description", is("Original desc"))
                .body("stock",       is(5));

        String price = given()
                .when().get("/product/" + created.id)
                .then().extract().path("price").toString();
        assertEquals(0, new BigDecimal("10.00").compareTo(new BigDecimal(price)));
    }

    @Test
    void testPatchProduct_updatesOnlyDescription() {
        Product created = createProductViaApi("Patch Desc", "Old desc", new BigDecimal("10.00"), 5);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Patch Desc\", \"description\": \"New desc\"}")
                .when().patch("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("description", is("New desc"))
                .body("name",        is("Patch Desc"))
                .body("stock",       is(5));
    }

    @Test
    void testPatchProduct_updatesOnlyPrice() {
        Product created = createProductViaApi("Patch Price", "desc", new BigDecimal("10.00"), 5);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Patch Price\", \"price\": 99.99}")
                .when().patch("/product/" + created.id)
                .then()
                .statusCode(200);

        String price = given()
                .when().get("/product/" + created.id)
                .then().extract().path("price").toString();
        assertEquals(0, new BigDecimal("99.99").compareTo(new BigDecimal(price)));
    }

    @Test
    void testPatchProduct_updatesOnlyStock() {
        Product created = createProductViaApi("Patch Stock", "desc", new BigDecimal("10.00"), 5);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Patch Stock\", \"stock\": 99}")
                .when().patch("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("stock", is(99));
    }

    @Test
    void testPatchProduct_returns404_whenNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Ghost\"}")
                .when().patch("/product/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testPatchProduct_withEmptyBody_returnsUnchangedProduct() {
        // PATCH with no fields is a valid no-op — all fields are optional
        Product created = createProductViaApi("Patch No-op", "desc", new BigDecimal("5.00"), 5);

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().patch("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("name",        is("Patch No-op"))
                .body("description", is("desc"))
                .body("stock",       is(5));
    }

    @Test
    void testPatchProduct_doesNotChangeUnprovidedFields() {
        Product created = createProductViaApi("Unchanged", "Keep this", new BigDecimal("7.77"), 3);

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Unchanged\"}")
                .when().patch("/product/" + created.id)
                .then()
                .statusCode(200)
                .body("description", is("Keep this"))
                .body("stock",       is(3));

        String price = given()
                .when().get("/product/" + created.id)
                .then().extract().path("price").toString();
        assertEquals(0, new BigDecimal("7.77").compareTo(new BigDecimal(price)));
    }

    @Test
    void testDeleteProduct_returns204_whenDeleted() {
        Product created = createProductViaApi("To Delete", "desc", new BigDecimal("1.00"), 1);

        given()
                .when().delete("/product/" + created.id)
                .then()
                .statusCode(204);
    }

    @Test
    void testDeleteProduct_returns404_whenNotFound() {
        given()
                .when().delete("/product/999999")
                .then()
                .statusCode(404)
                .body("error", containsString("999999"));
    }

    @Test
    void testDeleteProduct_returns404_afterAlreadyDeleted() {
        Product created = createProductViaApi("Delete Twice", "desc", new BigDecimal("1.00"), 1);

        given()
                .when().delete("/product/" + created.id)
                .then().statusCode(204);

        given()
                .when().delete("/product/" + created.id)
                .then()
                .statusCode(404)
                .body("error", containsString(String.valueOf(created.id)));
    }

    @Test
    void testDeleteProduct_isNoLongerRetrievableAfterDeletion() {
        Product created = createProductViaApi("Gone Soon", "desc", new BigDecimal("2.00"), 2);

        given().when().delete("/product/" + created.id).then().statusCode(204);

        given()
                .when().get("/product/" + created.id)
                .then()
                .statusCode(404);
    }

    @Test
    void testErrorMapper_returnsStructuredJson() {
        given()
                .when().get("/product/999999")
                .then()
                .statusCode(404)
                .body("exceptionType", notNullValue())
                .body("code",          is(404))
                .body("error",         notNullValue());
    }

    private Product createProductViaApi(String name, String description, BigDecimal price, int stock) {
        return given()
                .contentType(ContentType.JSON)
                .body(buildProduct(name, description, price, stock))
                .when().post("/product")
                .then()
                .statusCode(201)
                .extract().as(Product.class);
    }

    private Product buildProduct(String name, String description, BigDecimal price, int stock) {
        Product p = new Product();
        p.name        = name;
        p.description = description;
        p.price       = price;
        p.stock       = stock;
        return p;
    }
}