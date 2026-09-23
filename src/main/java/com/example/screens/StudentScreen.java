package com.example.screens;

import com.example.App;
import com.example.components.AudioRecorder;
import com.example.components.BigButton;
import com.example.components.TranscriptionService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class StudentScreen {

    private AudioRecorder recorder = new AudioRecorder();
    private boolean isRecording = false;

    public Parent build() {
        Label title = new Label("Student Exam");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        TextArea transcript = new TextArea();
        transcript.setEditable(false);
        transcript.setPrefHeight(200);
        transcript.setStyle("-fx-font-size: 18px;");
        transcript.setPromptText("Your words will appear here...");

        BigButton recordBtn = new BigButton("Start Recording");
        recordBtn.setOnAction(e -> {
            if (!isRecording) {
                try {
                    recorder.start();
                    isRecording = true;
                    recordBtn.setText("Stop Recording");
                    transcript.setText("Recording... speak now.");
                } catch (Exception ex) {
                    transcript.setText("Error: " + ex.getMessage());
                }
            } else {
                try {
                    byte[] audio = recorder.stop();
                    isRecording = false;
                    recordBtn.setText("Start Recording");
                    transcript.setText("Processing...");

                    // Transcribe on background thread
                    new Thread(() -> {
                        try {
                            // Path to your Vosk model folder
                            String modelPath = "D:/Projects/visa/models/vosk-model-small-en-us-0.15";
                            TranscriptionService ts = new TranscriptionService(modelPath);
                            String text = ts.transcribe(audio);
                            ts.close();
                            Platform.runLater(() -> transcript.setText(text));
                        } catch (Exception ex) {
                            Platform.runLater(() ->
                                transcript.setText("Transcription error: " + ex.getMessage()));
                        }
                    }).start();
                } catch (Exception ex) {
                    transcript.setText("Error: " + ex.getMessage());
                }
            }
        });

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> App.navigate(new LandingScreen().build()));

        VBox root = new VBox(20, title, transcript, recordBtn, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: white;");
        return root;
    }
}