package cz.pobuda.paneckuzlobse.controller;

import cz.pobuda.paneckuzlobse.App;
import javafx.fxml.FXML;

public class RulesController {

    @FXML
    private void zpetDoMenu() {
        App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Menu.fxml");
    }
}