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

public class LandingScreen {

    public Parent build() {
        Label title = new Label("VIS Exam App");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));

        Label subtitle = new Label(
                "Accessible exams for visually impaired students");
        subtitle.setFont(Font.font("Arial", 20));
        subtitle.setStyle("-fx-text-fill: #555;");

        BigButton studentBtn = new BigButton("I am a Student");
        studentBtn.setOnAction(e -> App.navigate(new StudentScreen().build()));
        BigButton teacherBtn = BigButton.accent("I am a Teacher");
        teacherBtn.setOnAction(e -> App.navigate(new TeacherScreen().build()));

        VBox root = new VBox(24, title, subtitle, studentBtn, teacherBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: white;");
        return root;
    }
}