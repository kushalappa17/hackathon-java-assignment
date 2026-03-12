package com.fulfilment.application.monolith.warehouses.domain.models;

public class WareHouseSearchRequest {

    public String location;
    public Integer minCapacity;
    public Integer maxCapacity;
    public String sortBy;
    public String sortOrder;
    public int page;
    public int pageSize;

    public WareHouseSearchRequest(
            String location,
            Integer minCapacity,
            Integer maxCapacity,
            String sortBy,
            String sortOrder,
            Integer page,
            Integer pageSize) {

        if (minCapacity != null && maxCapacity != null && minCapacity > maxCapacity) {
            throw new IllegalArgumentException(
                    "minCapacity (" + minCapacity + ") must not be greater than maxCapacity (" + maxCapacity + ")");
        }
        if (page != null && page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (pageSize != null && pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be > 0");
        }

        this.location = location;
        this.minCapacity = minCapacity;
        this.maxCapacity = maxCapacity;
        this.sortBy = (sortBy != null && sortBy.equals("capacity")) ? "capacity" : "createdAt";
        this.sortOrder = (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) ? "desc" : "asc";
        this.page = page != null ? page : 0;
        this.pageSize = (pageSize != null) ? Math.min(pageSize, 100) : 10;
    }
}
