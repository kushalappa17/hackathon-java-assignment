package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WareHouseValidator {

    private final  LocationResolver locationResolver;
    private final  WarehouseStore warehouseStore;
    private static final Logger LOGGER = Logger.getLogger(WareHouseValidator.class.getName());

    public WareHouseValidator(WarehouseStore warehouseStore, LocationResolver locationResolver){
        this.warehouseStore = warehouseStore;
        this.locationResolver = locationResolver;
    }

    protected Warehouse validateExisting(Warehouse newWarehouse) {
        Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
        if (existing == null) {
            LOGGER.errorf("Warehouse with business unit code", newWarehouse.businessUnitCode);
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' does not exist");
        }

        if (existing.archivedAt != null) {
            LOGGER.errorf("Warehouse with business code is already archived", newWarehouse.businessUnitCode);
            throw new IllegalArgumentException(
                    "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' is archived and cannot be replaced");
        }

        return existing;
    }

    protected ValidatedWarehouse validateExistingAndLocation(Warehouse newWarehouse) {
        Warehouse existing = validateExisting(newWarehouse);

        Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
        if (location == null) {
            LOGGER.errorf("Warehouse location is not valid", newWarehouse.location);
            throw new IllegalArgumentException(
                    "Location '" + newWarehouse.location + "' is not valid");
        }

        if (newWarehouse.capacity > location.maxCapacity()) {
            LOGGER.errorf("Warehouse new capacity exceeds location capacity ", "New " +newWarehouse.location,
                    "Location capacity : "+ location.maxCapacity());
            throw new IllegalArgumentException(
                    "Warehouse capacity (" + newWarehouse.capacity +
                            ") exceeds location max capacity (" + location.maxCapacity() + ")");
        }

        if (newWarehouse.stock > newWarehouse.capacity) {
            LOGGER.errorf("Requested stock exceeds capacity ", "Requested " +newWarehouse.stock,
                    "Capacity : "+ newWarehouse.capacity);
            throw new IllegalArgumentException(
                    "Warehouse stock (" + newWarehouse.stock +
                            ") exceeds warehouse capacity (" + newWarehouse.capacity + ")");
        }

        return new ValidatedWarehouse(existing, location);
    }

    protected record ValidatedWarehouse(Warehouse existing, Location location) {}

}
