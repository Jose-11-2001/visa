package com.example.screens;

import com.example.App;
import com.example.components.BigButton;
import com.example.models.Question;
import com.example.services.ExamStorage;
import com.example.services.ExamStorage.Submission;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewAnswersScreen {

    private final Map<Integer, TextField> markFields = new HashMap<>();
    private List<Question> currentQuestions = List.of();
    private String currentStudent = "";

    public Parent build() {
        Label title = new Label("Review Student Answers");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        // Dropdown of submissions
        ComboBox<Submission> picker = new ComboBox<>();
        picker.getItems().addAll(ExamStorage.listSubmissions());
        picker.setPrefWidth(500);
        picker.setPromptText("Select a student submission...");

        Label studentLabel = new Label("No submission selected.");
        studentLabel.setFont(Font.font("Arial", 16));

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);

        picker.setOnAction(e -> {
            Submission s = picker.getValue();
            if (s == null)
                return;

            markFields.clear();
            content.getChildren().clear();
            currentQuestions = s.questions;
            currentStudent = s.studentName;

            studentLabel.setText("Student: " + s.studentName
                    + " | Exam: " + s.examTitle);

            for (Question q : s.questions) {
                content.getChildren().add(buildQuestionBlock(q));
            }
        });

        BigButton exportBtn = BigButton.accent("Export Marked PDF");
        exportBtn.setOnAction(e -> exportMarked());

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> App.navigate(new LandingScreen().build()));

        VBox root = new VBox(16, title, picker, studentLabel, scroll, exportBtn, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");
        return root;
    }

    private VBox buildQuestionBlock(Question q) {
        Label qLabel = new Label("Q" + q.getNumber() + ": " + q.getText());
        qLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        qLabel.setWrapText(true);

        Label aLabel = new Label("Answer: " + q.getAnswer());
        aLabel.setFont(Font.font("Arial", 16));
        aLabel.setWrapText(true);
        aLabel.setStyle("-fx-text-fill: #333;");

        TextField markField = new TextField();
        markField.setPromptText("Marks");
        markField.setPrefWidth(100);
        markFields.put(q.getNumber(), markField);

        HBox markRow = new HBox(10, new Label("Marks:"), markField);
        markRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, qLabel, aLabel, markRow);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-border-color: #ccc; -fx-border-radius: 8; -fx-background-color: #fafafa;");
        return box;
    }

    private void exportMarked() {
        if (currentQuestions.isEmpty()) {
            System.out.println("No submission selected.");
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Student: ").append(currentStudent).append("\n\n");
            for (Question q : currentQuestions) {
                String mark = markFields.get(q.getNumber()) != null
                        ? markFields.get(q.getNumber()).getText()
                        : "";
                sb.append("Q").append(q.getNumber()).append(": ").append(q.getText()).append("\n");
                sb.append("Answer: ").append(q.getAnswer()).append("\n");
                sb.append("Marks: ").append(mark).append("\n\n");
            }
            String out = "D:/Projects/visa/marked_" + currentStudent + ".txt";
            java.nio.file.Files.writeString(java.nio.file.Path.of(out), sb.toString());
            System.out.println("Saved: " + out);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}