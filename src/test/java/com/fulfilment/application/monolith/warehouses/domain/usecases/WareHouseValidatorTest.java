package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class WareHouseValidatorTest {

    @InjectMock
    WarehouseStore warehouseStore;

    @InjectMock
    LocationResolver locationResolver;

    @Inject
    WareHouseValidator validator;

    @Test
    void validateExisting_returnsExistingWarehouse_whenFoundAndNotArchived() {
        var existing = activeWarehouse("WH-001", "London", 100, 50);
        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);

        var result = validator.validateExisting(warehouse("WH-001"));

        assertSame(existing, result);
        verify(warehouseStore).findByBusinessUnitCode("WH-001");
    }

    @Test
    void validateExisting_throwsIllegalArgument_whenWarehouseNotFound() {
        when(warehouseStore.findByBusinessUnitCode("WH-999")).thenReturn(null);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExisting(warehouse("WH-999")));

        assertTrue(ex.getMessage().contains("WH-999"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void validateExisting_throwsIllegalArgument_whenWarehouseIsArchived() {
        var archived = activeWarehouse("WH-001", "London", 100, 50);
        archived.archivedAt = LocalDateTime.now();
        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(archived);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExisting(warehouse("WH-001")));

        assertTrue(ex.getMessage().contains("WH-001"));
        assertTrue(ex.getMessage().contains("archived"));
    }

    @Test
    void validateExistingAndLocation_returnsValidatedWarehouse_whenAllValid() {
        var existing = activeWarehouse("WH-001", "London", 100, 50);
        var location = location("London", 500);
        var newWh    = warehouse("WH-001", "London", 200, 100);

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("London")).thenReturn(location);

        var result = validator.validateExistingAndLocation(newWh);

        assertSame(existing, result.existing());
        assertSame(location, result.location());
    }

    @Test
    void validateExistingAndLocation_allowsCapacityEqualToMaxCapacity() {
        var existing = activeWarehouse("WH-001", "London", 100, 50);
        var location = location("London", 300);
        var newWh    = warehouse("WH-001", "London", 300, 100); // capacity == maxCapacity

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("London")).thenReturn(location);

        assertDoesNotThrow(() -> validator.validateExistingAndLocation(newWh));
    }

    @Test
    void validateExistingAndLocation_allowsStockEqualToCapacity() {
        var existing = activeWarehouse("WH-001", "London", 100, 50);
        var location = location("London", 500);
        var newWh    = warehouse("WH-001", "London", 200, 200); // stock == capacity

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("London")).thenReturn(location);

        assertDoesNotThrow(() -> validator.validateExistingAndLocation(newWh));
    }

    @Test
    void validateExistingAndLocation_throwsIllegalArgument_whenWarehouseNotFound() {
        when(warehouseStore.findByBusinessUnitCode("WH-999")).thenReturn(null);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExistingAndLocation(warehouse("WH-999", "London", 100, 10)));

        assertTrue(ex.getMessage().contains("WH-999"));
        assertTrue(ex.getMessage().contains("does not exist"));
        verifyNoInteractions(locationResolver);
    }

    @Test
    void validateExistingAndLocation_throwsIllegalArgument_whenWarehouseIsArchived() {
        var archived = activeWarehouse("WH-001", "London", 100, 50);
        archived.archivedAt = LocalDateTime.now();
        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(archived);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExistingAndLocation(warehouse("WH-001", "London", 100, 10)));

        assertTrue(ex.getMessage().contains("archived"));
        verifyNoInteractions(locationResolver);
    }

    @Test
    void validateExistingAndLocation_throwsIllegalArgument_whenLocationNotFound() {
        var existing = activeWarehouse("WH-001", "Unknown", 100, 50);
        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("Unknown")).thenReturn(null);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExistingAndLocation(warehouse("WH-001", "Unknown", 100, 10)));

        assertTrue(ex.getMessage().contains("Unknown"));
        assertTrue(ex.getMessage().contains("not valid"));
    }

    @Test
    void validateExistingAndLocation_throwsIllegalArgument_whenCapacityExceedsLocationMax() {
        var existing = activeWarehouse("WH-001", "London", 100, 50);
        var location = location("London", 200);
        var newWh    = warehouse("WH-001", "London", 201, 50); // 201 > 200

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("London")).thenReturn(location);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExistingAndLocation(newWh));

        assertTrue(ex.getMessage().contains("201"));
        assertTrue(ex.getMessage().contains("200"));
        assertTrue(ex.getMessage().contains("exceeds location max capacity"));
    }

    @Test
    void validateExistingAndLocation_throwsIllegalArgument_whenStockExceedsCapacity() {
        var existing = activeWarehouse("WH-001", "London", 100, 50);
        var location = location("London", 500);
        var newWh    = warehouse("WH-001", "London", 200, 201); // stock 201 > capacity 200

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("London")).thenReturn(location);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExistingAndLocation(newWh));

        assertTrue(ex.getMessage().contains("201"));
        assertTrue(ex.getMessage().contains("200"));
        assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
    }

    @Test
    void validateExistingAndLocation_checksCapacityBeforeStock() {
        // Both violations present — capacity check should fire first
        var existing = activeWarehouse("WH-001", "London", 100, 50);
        var location = location("London", 100); // maxCapacity = 100
        var newWh    = warehouse("WH-001", "London", 150, 200); // capacity > max AND stock > capacity

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("London")).thenReturn(location);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateExistingAndLocation(newWh));

        assertTrue(ex.getMessage().contains("exceeds location max capacity"),
                "Expected capacity check to fire before stock check");
    }


    private Warehouse warehouse(String code) {
        return warehouse(code, null, 0, 0);
    }

    private Warehouse warehouse(String code, String location, int capacity, int stock) {
        var w = new Warehouse();
        w.businessUnitCode = code;
        w.location         = location;
        w.capacity         = capacity;
        w.stock            = stock;
        return w;
    }

    private Warehouse activeWarehouse(String code, String location, int capacity, int stock) {
        var w = warehouse(code, location, capacity, stock);
        w.archivedAt = null;
        return w;
    }

    private Location location(String identifier, int maxCapacity) {
        return new Location(identifier, 10, maxCapacity);
    }
}