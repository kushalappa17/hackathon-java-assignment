package com.fulfilment.application.monolith.products;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;


@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ProductResource {

  @Inject ProductRepository productRepository;

  private static final Logger LOGGER = Logger.getLogger(ProductResource.class.getName());

  @GET
  public List<Product> get() {
    return productRepository.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Product getSingle(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      LOGGER.errorf("Error while fetching product with id %s", id);
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(@Valid Product product) {
    if (product.id != null) {
      LOGGER.errorf("Id was invalidly set on request.", product.id);
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    if (productRepository.find("name", product.name).firstResult() != null) {
      LOGGER.error("Product with name '" + product.name + "' already exists.");
      throw new WebApplicationException(
              "Product with name '" + product.name + "' already exists.", 422);
    }

    productRepository.persist(product);
    return Response.ok(product).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Product update(Long id, Product product) {
    if (product.name == null) {
      LOGGER.error("Product Name was not set on request.");
      throw new WebApplicationException("Product Name was not set on request.", 422);
    }

    if (product.price == null) {
      LOGGER.error("Product Price was not set on request.");
      throw new WebApplicationException("Product Price was not set on request.", 422);
    }

    Product entity = productRepository.findById(id);

    if (entity == null) {
      LOGGER.error("Product with id of " + id + " does not exist.");
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }

    entity.name = product.name;
    entity.description = product.description;
    entity.price = product.price;
    entity.stock = product.stock;

    LOGGER.info("Product entity :"+ entity);

    productRepository.persist(entity);

    return entity;
  }

  @PATCH
  @Path("{id}")
  @Transactional
  public Product patch(Long id, Product product) {
    Product entity = productRepository.findById(id);

    if (entity == null) {
      LOGGER.error("Product with id of " + id + " does not exist.");
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }

    if (product.name != null) {
      entity.name = product.name;
      LOGGER.error("Product does not exist.");
    }
    if (product.description != null) {
      entity.description = product.description;
      LOGGER.error("Product description not found.");
    }
    if (product.price != null) {
      entity.price = product.price;
      LOGGER.error("Product with price not found");
    }
    if (product.stock != 0) {
      entity.stock = product.stock;
      LOGGER.error("Product with id of " + id + " does not exist.");
    }

    LOGGER.info("Product entity :"+ entity);
    productRepository.persist(entity);
    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      LOGGER.error("Product with id of " + id + " does not exist.");
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    productRepository.delete(entity);
    return Response.status(204).build();
  }
}
