package com.mycompany.presentacion.controllers;

import com.mycompany.persistencia.enums.MetodoPago;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

@Controller
public class ConfirmarPagoModalController {

    @FXML private Label lblSubtotal;
    @FXML private Label lblCargosExtra;
    @FXML private Label lblTotal;
    @FXML private TextField txtAnticipo;
    @FXML private ComboBox<MetodoPago> comboMetodoPago;
    @FXML private TextField txtReferencia;
    @FXML private VBox vboxReferencia;
    @FXML private Button btnConfirmar;

    private boolean pagoConfirmado = false;
    private double montoAnticipo = 0.0;
    private MetodoPago metodoSeleccionado;
    private String referenciaPago = "";

    @FXML
    public void initialize() {
        comboMetodoPago.setItems(FXCollections.observableArrayList(MetodoPago.values()));

        // Listener para mostrar/ocultar el campo de referencia según el método
        comboMetodoPago.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && (newValue == MetodoPago.TRANSFERENCIA || newValue == MetodoPago.TARJETA)) {
                vboxReferencia.setVisible(true);
                vboxReferencia.setManaged(true);
            } else {
                vboxReferencia.setVisible(false);
                vboxReferencia.setManaged(false);
                txtReferencia.clear();
            }
        });
    }

    public void setDatosCotizacion(double subtotal, double cargosExtra) {
        double total = subtotal + cargosExtra;
        lblSubtotal.setText(String.format("$%.2f", subtotal));
        lblCargosExtra.setText(String.format("$%.2f", cargosExtra));
        lblTotal.setText(String.format("$%.2f", total));
    }

    @FXML
    private void confirmar() {
        try {
            String input = txtAnticipo.getText().replace("$", "").replace(",", "").trim();
            montoAnticipo = Double.parseDouble(input);
            metodoSeleccionado = comboMetodoPago.getValue();
            referenciaPago = txtReferencia.getText().trim();

            if (metodoSeleccionado != null && montoAnticipo > 0) {
                // Validación: si es tarjeta/transferencia, exigir que haya referencia
                if ((metodoSeleccionado == MetodoPago.TRANSFERENCIA || metodoSeleccionado == MetodoPago.TARJETA)
                        && referenciaPago.isEmpty()) {
                    System.out.println("Debe ingresar el folio de autorización o referencia.");
                    return;
                }

                pagoConfirmado = true;
                cerrarModal();
            } else {
                System.out.println("Por favor, seleccione un método de pago e ingrese un monto válido.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Monto de anticipo inválido.");
        }
    }

    @FXML
    private void cancelar() {
        pagoConfirmado = false;
        cerrarModal();
    }

    private void cerrarModal() {
        Stage stage = (Stage) btnConfirmar.getScene().getWindow();
        stage.close();
    }

    public boolean isPagoConfirmado() { return pagoConfirmado; }
    public double getMontoAnticipo() { return montoAnticipo; }
    public MetodoPago getMetodoSeleccionado() { return metodoSeleccionado; }
    public String getReferenciaPago() { return referenciaPago; }
}