package com.example;

import com.example.screens.LandingScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("VIS Exam App");
        stage.setScene(new Scene(new LandingScreen().build(), 900, 650));
        stage.show();
    }

    /** Allows any screen to swap the current view. */
    public static void navigate(javafx.scene.Parent root) {
        primaryStage.setScene(new Scene(root, 900, 650));
    }

    public static void main(String[] args) {
        launch(args);
    }
}