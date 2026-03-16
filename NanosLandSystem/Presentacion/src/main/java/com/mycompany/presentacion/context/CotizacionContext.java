package com.mycompany.presentacion.context;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Data
public class CotizacionContext {
    private LocalDate fechaSeleccionada;

    public void limpiar() {
        this.fechaSeleccionada = null;
    }
}
