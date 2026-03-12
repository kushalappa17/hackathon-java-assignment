package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class.getName());

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // Validation 1: Warehouse must exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      LOGGER.errorf("Ware house with the unit code already exists", newWarehouse.businessUnitCode);
      throw new IllegalArgumentException(
              "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' does not exist");
    }

    // Validation 2: Warehouse must not be archived
    if (existing.archivedAt != null) {
      LOGGER.errorf("Ware house is archived", existing.location);
      throw new IllegalArgumentException(
              "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' is archived and cannot be replaced");
    }

    // Validation 3: Location must be valid
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      LOGGER.errorf("Ware house location is invalid", newWarehouse.location);
      throw new IllegalArgumentException(
              "Location '" + newWarehouse.location + "' is not valid");
    }

    // Validation 4: Capacity validation
    // - Capacity cannot exceed location's max capacity
    if (newWarehouse.capacity > location.maxCapacity()) {
      LOGGER.errorf("Warehouse new capacity exceeds location capacity ", "New " +newWarehouse.location,
              "Location capacity : "+ location.maxCapacity());
      throw new IllegalArgumentException(
              "Warehouse capacity (" + newWarehouse.capacity +
                      ") exceeds location max capacity (" + location.maxCapacity() + ")");
    }

    // - Stock cannot exceed capacity
    if (newWarehouse.stock > newWarehouse.capacity) {
      LOGGER.errorf("Requested stock exceeds capacity ", "Requested " +newWarehouse.stock,
              "Capacity : "+ newWarehouse.capacity);
      throw new IllegalArgumentException(
              "Warehouse stock (" + newWarehouse.stock +
                      ") exceeds warehouse capacity (" + newWarehouse.capacity + ")");
    }

    // Update warehouse fields (preserve createdAt, businessUnitCode, archivedAt)
    existing.location = newWarehouse.location;
    existing.capacity = newWarehouse.capacity;
    existing.stock = newWarehouse.stock;

    LOGGER.infof("Updated warehouse", existing);

    // Update the warehouse
    warehouseStore.update(existing);
  }
}
