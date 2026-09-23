module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires vosk;
    requires org.apache.pdfbox;

    exports com.example;
    exports com.example.models;

    exports com.example.screens;
    exports com.example.components;
    exports com.example.services; // <-- You also need to export your new service package

    opens com.example to javafx.fxml;
}