package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.list("archivedAt IS NULL").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.version = 0L;
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    this.persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse managed = getEntityManager()
            .createQuery("SELECT w FROM DbWarehouse w WHERE w.businessUnitCode = :code", DbWarehouse.class)
            .setParameter("code", warehouse.businessUnitCode)
            .getSingleResult();

    // Hibernate tracks 'managed' — will check @Version on commit
    managed.location = warehouse.location;
    managed.capacity = warehouse.capacity;
    managed.stock = warehouse.stock;
    managed.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse managed = getEntityManager()
            .createQuery("SELECT w FROM DbWarehouse w WHERE w.businessUnitCode = :code", DbWarehouse.class)
            .setParameter("code", warehouse.businessUnitCode)
            .getSingleResult();

    managed.archivedAt = warehouse.archivedAt;
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }


}
