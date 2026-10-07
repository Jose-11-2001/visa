package com.example.screens;

import com.example.App;
import com.example.components.BigButton;
import com.example.components.TtsService;
import com.example.models.Question;
import com.example.services.ExamStorage;
import com.example.services.PdfReaderService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class UploadQuestionsScreen {

    public Parent build() {
        Label title = new Label("Upload Question PDF");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        Label status = new Label("Pick a PDF file with numbered questions.");
        status.setFont(Font.font("Arial", 16));
        status.setWrapText(true);
        status.setMaxWidth(700);

        TextField examTitleField = new TextField();
        examTitleField.setPromptText("Exam title (e.g. Biology Midterm)");
        examTitleField.setPrefWidth(500);

        VBox preview = new VBox(10);
        preview.setPadding(new Insets(15));

        ScrollPane scroll = new ScrollPane(preview);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(350);
        scroll.setStyle("-fx-border-color: #ccc; -fx-border-radius: 8;");

        // Keep a reference to the parsed questions
        final List<Question>[] parsed = new List[] { null };

        BigButton pickBtn = new BigButton("Choose PDF");
        pickBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose Questions PDF");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File pdf = fc.showOpenDialog(null);
            if (pdf == null)
                return;

            try {
                List<Question> qs = new PdfReaderService().extractQuestions(pdf);
                parsed[0] = qs;

                preview.getChildren().clear();
                for (Question q : qs) {
                    Label l = new Label("Q" + q.getNumber() + ": " + q.getText());
                    l.setWrapText(true);
                    l.setFont(Font.font("Arial", 14));
                    preview.getChildren().add(l);
                }

                examTitleField.setText(pdf.getName().replaceAll("\\.pdf$", ""));
                status.setText("Loaded " + qs.size() + " questions. Review below, then click Save.");
                TtsService.speak("Loaded " + qs.size() + " questions. Review them, then click Save exam.");
            } catch (Exception ex) {
                status.setText("Error reading PDF: " + ex.getMessage());
                TtsService.speak("Error reading the PDF.");
            }
        });

        BigButton saveBtn = BigButton.accent("Save Exam");
        saveBtn.setOnAction(e -> {
            if (parsed[0] == null || parsed[0].isEmpty()) {
                status.setText("Please load a PDF first.");
                return;
            }
            String examTitle = examTitleField.getText().isBlank()
                    ? "Exam"
                    : examTitleField.getText().trim();

            try {
                File saved = ExamStorage.saveTemplate(examTitle, parsed[0]);
                status.setText("Saved exam template: " + saved.getAbsolutePath());
                TtsService.speak("Exam saved. The student can now take this exam.");
            } catch (Exception ex) {
                status.setText("Save error: " + ex.getMessage());
            }
        });

        BigButton back = BigButton.accent("Back to Teacher");
        back.setOnAction(e -> App.navigate(new TeacherScreen().build()));

        VBox root = new VBox(14, title, examTitleField, pickBtn, status,
                scroll, saveBtn, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");
        return root;
    }
}