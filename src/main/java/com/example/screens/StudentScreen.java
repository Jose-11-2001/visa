package com.example.screens;

import com.example.App;
import com.example.components.AudioRecorder;
import com.example.components.BigButton;
import com.example.components.TtsService;
import com.example.components.TranscriptionService;
import com.example.components.VoiceCommandService;
import com.example.models.Question;
import com.example.services.ExamStorage;
import com.example.services.PdfWriterService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.List;

public class StudentScreen {

    private final AudioRecorder recorder = new AudioRecorder();
    private final PdfWriterService writer = new PdfWriterService();
    private final VoiceCommandService voice = new VoiceCommandService(MODEL_PATH);

    private List<Question> questions;
    private int currentIndex = 0;
    private boolean isRecording = false;
    private String currentExamTitle = "Exam";

    private BigButton recordBtn;
    private BigButton nextBtn;
    private Label questionLabel;
    private Label status;
    private TextArea transcript;

    private static final String MODEL_PATH = "D:/Projects/visa/models/vosk-model-small-en-us-0.15";

    public Parent build() {
        Label title = new Label("Student Exam");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        status = new Label("Choose an exam from the list below.");
        status.setFont(Font.font("Arial", 18));
        status.setWrapText(true);
        status.setMaxWidth(800);

        // Dropdown of teacher-uploaded exams
        ComboBox<String> examPicker = new ComboBox<>();
        examPicker.getItems().addAll(ExamStorage.listTemplates());
        examPicker.setPromptText("Select an exam...");
        examPicker.setPrefWidth(500);

        questionLabel = new Label("");
        questionLabel.setFont(Font.font("Arial", 22));
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(800);

        transcript = new TextArea();
        transcript.setEditable(false);
        transcript.setPrefHeight(150);
        transcript.setStyle("-fx-font-size: 18px;");
        transcript.setPromptText("Your spoken answer will appear here...");

        recordBtn = new BigButton("Start Answering");
        recordBtn.setDisable(true);
        recordBtn.setOnAction(e -> {
            if (!isRecording)
                startRecording();
            else
                stopRecording();
        });

        nextBtn = BigButton.accent("Next Question");
        nextBtn.setDisable(true);
        nextBtn.setOnAction(e -> goNext());

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> {
            voice.stop();
            TtsService.stop();
            App.navigate(new LandingScreen().build());
        });

        // When teacher uploads a new exam while this screen is open, refresh
        examPicker.setOnShowing(e -> {
            examPicker.getItems().clear();
            examPicker.getItems().addAll(ExamStorage.listTemplates());
        });

        examPicker.setOnAction(e -> {
            String chosen = examPicker.getValue();
            if (chosen == null)
                return;
            loadTemplate(chosen);
        });

        VBox root = new VBox(16, title, status, examPicker, questionLabel,
                transcript, recordBtn, nextBtn, back);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        // Start voice command listener
        voice.setOnCommand(this::handleVoiceCommand);
        try {
            voice.start();
        } catch (Exception ex) {
            System.err.println("Voice listener failed: " + ex.getMessage());
        }

        Platform.runLater(() -> TtsService.speak(
                "Student exam ready. Choose an exam from the list. " +
                        "Then say answer to begin, stop when done, and next to continue."));

        return root;
    }

    /** Loads a teacher-saved exam template. */
    private void loadTemplate(String examName) {
        try {
            List<Question> qs = ExamStorage.loadTemplate(examName);
            if (qs.isEmpty()) {
                updateStatus("Exam '" + examName + "' has no questions.");
                return;
            }
            questions = qs;
            currentIndex = 0;
            currentExamTitle = examName;

            recordBtn.setDisable(false);
            nextBtn.setDisable(true);
            transcript.clear();

            updateStatus("Loaded " + qs.size() + " questions from '" + examName + "'.");
            askCurrentQuestion();
        } catch (Exception ex) {
            updateStatus("Load error: " + ex.getMessage());
        }
    }

    private void handleVoiceCommand(String command) {
        Platform.runLater(() -> {
            switch (command) {
                case "answer":
                    if (questions == null)
                        TtsService.speak("Choose an exam first.");
                    else if (!isRecording)
                        startRecording();
                    break;
                case "stop":
                    if (isRecording)
                        stopRecording();
                    break;
                case "next":
                    if (!nextBtn.isDisabled())
                        goNext();
                    else
                        TtsService.speak("Answer the current question first.");
                    break;
                case "repeat":
                    if (questions != null) {
                        Question q = questions.get(currentIndex);
                        TtsService.speak("Question " + q.getNumber() + ". " + q.getText());
                    }
                    break;
                case "finish":
                    if (questions != null && currentIndex + 1 >= questions.size())
                        exportPdf();
                    else
                        TtsService.speak("You still have more questions.");
                    break;
            }
        });
    }

    private void updateStatus(String text) {
        status.setText(text);
        System.out.println(">>> " + text);
    }

    private void askCurrentQuestion() {
        Question q = questions.get(currentIndex);
        String prompt = "Question " + q.getNumber() + ". " + q.getText();
        questionLabel.setText(prompt);
        updateStatus("Question " + q.getNumber() + " of " + questions.size()
                + ". Say 'answer' to begin, or 'repeat' to hear it again.");
        TtsService.speak(prompt + ". Say answer when you are ready.");
    }

    private void startRecording() {
        try {
            voice.pause();
            recorder.start();
            isRecording = true;
            recordBtn.setText("Stop Answering");
            updateStatus("Recording... speak your answer.");
            transcript.setText("(recording)");
            TtsService.speak("Recording. Please speak. Say stop when done.");
        } catch (Exception ex) {
            voice.resume();
            updateStatus("Mic error: " + ex.getMessage());
            TtsService.speak("Microphone error.");
        }
    }

    private void stopRecording() {
        try {
            byte[] audio = recorder.stop();
            isRecording = false;
            recordBtn.setText("Start Answering");
            updateStatus("Processing your answer...");
            transcript.setText("Processing...");
            TtsService.speak("Processing your answer.");

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
                        voice.resume();

                        String spoken = text.isBlank()
                                ? "I did not catch anything. Say answer to try again."
                                : "You said: " + text + ". Say next to continue.";

                        updateStatus(spoken);
                        TtsService.speak(spoken);
                        nextBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        voice.resume();
                        transcript.setText("Transcription error: " + ex.getMessage());
                        TtsService.speak("Transcription error.");
                    });
                }
            }).start();
        } catch (Exception ex) {
            voice.resume();
            updateStatus("Stop error: " + ex.getMessage());
        }
    }

    private void goNext() {
        if (currentIndex + 1 >= questions.size()) {
            exportPdf();
            return;
        }
        currentIndex++;
        transcript.clear();
        nextBtn.setDisable(true);
        askCurrentQuestion();
    }

    private void exportPdf() {
        try {
            TextInputDialog dialog = new TextInputDialog("Student");
            dialog.setTitle("Save Submission");
            dialog.setHeaderText("Enter the student's name:");
            dialog.setContentText("Name:");
            String name = dialog.showAndWait().orElse("Student");

            ExamStorage.save(name, currentExamTitle, questions);

            File out = new File("D:/Projects/visa/answers_" + name + ".pdf");
            writer.writeAnswersPdf(questions, out);

            updateStatus("Saved: " + out.getAbsolutePath());
            TtsService.speak("Your answers have been saved. Thank you.");
        } catch (Exception ex) {
            updateStatus("Export error: " + ex.getMessage());
        }
    }
}