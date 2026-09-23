package com.example.screens;

import com.example.App;
import com.example.components.BigButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class StudentScreen {

    public Parent build() {
        Label title = new Label("Student Exam");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        Label info = new Label(
            "Waiting for the teacher to load an exam..."
        );
        info.setFont(Font.font("Arial", 20));
        info.setStyle("-fx-text-fill: #555;");

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> App.navigate(new LandingScreen().build()));

        VBox root = new VBox(24, title, info, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: white;");
        return root;
    }
}