package com.fulfilment.application.monolith.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.logging.Logger;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;
  
  @Inject Event<StoreCreatedEvent> storeCreatedEvent;
  
  @Inject Event<StoreUpdatedEvent> storeUpdatedEvent;

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      LOGGER.errorf("Error while fetching store with id %s", id);
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(Store store) {
    if (store.id != null) {
      LOGGER.errorf("Id was invalidly set on request.", store.id);
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    if (store.name == null) {
      LOGGER.errorf("Store name does not exist");
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    if (Store.find("name", store.name).firstResult() != null) {
      LOGGER.errorf("Store already exists", store.name);
      throw new WebApplicationException(
              "Store with name '" + store.name + "' already exists.", 422);
    }

    LOGGER.infof("STORE", store);
    store.persist();
    storeCreatedEvent.fire(new StoreCreatedEvent(store));

    return Response.ok(store).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      LOGGER.errorf("Store name does not exist");
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      LOGGER.errorf("Store with the ID does not exist", id);
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = updatedStore.name;

    if (updatedStore.quantityProductsInStock != 0) {
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    }

    LOGGER.infof("Store", entity);
    storeUpdatedEvent.fireAsync(new StoreUpdatedEvent(entity));

    return entity;
  }

  @PATCH
  @Path("{id}")
  @Transactional
  public Store patch(Long id, Store updatedStore) {

    Store entity = Store.findById(id);

    if (entity == null) {
      LOGGER.errorf("Store with the ID does not exist", id);
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    if(updatedStore.name != null){
      entity.name = updatedStore.name;
    }

    if (updatedStore.quantityProductsInStock != 0) {
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    }
    LOGGER.infof("Store", entity);
    storeUpdatedEvent.fireAsync(new StoreUpdatedEvent(entity));

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      LOGGER.errorf("Store with the ID does not exist", id);
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    LOGGER.infof("Store deleted successfully");
    return Response.status(204).build();
  }
}
