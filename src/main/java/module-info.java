module cz.pobuda.paneckuzlobse.paneckuzlobse {
    requires javafx.controls;
    requires javafx.fxml;

    opens cz.pobuda.paneckuzlobse to javafx.fxml;
    opens cz.pobuda.paneckuzlobse.controller to javafx.fxml;
    opens cz.pobuda.paneckuzlobse.images;

    exports cz.pobuda.paneckuzlobse;
}