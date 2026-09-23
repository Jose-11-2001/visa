package com.example.screens;

import com.example.App;
import com.example.components.AudioRecorder;
import com.example.components.BigButton;
import com.example.components.TtsService;
import com.example.components.TranscriptionService;
import com.example.models.Question;
import com.example.services.ExamStorage;
import com.example.services.PdfReaderService;
import com.example.services.PdfWriterService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class StudentScreen {

    private final AudioRecorder recorder = new AudioRecorder();
    private final PdfReaderService reader = new PdfReaderService();
    private final PdfWriterService writer = new PdfWriterService();

    private List<Question> questions;
    private int currentIndex = 0;
    private boolean isRecording = false;

    private BigButton recordBtn;
    private BigButton nextBtn;

    private static final String MODEL_PATH = "D:/Projects/visa/models/vosk-model-small-en-us-0.15";

    public Parent build() {
        Label title = new Label("Student Exam");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        Label status = new Label("Click 'Load Questions PDF' to begin.");
        status.setFont(Font.font("Arial", 18));
        status.setWrapText(true);
        status.setMaxWidth(800);

        Label questionLabel = new Label("");
        questionLabel.setFont(Font.font("Arial", 22));
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(800);

        TextArea transcript = new TextArea();
        transcript.setEditable(false);
        transcript.setPrefHeight(150);
        transcript.setStyle("-fx-font-size: 18px;");
        transcript.setPromptText("Your spoken answer will appear here...");

        BigButton loadBtn = new BigButton("Load Questions PDF");
        loadBtn.setOnAction(e -> {
            File pdf = choosePdf();
            if (pdf == null)
                return;
            try {
                questions = reader.extractQuestions(pdf);
                currentIndex = 0;
                updateStatus(status, "Loaded " + questions.size() + " questions.");
                recordBtn.setDisable(false);
                askCurrentQuestion(questionLabel, status);
            } catch (Exception ex) {
                updateStatus(status, "Error reading PDF: " + ex.getMessage());
            }
        });

        recordBtn = new BigButton("Start Answering");
        recordBtn.setDisable(true);
        recordBtn.setOnAction(e -> {
            if (questions == null)
                return;
            if (!isRecording)
                startRecording(transcript, status);
            else
                stopRecording(transcript, status);
        });

        nextBtn = BigButton.accent("Next Question");
        nextBtn.setDisable(true);
        nextBtn.setOnAction(e -> {
            if (questions == null)
                return;
            if (currentIndex + 1 >= questions.size()) {
                exportPdf(status);
                return;
            }
            currentIndex++;
            transcript.clear();
            nextBtn.setDisable(true);
            askCurrentQuestion(questionLabel, status);
        });

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> {
            TtsService.stop();
            App.navigate(new LandingScreen().build());
        });

        VBox root = new VBox(16, title, status, questionLabel, transcript,
                loadBtn, recordBtn, nextBtn, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        // Welcome message when the screen opens
        Platform.runLater(() -> TtsService.speak("Student exam. Click Load Questions PDF to begin."));

        return root;
    }

    private File choosePdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Questions PDF");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fc.showOpenDialog(null);
    }

    private void updateStatus(Label status, String text) {
        status.setText(text);
        System.out.println(">>> " + text);
    }

    private void askCurrentQuestion(Label questionLabel, Label status) {
        Question q = questions.get(currentIndex);
        String prompt = "Question " + q.getNumber() + ". " + q.getText();
        questionLabel.setText(prompt);
        updateStatus(status,
                "Question " + q.getNumber() + " of " + questions.size()
                        + ". Click Start Answering when ready.");
        TtsService.speak(prompt + ". Click Start Answering when you are ready.");
    }

    private void startRecording(TextArea transcript, Label status) {
        try {
            recorder.start();
            isRecording = true;
            recordBtn.setText("Stop Answering");
            updateStatus(status, "Recording... speak your answer.");
            transcript.setText("(recording)");
            TtsService.speak("Recording started. Please speak your answer now. "
                    + "Click Stop Answering when you are done.");
        } catch (Exception ex) {
            updateStatus(status, "Mic error: " + ex.getMessage());
            TtsService.speak("Microphone error. " + ex.getMessage());
        }
    }

    private void stopRecording(TextArea transcript, Label status) {
        try {
            byte[] audio = recorder.stop();
            isRecording = false;
            recordBtn.setText("Start Answering");
            updateStatus(status, "Processing your answer...");
            transcript.setText("Processing...");
            TtsService.speak("Please wait. Processing your answer.");

            new Thread(() -> {
                try {
                    TranscriptionService ts = new TranscriptionService(MODEL_PATH);
                    String text = ts.transcribe(
                            audio,
                            recorder.getActualFormat().getSampleRate());
                    ts.close();
                    Platform.runLater(() -> {
                        transcript.setText(text);
                        questions.get(currentIndex).setAnswer(text);

                        String spoken = text.isBlank()
                                ? "I did not catch anything. Please try again."
                                : "You said: " + text + ". Click Next Question to continue.";

                        updateStatus(status, spoken);
                        TtsService.speak(spoken);
                        nextBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        transcript.setText("Transcription error: " + ex.getMessage());
                        TtsService.speak("Transcription error.");
                    });
                }
            }).start();
        } catch (Exception ex) {
            updateStatus(status, "Stop error: " + ex.getMessage());
        }
    }

    private void exportPdf(Label status) {
        try {
            TextInputDialog dialog = new TextInputDialog("Student");
            dialog.setTitle("Save Submission");
            dialog.setHeaderText("Enter the student's name:");
            dialog.setContentText("Name:");
            String name = dialog.showAndWait().orElse("Student");

            ExamStorage.save(name, "Exam", questions);

            File out = new File("D:/Projects/visa/answers_" + name + ".pdf");
            writer.writeAnswersPdf(questions, out);

            updateStatus(status, "Saved: " + out.getAbsolutePath());
            TtsService.speak("Your answers have been saved. Thank you. You may close the app now.");
        } catch (Exception ex) {
            updateStatus(status, "Export error: " + ex.getMessage());
        }
    }
}