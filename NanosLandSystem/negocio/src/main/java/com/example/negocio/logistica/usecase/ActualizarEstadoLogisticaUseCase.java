package com.example.negocio.logistica.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.LogisticaDTO;
import com.mycompany.common.mapper.LogisticaMapper;
import com.mycompany.persistencia.dominio.LogisticaServicio;
import com.mycompany.persistencia.enums.EstadoLogistica;
import com.mycompany.persistencia.repository.LogisticaServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para actualizar el estado y datos de logística de un servicio (CU-06).
 *
 * Responsabilidades:
 * - Cambiar el estado de un servicio en la bitácora (POR_CONTACTAR → ENCARGADO → CONFIRMADO → LISTO).
 * - Actualizar los datos operativos (responsable, ubicación, hora, etc.).
 */
@Service
@RequiredArgsConstructor
public class ActualizarEstadoLogisticaUseCase {

    private final LogisticaServicioRepository logisticaRepo;

    @Transactional
    public LogisticaDTO actualizarEstado(Long logisticaId, EstadoLogistica nuevoEstado) {
        LogisticaServicio entidad = logisticaRepo.findById(logisticaId)
                .orElseThrow(() -> new CotizacionException("No se encontró el registro de logística con ID: " + logisticaId));

        entidad.setEstado(nuevoEstado);
        LogisticaServicio guardado = logisticaRepo.save(entidad);
        return LogisticaMapper.toDTO(guardado);
    }

    @Transactional
    public LogisticaDTO actualizarDatos(Long logisticaId, LogisticaDTO datos) {
        LogisticaServicio entidad = logisticaRepo.findById(logisticaId)
                .orElseThrow(() -> new CotizacionException("No se encontró el registro de logística con ID: " + logisticaId));

        if (datos.getResponsableTurno() != null) {
            entidad.setResponsableTurno(datos.getResponsableTurno().trim());
        }
        if (datos.getUbicacionMontaje() != null) {
            entidad.setUbicacionMontaje(datos.getUbicacionMontaje().trim());
        }
        if (datos.getEspecificaciones() != null) {
            entidad.setEspecificaciones(datos.getEspecificaciones().trim());
        }
        if (datos.getDesgloseOpciones() != null) {
            entidad.setDesgloseOpciones(datos.getDesgloseOpciones().trim());
        }
        if (datos.getHoraRequerida() != null) {
            entidad.setHoraRequerida(datos.getHoraRequerida());
        }
        if (datos.getEstado() != null) {
            entidad.setEstado(datos.getEstado());
        }

        LogisticaServicio guardado = logisticaRepo.save(entidad);
        return LogisticaMapper.toDTO(guardado);
    }
}
