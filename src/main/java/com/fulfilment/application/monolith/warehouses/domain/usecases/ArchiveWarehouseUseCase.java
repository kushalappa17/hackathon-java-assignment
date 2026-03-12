package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@ApplicationScoped
@Transactional
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private static final Logger LOGGER = Logger.getLogger(ArchiveWarehouseUseCase.class.getName());

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    // Validation 1: Warehouse must exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing == null) {
      LOGGER.errorf("Ware house with the unit code does not exist", warehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }

    // Validation 2: Warehouse must not already be archived
    if (existing.archivedAt != null) {
      LOGGER.errorf("Ware house has already archived", existing.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' is already archived");
    }

    // Set archive timestamp
    existing.stock = warehouse.stock;
    existing.archivedAt = java.time.LocalDateTime.now();
    LOGGER.info(warehouse);
    // Update the warehouse
    warehouseStore.update(existing);
  }
}
