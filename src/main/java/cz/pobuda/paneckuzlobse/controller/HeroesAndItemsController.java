package cz.pobuda.paneckuzlobse.controller;

import cz.pobuda.paneckuzlobse.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class HeroesAndItemsController {

    @FXML
    private void zpetDoMenu(ActionEvent event) {
        App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Menu.fxml");
    }
}