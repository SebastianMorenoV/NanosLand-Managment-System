package com.example.negocio.estadoCuenta.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.CargoExtraDTO;
import com.mycompany.common.mapper.CargoExtraMapper;
import com.mycompany.persistencia.dominio.CargoExtra;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.repository.CargoExtraRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrarCargoExtraUseCase {

    private final CargoExtraRepository cargoExtraRepository;
    private final EventoRepository eventoRepository;

    @Transactional
    public CargoExtraDTO registrarCargo(CargoExtraDTO dto) {
        if (dto.getEventoId() == null) {
            throw new CotizacionException("El ID del evento es obligatorio.");
        }
        if (dto.getDescripcion() == null || dto.getDescripcion().trim().isEmpty()) {
            throw new CotizacionException("La descripción del cargo extra es obligatoria.");
        }
        if (dto.getCantidad() <= 0) {
            throw new CotizacionException("La cantidad debe ser mayor a cero.");
        }
        if (dto.getPrecioUnitario() < 0) {
            throw new CotizacionException("El precio unitario no puede ser negativo.");
        }

        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new CotizacionException("No se encontró el evento con ID: " + dto.getEventoId()));

        CargoExtra cargo = new CargoExtra();
        cargo.setEvento(evento);
        cargo.setDescripcion(dto.getDescripcion().trim());
        cargo.setCantidad(dto.getCantidad());
        cargo.setPrecioUnitario(dto.getPrecioUnitario());
        
        // Calcular subtotal
        double subtotal = dto.getCantidad() * dto.getPrecioUnitario();
        cargo.setSubtotal(subtotal);
        
        cargo.setFechaHoraCargo(LocalDateTime.now());
        
        // No se asigna servicio de momento, ya que suelen ser custom (ej. horas extra)

        CargoExtra guardado = cargoExtraRepository.save(cargo);
        return CargoExtraMapper.toDTO(guardado);
    }
}
