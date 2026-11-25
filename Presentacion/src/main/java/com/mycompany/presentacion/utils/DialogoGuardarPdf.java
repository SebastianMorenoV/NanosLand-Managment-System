package com.mycompany.presentacion.utils;

import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javax.swing.filechooser.FileSystemView;

/**
 * Diálogo "Guardar como" para los PDF que genera el sistema (contratos, comprobantes,
 * reportes): abre en la carpeta de Documentos del usuario, con el nombre sugerido ya escrito.
 */
public final class DialogoGuardarPdf {

    private DialogoGuardarPdf() {
    }

    /**
     * Pregunta dónde guardar el PDF.
     *
     * @return el archivo elegido, o {@code null} si se canceló
     */
    public static File elegir(Window ventana, String titulo, String nombreSugerido) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(titulo);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        File documentos = carpetaDocumentos();
        if (documentos != null) {
            fileChooser.setInitialDirectory(documentos);
        }
        fileChooser.setInitialFileName(nombreSugerido);
        return fileChooser.showSaveDialog(ventana);
    }

    /** Hace que un diálogo ya armado abra en la carpeta de Documentos. */
    public static void abrirEnDocumentos(FileChooser fileChooser) {
        File documentos = carpetaDocumentos();
        if (documentos != null) {
            fileChooser.setInitialDirectory(documentos);
        }
    }

    /**
     * La carpeta Documentos que el sistema operativo tiene asignada al usuario (en Windows
     * respeta que se haya movido, p. ej. a OneDrive). Si no se puede saber, la carpeta
     * personal; {@code null} si tampoco existe, y el diálogo abre donde decida el sistema.
     */
    public static File carpetaDocumentos() {
        try {
            File carpeta = FileSystemView.getFileSystemView().getDefaultDirectory();
            if (carpeta != null && carpeta.isDirectory()) {
                return carpeta;
            }
        } catch (RuntimeException e) {
            // Sin acceso a las carpetas del sistema: se usan las de respaldo.
        }
        File personal = new File(System.getProperty("user.home"));
        File documentos = new File(personal, "Documents");
        if (documentos.isDirectory()) {
            return documentos;
        }
        return personal.isDirectory() ? personal : null;
    }
}
