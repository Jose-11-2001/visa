package com.example.screens;

import com.example.App;
import com.example.components.BigButton;
import com.example.components.TtsService;
import com.example.components.VoiceCommandService;
import com.example.services.ModelLocator;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StudyScreen {

    private final String modelPath = ModelLocator.findModelPath();
    private final VoiceCommandService voice = new VoiceCommandService(modelPath);

    private List<String> paragraphs = new ArrayList<>();
    private int currentIndex = 0;

    private Label paragraphLabel;
    private Label status;
    private ScrollPane scroll;

    public Parent build() {
        Label title = new Label("Study Notes");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        status = new Label("Load a PDF to start studying.");
        status.setFont(Font.font("Arial", 18));
        status.setWrapText(true);
        status.setMaxWidth(800);

        paragraphLabel = new Label("");
        paragraphLabel.setFont(Font.font("Arial", 22));
        paragraphLabel.setWrapText(true);
        paragraphLabel.setPadding(new Insets(10));

        scroll = new ScrollPane(paragraphLabel);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 8;");

        BigButton loadBtn = new BigButton("Load PDF Notes");
        loadBtn.setOnAction(e -> loadPdf());

        BigButton repeatBtn = BigButton.accent("Repeat");
        repeatBtn.setOnAction(e -> readCurrent());

        BigButton prevBtn = BigButton.accent("Previous");
        prevBtn.setOnAction(e -> previousParagraph());

        BigButton nextBtn = BigButton.accent("Next");
        nextBtn.setOnAction(e -> nextParagraph());

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> {
            voice.stop();
            TtsService.stop();
            App.navigate(new LandingScreen().build());
        });

        HBox navRow = new HBox(10, prevBtn, repeatBtn, nextBtn);
        navRow.setAlignment(Pos.CENTER);

        VBox root = new VBox(16, title, status, loadBtn, scroll, navRow, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        voice.setOnCommand(this::handleVoiceCommand);
        try {
            voice.start();
        } catch (Exception ex) {
            System.err.println("Voice listener failed: " + ex.getMessage());
            updateStatus("Voice mode unavailable. Use the buttons.");
        }

        return root;
    }

    private void handleVoiceCommand(String command) {
        Platform.runLater(() -> {
            switch (command) {
                case "next":
                    nextParagraph();
                    break;
                case "previous":
                    previousParagraph();
                    break;
                case "repeat":
                    readCurrent();
                    break;
                case "stop":
                    TtsService.stop();
                    updateStatus("Reading paused.");
                    break;
                case "slower":
                    TtsService.slowDown();
                    TtsService.speak("Speed now " + describeSpeed());
                    break;
                case "faster":
                    TtsService.speedUp();
                    TtsService.speak("Speed now " + describeSpeed());
                    break;
                case "normal":
                    TtsService.resetSpeed();
                    TtsService.speak("Speed reset to normal.");
                    break;
                case "help":
                    TtsService.speak("Say next to read the next paragraph. " +
                            "Say previous to go back. " +
                            "Say repeat to read again. " +
                            "Say stop to pause. " +
                            "Say faster or slower to change speed.");
                    break;
            }
        });
    }

    private void loadPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Notes PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File pdf = fc.showOpenDialog(null);
        if (pdf == null)
            return;

        try (PDDocument doc = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(doc);

            // Split into paragraphs based on double line breaks or single line breaks
            String[] blocks = text.split("\\n\\s*\\n");
            paragraphs.clear();
            for (String b : blocks) {
                String trimmed = b.trim().replace("\n", " ");
                if (trimmed.length() > 20) { // Skip tiny fragments
                    paragraphs.add(trimmed);
                }
            }

            if (paragraphs.isEmpty()) {
                updateStatus("No readable text found in this PDF.");
                return;
            }

            currentIndex = 0;
            updateStatus("Loaded " + paragraphs.size() + " paragraphs.");
            readCurrent();
        } catch (Exception ex) {
            updateStatus("Error reading PDF: " + ex.getMessage());
        }
    }

    private void readCurrent() {
        if (paragraphs.isEmpty())
            return;
        String text = paragraphs.get(currentIndex);
        paragraphLabel.setText(text);
        scroll.setVvalue(0); // Scroll to top
        updateStatus("Paragraph " + (currentIndex + 1) + " of " + paragraphs.size());
        TtsService.speak(text);
    }

    private void nextParagraph() {
        if (paragraphs.isEmpty() || currentIndex >= paragraphs.size() - 1) {
            TtsService.speak("End of notes.");
            return;
        }
        currentIndex++;
        readCurrent();
    }

    private void previousParagraph() {
        if (paragraphs.isEmpty() || currentIndex <= 0) {
            TtsService.speak("This is the first paragraph.");
            return;
        }
        currentIndex--;
        readCurrent();
    }

    private void updateStatus(String msg) {
        status.setText(msg);
        System.out.println(">>> " + msg);
    }

    private String describeSpeed() {
        int r = TtsService.getRate();
        if (r <= -4)
            return "Slow";
        if (r >= 4)
            return "Fast";
        return "Normal";
    }
}