module me.julionxn.nobaitc {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.controlsfx.controls;
    requires static lombok;

    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;

    requires org.kordamp.ikonli.fontawesome6;
    requires org.kordamp.ikonli.typicons;

    requires kernel;
    requires layout;
    requires io;
    requires org.slf4j;
    requires org.apache.logging.log4j;

    exports me.julionxn.nobaitc;
    exports me.julionxn.nobaitc.controllers;
    exports me.julionxn.nobaitc.data;

    opens me.julionxn.nobaitc to javafx.fxml;
    opens me.julionxn.nobaitc.controllers to javafx.fxml;
    opens me.julionxn.nobaitc.data.nonbpa to javafx.fxml, javafx.base;
    exports me.julionxn.nobaitc.data.nonbpa;
}