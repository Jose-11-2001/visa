package com.example.screens;

import com.example.App;
import com.example.components.BigButton;
import com.example.components.TtsService;
import com.example.models.Question;
import com.example.services.ExamStorage;
import com.example.services.ExamStorage.Submission;
import com.example.services.PdfWriterService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewAnswersScreen {

    private final Map<Integer, TextField> markFields = new HashMap<>();
    private final Map<Integer, TextField> commentFields = new HashMap<>();
    private List<Question> currentQuestions = List.of();
    private String currentStudent = "";

    public Parent build() {
        Label title = new Label("Review Student Answers");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));

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
            commentFields.clear();
            content.getChildren().clear();
            currentQuestions = s.questions;
            currentStudent = s.studentName;

            studentLabel.setText("Student: " + s.studentName
                    + " | Exam: " + s.examTitle);

            for (Question q : s.questions) {
                content.getChildren().add(buildQuestionBlock(q));
            }

            TtsService.speak("Loaded submission for " + s.studentName
                    + ". " + s.questions.size() + " answers.");
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
        markField.setPromptText("Marks / " + q.getMaxMarks());
        markField.setPrefWidth(100);
        markFields.put(q.getNumber(), markField);

        TextField commentField = new TextField();
        commentField.setPromptText("Comment (optional)");
        commentField.setPrefWidth(400);
        commentFields.put(q.getNumber(), commentField);

        HBox markRow = new HBox(10,
                new Label("Marks:"), markField,
                new Label("Comment:"), commentField);
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
            Map<Integer, String> marks = new HashMap<>();
            Map<Integer, String> comments = new HashMap<>();
            for (Question q : currentQuestions) {
                TextField m = markFields.get(q.getNumber());
                if (m != null)
                    marks.put(q.getNumber(), m.getText().isBlank() ? "0" : m.getText());

                TextField c = commentFields.get(q.getNumber());
                if (c != null)
                    comments.put(q.getNumber(), c.getText());
            }

            String safeName = currentStudent.replaceAll("[^a-zA-Z0-9_-]", "_");
            File out = new File("D:/Projects/visa/marked_" + safeName + ".pdf");

            PdfWriterService writer = new PdfWriterService();
            writer.writeMarkedPdf(currentStudent, "Exam",
                    currentQuestions, marks, comments, out);

            System.out.println("Saved: " + out.getAbsolutePath());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Complete");
            alert.setHeaderText("Marked PDF saved");
            alert.setContentText(out.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText("Could not write the PDF");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }
}