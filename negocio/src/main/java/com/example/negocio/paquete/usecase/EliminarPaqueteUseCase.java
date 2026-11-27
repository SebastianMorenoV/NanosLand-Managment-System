package com.example.negocio.paquete.usecase;

import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.repository.PaqueteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import com.example.negocio.exception.CotizacionException;

@Service
@Transactional
public class EliminarPaqueteUseCase {

    @Autowired
    private PaqueteRepository paqueteRepository;

    @Autowired
    private com.mycompany.persistencia.repository.CotizacionRepository cotizacionRepository;

    public void eliminarPaqueteLogico(Long id) {
        Optional<Paquete> opt = paqueteRepository.findById(id);
        if (opt.isEmpty() || !opt.get().isActivo()) {
            throw new CotizacionException("El paquete no existe o ya está eliminado.");
        }

        // Las cotizaciones abiertas con su copia del paquete no dependen de él: conservan lo que se
        // les vendió aunque el paquete se dé de baja. Las que aún no la tienen, sí dependen.
        java.util.List<String> sinCopia = cotizacionRepository.foliosAbiertosSinCopiaDelPaquete(id,
            ActualizarPaqueteUseCase.ESTADOS_ABIERTOS, com.mycompany.persistencia.enums.EstadoEvento.FINALIZADO);
        if (!sinCopia.isEmpty()) {
            throw new CotizacionException("No se puede eliminar el paquete todavía: estas cotizaciones abiertas no "
                + "guardan su copia del paquete (" + String.join(", ", sinCopia) + "). Cierre y vuelva a abrir el "
                + "sistema para que se guarde.");
        }

        Paquete p = opt.get();
        p.setActivo(false);
        p.getServicios().clear(); // Desvincula los servicios asociados
        // La tabla de precios se conserva: no estorba (las comidas en uso solo se revisan
        // en paquetes activos) y deja ver con qué precios se vendió el paquete.
        paqueteRepository.save(p);
    }
}
