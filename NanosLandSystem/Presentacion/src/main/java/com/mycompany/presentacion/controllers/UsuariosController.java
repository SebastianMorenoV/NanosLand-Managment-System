package com.mycompany.presentacion.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Controller;

@Controller
public class UsuariosController {

    @FXML
    private TableView<?> tablaUsuarios;

    @FXML
    public void initialize() {
        if (tablaUsuarios != null) {
            tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }
    }
}
