package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class.getName());

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // Validation 1: Business unit code must be unique
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      LOGGER.errorf("Ware house with the unit code already exists", warehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists");
    }

    // Validation 2: Location must be valid (must exist)
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      LOGGER.errorf("Invalid Ware house location", warehouse.location);
      throw new IllegalArgumentException(
          "Location '" + warehouse.location + "' is not valid");
    }

    // Validation 3: Capacity validation
    // - Capacity cannot exceed location's max capacity
    if (warehouse.capacity > location.maxCapacity()) {
      LOGGER.errorf("Ware house capacity exceeds max ",
              "Current : "+warehouse.capacity +"Max : "+ location.maxCapacity());
      throw new IllegalArgumentException(
          "Warehouse capacity (" + warehouse.capacity + 
          ") exceeds location max capacity (" + location.maxCapacity() + ")");
    }

    // - Stock cannot exceed capacity
    if (warehouse.stock > warehouse.capacity) {
      LOGGER.errorf("Ware house stock exceeds its capacity",
              "Current : "+warehouse.stock +"Max : "+ location.maxCapacity());
      throw new IllegalArgumentException(
          "Warehouse stock (" + warehouse.stock + 
          ") exceeds warehouse capacity (" + warehouse.capacity + ")");
    }

    // Set creation timestamp
    warehouse.createdAt = java.time.LocalDateTime.now();
    LOGGER.infof("Warehouse", warehouse);
    // All validations passed, create the warehouse
    warehouseStore.create(warehouse);
  }
}
