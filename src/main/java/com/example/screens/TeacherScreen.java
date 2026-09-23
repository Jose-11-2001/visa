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

public class TeacherScreen {

    public Parent build() {
        Label title = new Label("Teacher Dashboard");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        BigButton uploadBtn = new BigButton("Upload Question PDF");
        uploadBtn.setOnAction(e -> System.out.println("TODO: open upload dialog"));

        BigButton reviewBtn = new BigButton("Review Submissions");
        reviewBtn.setOnAction(e -> App.navigate(new ReviewAnswersScreen().build()));

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> App.navigate(new LandingScreen().build()));

        VBox root = new VBox(20, title, uploadBtn, reviewBtn, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: white;");
        return root;
    }
}