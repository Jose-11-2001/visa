package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // --- Title ---
        Label title = new Label("VIS Exam App");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 40));

        // --- Subtitle ---
        Label subtitle = new Label("Accessible exams for visually impaired students");
        subtitle.setFont(Font.font("Arial", 18));
        subtitle.setStyle("-fx-text-fill: #555;");

        // --- Buttons ---
        Button studentBtn = new Button("I am a Student");
        studentBtn.setPrefWidth(300);
        studentBtn.setPrefHeight(60);
        studentBtn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        studentBtn.setOnAction(e -> showStudentScreen(stage));

        Button teacherBtn = new Button("I am a Teacher");
        teacherBtn.setPrefWidth(300);
        teacherBtn.setPrefHeight(60);
        teacherBtn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        teacherBtn.setOnAction(e -> showTeacherScreen(stage));

        // --- Layout ---
        VBox root = new VBox(20, title, subtitle, studentBtn, teacherBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("VIS Exam App");
        stage.setScene(scene);
        stage.show();
    }

    private void showStudentScreen(Stage stage) {
        Label label = new Label("Student Screen");
        label.setFont(Font.font("Arial", 32));

        Button back = new Button("Back");
        back.setOnAction(e -> start(stage));

        VBox root = new VBox(20, label, back);
        root.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(root, 800, 600));
    }

    private void showTeacherScreen(Stage stage) {
        Label label = new Label("Teacher Screen");
        label.setFont(Font.font("Arial", 32));

        Button back = new Button("Back");
        back.setOnAction(e -> start(stage));

        VBox root = new VBox(20, label, back);
        root.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(root, 800, 600));
    }

    public static void main(String[] args) {
        launch(args);
    }
}