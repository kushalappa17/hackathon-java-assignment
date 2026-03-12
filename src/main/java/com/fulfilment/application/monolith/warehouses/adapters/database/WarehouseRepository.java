package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.WareHouseSearchRequest;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

  @Override
  public List<Warehouse> search(WareHouseSearchRequest request) {
    SearchQuery query = buildQuery(request);
    String jpql = "SELECT w FROM DbWarehouse w WHERE " + query.where
            + " ORDER BY w." + request.sortBy + " " + request.sortOrder;

    TypedQuery<DbWarehouse> q = getEntityManager()
            .createQuery(jpql, DbWarehouse.class)
            .setFirstResult(request.page * request.pageSize)
            .setMaxResults(request.pageSize);

    query.applyParams(q);

    return q.getResultList().stream().map(DbWarehouse::toWarehouse).toList();
  }

  private SearchQuery buildQuery(WareHouseSearchRequest request) {
    List<String> conditions = new ArrayList<>();
    List<Object[]> params = new ArrayList<>();

    // Always exclude archived warehouses
    conditions.add("w.archivedAt IS NULL");

    if (request.location != null && !request.location.isBlank()) {
      conditions.add("w.location = :location");
      params.add(new Object[]{"location", request.location});
    }
    if (request.minCapacity != null) {
      conditions.add("w.capacity >= :minCapacity");
      params.add(new Object[]{"minCapacity", request.minCapacity});
    }
    if (request.maxCapacity != null) {
      conditions.add("w.capacity <= :maxCapacity");
      params.add(new Object[]{"maxCapacity", request.maxCapacity});
    }

    return new SearchQuery(String.join(" AND ", conditions), params);
  }

  private static class SearchQuery {
    final String where;
    final List<Object[]> params;

    SearchQuery(String where, List<Object[]> params) {
      this.where = where;
      this.params = params;
    }

    void applyParams(TypedQuery<?> query) {
      for (Object[] param : params) {
        query.setParameter((String) param[0], param[1]);
      }
    }
  }


}
