package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WareHouseValidator {

    private final  LocationResolver locationResolver;
    private final  WarehouseStore warehouseStore;

    public WareHouseValidator(WarehouseStore warehouseStore, LocationResolver locationResolver){
        this.warehouseStore = warehouseStore;
        this.locationResolver = locationResolver;
    }

    protected Warehouse validateExisting(Warehouse newWarehouse) {
        Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
        if (existing == null) {
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' does not exist");
        }

        if (existing.archivedAt != null) {
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' is archived and cannot be replaced");
        }

        return existing;
    }

    protected ValidatedWarehouse validateExistingAndLocation(Warehouse newWarehouse) {
        Warehouse existing = validateExisting(newWarehouse);

        Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
        if (location == null) {
            throw new IllegalArgumentException(
                    "Location '" + newWarehouse.location + "' is not valid");
        }

        if (newWarehouse.capacity > location.maxCapacity()) {
            throw new IllegalArgumentException(
                    "Warehouse capacity (" + newWarehouse.capacity +
                            ") exceeds location max capacity (" + location.maxCapacity() + ")");
        }

        if (newWarehouse.stock > newWarehouse.capacity) {
            throw new IllegalArgumentException(
                    "Warehouse stock (" + newWarehouse.stock +
                            ") exceeds warehouse capacity (" + newWarehouse.capacity + ")");
        }

        return new ValidatedWarehouse(existing, location);
    }

    protected record ValidatedWarehouse(Warehouse existing, Location location) {}

}
