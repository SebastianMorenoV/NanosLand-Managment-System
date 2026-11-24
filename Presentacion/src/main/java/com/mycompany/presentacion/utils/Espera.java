package com.mycompany.presentacion.utils;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Ejecuta en segundo plano las operaciones que pueden tardar (consultas pesadas,
 * PDF, inicio de sesión) y mientras tanto cubre la ventana con un aviso de que
 * el sistema está trabajando, para que no parezca que se trabó.
 * <p>
 * La capa bloquea clics y teclado desde el primer momento (así no se lanza dos
 * veces la misma operación), pero solo se hace visible si el trabajo tarda más de
 * {@value #RETRASO_MS} ms: en operaciones rápidas no hay parpadeo.
 */
public final class Espera {

    private static final int RETRASO_MS = 250;

    private Espera() {
    }

    /**
     * Ejecuta {@code trabajo} fuera del hilo de JavaFX con el aviso sobre la ventana de
     * {@code origen}. Las respuestas se entregan en el hilo de JavaFX, después de
     * quitar el aviso. Debe llamarse desde el hilo de JavaFX.
     *
     * @param origen     cualquier nodo de la ventana a cubrir; si todavía no está en
     *                   una ventana (pantalla que se está armando), el aviso aparece
     *                   en cuanto se coloque.
     * @param mensaje    qué se está haciendo, por ejemplo "Generando el PDF…".
     * @param trabajo    la operación; no debe tocar la interfaz.
     * @param alTerminar recibe el resultado.
     * @param alFallar   recibe el error (ya registrado en el log).
     */
    public static <T> void ejecutar(Node origen, String mensaje, Callable<T> trabajo,
            Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Capa capa = new Capa(mensaje);
        capa.colocarSobre(origen);
        lanzar(capa, trabajo, alTerminar, alFallar);
    }

    /**
     * Igual que {@link #ejecutar}, pero el aviso cubre solo {@code zona} (por ejemplo,
     * la raíz de una pantalla mientras carga sus datos): el menú sigue disponible.
     */
    public static <T> void ejecutarEnZona(Pane zona, String mensaje, Callable<T> trabajo,
            Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Capa capa = new Capa(mensaje);
        capa.colocarEn(zona);
        lanzar(capa, trabajo, alTerminar, alFallar);
    }

    private static <T> void lanzar(Capa capa, Callable<T> trabajo,
            Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> {
            capa.quitar();
            alTerminar.accept(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            capa.quitar();
            Throwable error = tarea.getException();
            error.printStackTrace();
            alFallar.accept(error);
        });
        Thread hilo = new Thread(tarea, "hilo-espera");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Capa semitransparente con el indicador y el mensaje. */
    private static final class Capa {

        private final StackPane nodo;
        private final FadeTransition aparecer;
        private Scene escena;
        private Node focoAnterior;
        private Pane anfitrion;
        private ChangeListener<Number> ajustarTamano;
        private StackPane envoltura;
        private Parent raizOriginal;
        private boolean quitada;

        Capa(String mensaje) {
            ProgressIndicator indicador = new ProgressIndicator();
            indicador.getStyleClass().add("espera-indicador");
            Label texto = new Label(mensaje);
            texto.getStyleClass().add("espera-texto");

            VBox tarjeta = new VBox(14, indicador, texto);
            tarjeta.setAlignment(Pos.CENTER);
            tarjeta.getStyleClass().add("espera-tarjeta");
            tarjeta.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            nodo = new StackPane(tarjeta);
            nodo.getStyleClass().add("espera-capa");
            // Sus estilos viajan con la capa: no dependen de dónde se coloque.
            nodo.getStylesheets().add(Espera.class.getResource(ModalHelper.HOJA_ESTILOS).toExternalForm());
            nodo.setPickOnBounds(true);
            nodo.setCursor(Cursor.WAIT);
            nodo.setFocusTraversable(true);
            // Enter o espacio sobre el botón que lanzó la operación no deben repetirla.
            nodo.addEventFilter(KeyEvent.ANY, KeyEvent::consume);

            nodo.setOpacity(0);
            aparecer = new FadeTransition(Duration.millis(150), nodo);
            aparecer.setDelay(Duration.millis(RETRASO_MS));
            aparecer.setToValue(1);
            aparecer.play();
        }

        void colocarSobre(Node origen) {
            if (origen.getScene() != null) {
                colocar(origen.getScene());
                return;
            }
            origen.sceneProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Scene> obs, Scene antes, Scene ahora) {
                    if (ahora != null) {
                        origen.sceneProperty().removeListener(this);
                        if (!quitada) {
                            colocar(ahora);
                        }
                    }
                }
            });
        }

        private void colocar(Scene s) {
            escena = s;
            focoAnterior = s.getFocusOwner();
            Parent raiz = s.getRoot();
            if (raiz instanceof Pane pane) {
                colocarEn(pane);
            } else {
                // Raíz sin hijos accesibles (por ejemplo, un ScrollPane): se envuelve mientras dura.
                raizOriginal = raiz;
                envoltura = new StackPane();
                s.setRoot(envoltura);
                envoltura.getChildren().addAll(raiz, nodo);
                nodo.requestFocus();
            }
        }

        /** Encima de todo dentro de {@code pane}, sin alterar la distribución de la pantalla. */
        void colocarEn(Pane pane) {
            anfitrion = pane;
            nodo.setManaged(false);
            ajustarTamano = (obs, antes, ahora) -> nodo.resizeRelocate(0, 0, pane.getWidth(), pane.getHeight());
            pane.widthProperty().addListener(ajustarTamano);
            pane.heightProperty().addListener(ajustarTamano);
            nodo.resizeRelocate(0, 0, pane.getWidth(), pane.getHeight());
            pane.getChildren().add(nodo);
            nodo.requestFocus();
        }

        void quitar() {
            quitada = true;
            aparecer.stop();
            if (anfitrion != null) {
                anfitrion.widthProperty().removeListener(ajustarTamano);
                anfitrion.heightProperty().removeListener(ajustarTamano);
                anfitrion.getChildren().remove(nodo);
            } else if (envoltura != null) {
                envoltura.getChildren().clear();
                if (escena.getRoot() == envoltura) {
                    escena.setRoot(raizOriginal);
                }
            }
            if (focoAnterior != null && focoAnterior.getScene() == escena) {
                focoAnterior.requestFocus();
            }
        }
    }
}
