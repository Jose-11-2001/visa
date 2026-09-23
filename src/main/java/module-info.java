module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires vosk;       // ← Changed from org.vosk
    
    exports com.example;
    exports com.example.screens;
    exports com.example.components;

    opens com.example to javafx.fxml;
}