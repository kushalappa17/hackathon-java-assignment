package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@QuarkusTest
public class WarehouseResourceImplTest {

    @InjectMock
    WarehouseRepository warehouseRepository;

    @InjectMock
    CreateWarehouseOperation createWarehouseOperation;

    @InjectMock
    ArchiveWarehouseOperation archiveWarehouseOperation;

    @InjectMock
    ReplaceWarehouseOperation replaceWarehouseOperation;

    @Test
    void listAll_returns200_withEmptyList_whenNoWarehousesExist() {
        when(warehouseRepository.getAll()).thenReturn(List.of());

        given()
                .when().get("/warehouse")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    void listAll_returns200_withMappedWarehouses() {
        var w1 = domainWarehouse("WH-001", "London",  100, 50);
        var w2 = domainWarehouse("WH-002", "Paris",   200, 80);
        when(warehouseRepository.getAll()).thenReturn(List.of(w1, w2));

        given()
                .when().get("/warehouse")
                .then()
                .statusCode(200)
                .body("$.size()",               is(2))
                .body("[0].businessUnitCode",   is("WH-001"))
                .body("[0].location",           is("London"))
                .body("[0].capacity",           is(100))
                .body("[0].stock",              is(50))
                .body("[1].businessUnitCode",   is("WH-002"))
                .body("[1].location",           is("Paris"))
                .body("[1].capacity",           is(200))
                .body("[1].stock",              is(80));
    }

    @Test
    void listAll_mapsAllFieldsCorrectly() {
        when(warehouseRepository.getAll())
                .thenReturn(List.of(domainWarehouse("WH-XYZ", "Berlin", 500, 0)));

        given()
                .when().get("/warehouse")
                .then()
                .statusCode(200)
                .body("[0].businessUnitCode", is("WH-XYZ"))
                .body("[0].location",         is("Berlin"))
                .body("[0].capacity",         is(500))
                .body("[0].stock",            is(0));
    }

    @Test
    void getById_returns200_withWarehouse_whenFound() {
        when(warehouseRepository.findByBusinessUnitCode("WH-001"))
                .thenReturn(domainWarehouse("WH-001", "London", 100, 50));

        given()
                .when().get("/warehouse/WH-001")
                .then()
                .statusCode(200)
                .body("businessUnitCode", is("WH-001"))
                .body("location",         is("London"))
                .body("capacity",         is(100))
                .body("stock",            is(50));
    }

    @Test
    void getById_returns404_whenNotFound() {
        when(warehouseRepository.findByBusinessUnitCode("WH-999")).thenReturn(null);

        given()
                .when().get("/warehouse/WH-999")
                .then()
                .statusCode(404)
                .body(containsString("WH-999"));
    }

    @Test
    void create_returns200_withCreatedWarehouse() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "businessUnitCode": "WH-NEW",
                  "location": "Madrid",
                  "capacity": 300,
                  "stock": 10
                }
                """)
                .when().post("/warehouse")
                .then()
                .statusCode(200)
                .body("businessUnitCode", is("WH-NEW"))
                .body("location",         is("Madrid"))
                .body("capacity",         is(300))
                .body("stock",            is(10));

        verify(createWarehouseOperation).create(argThat(w ->
                "WH-NEW".equals(w.businessUnitCode) &&
                        "Madrid".equals(w.location)         &&
                        w.capacity == 300                   &&
                        w.stock == 10
        ));
    }

    @Test
    void create_defaultsStockToZero_whenStockIsNull() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "businessUnitCode": "WH-NULL",
                  "location": "Rome",
                  "capacity": 100
                }
                """)
                .when().post("/warehouse")
                .then()
                .statusCode(200)
                .body("stock", is(0));

        verify(createWarehouseOperation).create(argThat(w -> w.stock == 0));
    }

    @Test
    void create_returns400_whenCreateOperationThrowsIllegalArgument() {
        doThrow(new IllegalArgumentException("Capacity must be positive"))
                .when(createWarehouseOperation).create(any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "businessUnitCode": "WH-BAD",
                  "location": "Nowhere",
                  "capacity": -1,
                  "stock": 0
                }
                """)
                .when().post("/warehouse")
                .then()
                .statusCode(400)
                .body(containsString("Capacity must be positive"));
    }

    @Test
    void archive_returns204_whenWarehouseArchivedSuccessfully() {
        when(warehouseRepository.findByBusinessUnitCode("WH-001"))
                .thenReturn(domainWarehouse("WH-001", "London", 100, 50));

        given()
                .when().delete("/warehouse/WH-001")
                .then()
                .statusCode(204);

        verify(archiveWarehouseOperation).archive(argThat(w ->
                "WH-001".equals(w.businessUnitCode)
        ));
    }

    @Test
    void archive_returns404_whenWarehouseNotFound() {
        when(warehouseRepository.findByBusinessUnitCode("WH-999")).thenReturn(null);

        given()
                .when().delete("/warehouse/WH-999")
                .then()
                .statusCode(404)
                .body(containsString("WH-999"));

        verifyNoInteractions(archiveWarehouseOperation);
    }

    @Test
    void archive_returns400_whenArchiveOperationThrowsIllegalArgument() {
        var domain = domainWarehouse("WH-001", "London", 100, 50);
        when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(domain);
        doThrow(new IllegalArgumentException("Already archived"))
                .when(archiveWarehouseOperation).archive(any());

        given()
                .when().delete("/warehouse/WH-001")
                .then()
                .statusCode(400)
                .body(containsString("Already archived"));
    }

    @Test
    void replace_returns200_withUpdatedWarehouse() {
        when(warehouseRepository.findByBusinessUnitCode("WH-001"))
                .thenReturn(domainWarehouse("WH-001", "Amsterdam", 400, 20));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "businessUnitCode": "WH-001",
                  "location": "Amsterdam",
                  "capacity": 400,
                  "stock": 20
                }
                """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200)
                .body("businessUnitCode", is("WH-001"))
                .body("location",         is("Amsterdam"))
                .body("capacity",         is(400))
                .body("stock",            is(20));
    }

    @Test
    void replace_usesPathBusinessUnitCode_notBodyCode() {
        // Body has a different code — the path param must take precedence
        when(warehouseRepository.findByBusinessUnitCode("WH-PATH"))
                .thenReturn(domainWarehouse("WH-PATH", "Amsterdam", 400, 20));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "businessUnitCode": "WH-BODY",
                  "location": "Amsterdam",
                  "capacity": 400,
                  "stock": 20
                }
                """)
                .when().post("/warehouse/WH-PATH/replacement")
                .then()
                .statusCode(200);

        verify(replaceWarehouseOperation).replace(argThat(w ->
                "WH-PATH".equals(w.businessUnitCode)
        ));
    }

    @Test
    void replace_defaultsStockToZero_whenStockIsNull() {
        when(warehouseRepository.findByBusinessUnitCode("WH-001"))
                .thenReturn(domainWarehouse("WH-001", "Oslo", 200, 0));

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "businessUnitCode": "WH-001",
                  "location": "Oslo",
                  "capacity": 200
                }
                """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(200)
                .body("stock", is(0));

        verify(replaceWarehouseOperation).replace(argThat(w -> w.stock == 0));
    }

    @Test
    void replace_returns400_whenReplaceOperationThrowsIllegalArgument() {
        doThrow(new IllegalArgumentException("Warehouse not active"))
                .when(replaceWarehouseOperation).replace(any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "businessUnitCode": "WH-001",
                  "location": "Amsterdam",
                  "capacity": 400,
                  "stock": 20
                }
                """)
                .when().post("/warehouse/WH-001/replacement")
                .then()
                .statusCode(400)
                .body(containsString("Warehouse not active"));
    }

    @Test
    void search_returns200_withAllFiltersProvided() {
        var w1 = domainWarehouse("WH-001", "AMSTERDAM-001", 100, 50);
        var w2 = domainWarehouse("WH-002", "AMSTERDAM-001", 200, 80);
        when(warehouseRepository.search(any())).thenReturn(List.of(w1, w2));

        given()
                .queryParam("location",    "AMSTERDAM-001")
                .queryParam("minCapacity", 50)
                .queryParam("maxCapacity", 300)
                .queryParam("sortBy",      "capacity")
                .queryParam("sortOrder",   "asc")
                .queryParam("page",        0)
                .queryParam("pageSize",    10)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("$.size()",             is(2))
                .body("[0].businessUnitCode", is("WH-001"))
                .body("[0].location",         is("AMSTERDAM-001"))
                .body("[1].businessUnitCode", is("WH-002"));
    }

    @Test
    void search_returns200_withEmptyList_whenNoMatchFound() {
        when(warehouseRepository.search(any())).thenReturn(List.of());

        given()
                .queryParam("location", "NOWHERE-999")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    void search_withNoQueryParams_returns200() {
        // All params are optional — bare request is valid
        when(warehouseRepository.search(any())).thenReturn(List.of(
                domainWarehouse("WH-001", "LONDON-001", 100, 50)
        ));

        given()
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("$.size()", is(1));
    }

    @Test
    void search_mapsAllResponseFieldsCorrectly() {
        when(warehouseRepository.search(any()))
                .thenReturn(List.of(domainWarehouse("WH-MAP", "BERLIN-001", 999, 123)));

        given()
                .queryParam("location", "BERLIN-001")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("[0].businessUnitCode", is("WH-MAP"))
                .body("[0].location",         is("BERLIN-001"))
                .body("[0].capacity",         is(999))
                .body("[0].stock",            is(123));
    }

    @Test
    void search_passesAllQueryParamsCorrectlyToRepository() {
        when(warehouseRepository.search(any())).thenReturn(List.of());

        given()
                .queryParam("location",    "LONDON-001")
                .queryParam("minCapacity", 100)
                .queryParam("maxCapacity", 500)
                .queryParam("sortBy",      "capacity")
                .queryParam("sortOrder",   "desc")
                .queryParam("page",        1)
                .queryParam("pageSize",    20)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200);

        verify(warehouseRepository).search(argThat(req ->
                "LONDON-001".equals(req.location)  &&
                        req.minCapacity == 100             &&
                        req.maxCapacity == 500             &&
                        "capacity".equals(req.sortBy)      &&
                        "desc".equals(req.sortOrder)       &&
                        req.page == 1                      &&
                        req.pageSize == 20
        ));
    }

    @Test
    void search_withSortByCreatedAt_passesCorrectSortField() {
        // createdAt is the other valid sortBy enum value
        when(warehouseRepository.search(any())).thenReturn(List.of());

        given()
                .queryParam("sortBy",    "createdAt")
                .queryParam("sortOrder", "asc")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200);

        verify(warehouseRepository).search(argThat(req ->
                "createdAt".equals(req.sortBy) && "asc".equals(req.sortOrder)
        ));
    }

    @Test
    void search_withSortByCapacityDesc_passesCorrectSortField() {
        when(warehouseRepository.search(any())).thenReturn(List.of());

        given()
                .queryParam("sortBy",    "capacity")
                .queryParam("sortOrder", "desc")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200);

        verify(warehouseRepository).search(argThat(req ->
                "capacity".equals(req.sortBy) && "desc".equals(req.sortOrder)
        ));
    }

    @Test
    void search_withMaxAllowedPageSize_returns200() {
        // pageSize max per spec is 100
        when(warehouseRepository.search(any())).thenReturn(List.of());

        given()
                .queryParam("pageSize", 100)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200);

        verify(warehouseRepository).search(argThat(req -> req.pageSize == 100));
    }

    @Test
    void search_withZeroIndexedPage_passesCorrectPageToRepository() {
        // page is 0-indexed per spec
        when(warehouseRepository.search(any())).thenReturn(List.of());

        given()
                .queryParam("page",     0)
                .queryParam("pageSize", 10)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200);

        verify(warehouseRepository).search(argThat(req ->
                req.page == 0 && req.pageSize == 10
        ));
    }

    @Test
    void search_returns400_whenMinCapacityExceedsMaxCapacity() {
        // WareHouseSearchRequest constructor throws before the repository is called
        // so the mock is not needed — the real message comes from the constructor
        given()
                .queryParam("minCapacity", 500)
                .queryParam("maxCapacity", 100)
                .when().get("/warehouse/search")
                .then()
                .statusCode(400)
                .body(containsString("minCapacity (500) must not be greater than maxCapacity (100)"));
    }

    @Test
    void search_returns400_whenInvalidSortByValueProvided() {
        doThrow(new IllegalArgumentException("Invalid sortBy value. Must be one of: createdAt, capacity"))
                .when(warehouseRepository).search(any());

        given()
                .queryParam("sortBy", "invalidField")
                .when().get("/warehouse/search")
                .then()
                .statusCode(400)
                .body(containsString("Invalid sortBy value"));
    }

    @Test
    void search_returns400_whenInvalidSortOrderValueProvided() {
        doThrow(new IllegalArgumentException("Invalid sortOrder value. Must be one of: asc, desc"))
                .when(warehouseRepository).search(any());

        given()
                .queryParam("sortOrder", "random")
                .when().get("/warehouse/search")
                .then()
                .statusCode(400)
                .body(containsString("Invalid sortOrder value"));
    }

    private Warehouse domainWarehouse(String code, String location, int capacity, int stock) {
        var w = new Warehouse();
        w.businessUnitCode = code;
        w.location         = location;
        w.capacity         = capacity;
        w.stock            = stock;
        return w;
    }
}