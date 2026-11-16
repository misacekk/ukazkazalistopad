package cz.pobuda.paneckuzlobse.controller;

import cz.pobuda.paneckuzlobse.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class MenuController {

    @FXML
    private void spustitHru(ActionEvent event) {
        App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Game.fxml");
    }

    @FXML
    private void otevritLore(ActionEvent event) {
        App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/HeroesAndItems.fxml");
    }

    @FXML
    private void ukoncitAplikaci(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    private void otvritPravidla() {
        App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Rules.fxml");
    }
}