package com.mycompany.common.mapper;

import com.mycompany.common.dtos.ProductoDTO;
import com.mycompany.persistencia.dominio.Producto;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static ProductoDTO toDTO(Producto producto) {
        if (producto == null) return null;
        ProductoDTO dto = new ProductoDTO();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setPrecio(producto.getPrecio());
        return dto;
    }
}
