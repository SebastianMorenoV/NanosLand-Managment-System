package com.mycompany.common.mapper;

import com.mycompany.common.dtos.DetalleVentaDTO;
import com.mycompany.common.dtos.VentaDTO;
import com.mycompany.persistencia.dominio.DetalleVenta;
import com.mycompany.persistencia.dominio.Venta;

import java.util.List;
import java.util.stream.Collectors;

public final class VentaMapper {

    private VentaMapper() {
    }

    public static VentaDTO toDTO(Venta venta) {
        if (venta == null) return null;
        VentaDTO dto = new VentaDTO();
        dto.setId(venta.getId());
        dto.setFolio(venta.getFolio());
        dto.setFechaHora(venta.getFechaHora());
        dto.setMetodoPago(venta.getMetodoPago());
        dto.setTotal(venta.getTotal());
        dto.setEstado(venta.getEstado());
        if (venta.getUsuario() != null) {
            dto.setUsuario(venta.getUsuario().getCorreo());
        }
        dto.setDetalles(venta.getDetalles().stream().map(VentaMapper::detalleToDTO).collect(Collectors.toList()));
        return dto;
    }

    public static List<VentaDTO> toDTOList(List<Venta> ventas) {
        return ventas.stream().map(VentaMapper::toDTO).collect(Collectors.toList());
    }

    private static DetalleVentaDTO detalleToDTO(DetalleVenta detalle) {
        DetalleVentaDTO dto = new DetalleVentaDTO();
        dto.setProductoId(detalle.getProducto().getId());
        dto.setNombreProducto(detalle.getProducto().getNombre());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubtotal(detalle.getSubtotal());
        return dto;
    }
}
