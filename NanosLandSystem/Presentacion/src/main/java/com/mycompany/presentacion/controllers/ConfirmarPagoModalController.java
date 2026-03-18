package com.mycompany.presentacion.controllers;

import com.mycompany.persistencia.enums.MetodoPago;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

@Controller
public class ConfirmarPagoModalController {

    @FXML private Label lblSubtotal;
    @FXML private Label lblCargosExtra;
    @FXML private Label lblTotal;
    @FXML private TextField txtAnticipo;
    @FXML private ComboBox<MetodoPago> comboMetodoPago;
    @FXML private Button btnConfirmar;

    // Variables to store the results
    private boolean pagoConfirmado = false;
    private double montoAnticipo = 0.0;
    private MetodoPago metodoSeleccionado;

    @FXML
    public void initialize() {
        // Populate the combo box with the enum values
        comboMetodoPago.setItems(FXCollections.observableArrayList(MetodoPago.values()));
    }

    // Method to pass data from your main screen into this modal
    public void setDatosCotizacion(double subtotal, double cargosExtra) {
        double total = subtotal + cargosExtra;
        lblSubtotal.setText(String.format("$%.2f", subtotal));
        lblCargosExtra.setText(String.format("$%.2f", cargosExtra));
        lblTotal.setText(String.format("$%.2f", total));
    }

    @FXML
    private void confirmar() {
        try {
            // Clean the input and parse the number
            String input = txtAnticipo.getText().replace("$", "").replace(",", "").trim();
            montoAnticipo = Double.parseDouble(input);
            metodoSeleccionado = comboMetodoPago.getValue();

            // Basic validation
            if (metodoSeleccionado != null && montoAnticipo > 0) {
                pagoConfirmado = true;
                cerrarModal();
            } else {
                System.out.println("Por favor, llene todos los campos correctamente.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Monto inválido.");
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

    // Getters so the parent controller can retrieve the user's choices
    public boolean isPagoConfirmado() { return pagoConfirmado; }
    public double getMontoAnticipo() { return montoAnticipo; }
    public MetodoPago getMetodoSeleccionado() { return metodoSeleccionado; }
}