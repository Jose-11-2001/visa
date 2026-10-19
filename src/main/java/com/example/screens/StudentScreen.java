package com.example.screens;

import com.example.App;
import com.example.components.AudioRecorder;
import com.example.components.BigButton;
import com.example.components.TtsService;
import com.example.components.TranscriptionService;
import com.example.components.VoiceCommandService;
import com.example.models.Question;
import com.example.services.ExamStorage;
import com.example.services.ModelLocator;
import com.example.services.PdfWriterService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.List;

public class StudentScreen {

    private final AudioRecorder recorder = new AudioRecorder();
    private final PdfWriterService writer = new PdfWriterService();
    private final String modelPath = ModelLocator.findModelPath();
    private final VoiceCommandService voice = new VoiceCommandService(modelPath);

    private List<Question> questions;
    private int currentIndex = 0;
    private boolean isRecording = false;
    private boolean voiceModeEnabled = true; // voice commands on/off
    private String currentExamTitle = "Exam";

    private BigButton recordBtn;
    private BigButton nextBtn;
    private BigButton hearBtn;
    private BigButton finishBtn;
    private BigButton skipBtn;
    private BigButton voiceToggleBtn;
    private BigButton speedBtn;
    private Label questionLabel;
    private Label status;
    private TextArea transcript;

    public Parent build() {
        Label title = new Label("Student Exam");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        status = new Label("Choose an exam from the list below.");
        status.setFont(Font.font("Arial", 18));
        status.setWrapText(true);
        status.setMaxWidth(800);

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

        hearBtn = BigButton.accent("Hear My Answer");
        hearBtn.setDisable(true);
        hearBtn.setOnAction(e -> hearMyAnswer());

        skipBtn = BigButton.accent("Skip Question");
        skipBtn.setDisable(true);
        skipBtn.setOnAction(e -> skipQuestion());

        nextBtn = BigButton.accent("Next Question");
        nextBtn.setDisable(true);
        nextBtn.setOnAction(e -> goNext());

        finishBtn = BigButton.accent("Finish & Submit");
        finishBtn.setDisable(true);
        finishBtn.setOnAction(e -> confirmAndSubmit());

        speedBtn = BigButton.accent("Speed: Normal");
        speedBtn.setOnAction(e -> cycleSpeed());

        voiceToggleBtn = BigButton.accent("Voice: ON");
        voiceToggleBtn.setOnAction(e -> toggleVoiceMode());

        HBox actionRow = new HBox(10, recordBtn, hearBtn, skipBtn, nextBtn, finishBtn);
        actionRow.setAlignment(Pos.CENTER);

        HBox controlRow = new HBox(10, voiceToggleBtn, speedBtn);
        controlRow.setAlignment(Pos.CENTER);

        BigButton helpBtn = BigButton.accent("Voice Commands");
        helpBtn.setOnAction(e -> speakHelp());

        BigButton back = BigButton.accent("Back to Home");
        back.setOnAction(e -> {
            voice.stop();
            TtsService.stop();
            App.navigate(new LandingScreen().build());
        });

        HBox bottomRow = new HBox(10, helpBtn, back);
        bottomRow.setAlignment(Pos.CENTER);

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

        VBox root = new VBox(12, title, status, examPicker, questionLabel,
                transcript, actionRow, controlRow, bottomRow);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: white;");

        voice.setOnCommand(this::handleVoiceCommand);
        try {
            voice.start();
        } catch (Exception ex) {
            System.err.println("Voice listener failed: " + ex.getMessage());
            updateStatus("Voice mode unavailable. Use the buttons.");
        }

        Platform.runLater(() -> TtsService.speak(
                "Student exam ready. Choose an exam from the list. " +
                        "Say answer to begin, stop when done, next to continue, " +
                        "skip to move on, or repeat to hear the question again. " +
                        "Say help for all commands."));

        return root;
    }

    /* ------------------------------------------------------------------ */
    /* Voice command handling */
    /* ------------------------------------------------------------------ */

    private void handleVoiceCommand(String command) {
        if (!voiceModeEnabled)
            return;

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
                        TtsService.speak("Answer the current question first, or say skip.");
                    break;

                case "skip":
                    if (questions != null)
                        skipQuestion();
                    break;

                case "previous":
                    if (questions != null && currentIndex > 0) {
                        currentIndex--;
                        transcript.clear();
                        askCurrentQuestion();
                    } else {
                        TtsService.speak("This is the first question.");
                    }
                    break;

                case "repeat":
                    if (questions != null) {
                        Question q = questions.get(currentIndex);
                        TtsService.speak("Question " + q.getNumber() + ". " + q.getText());
                    }
                    break;

                case "hear":
                    hearMyAnswer();
                    break;

                case "slower":
                    TtsService.slowDown();
                    updateSpeedLabel();
                    TtsService.speak("Speed now " + describeSpeed());
                    break;

                case "faster":
                    TtsService.speedUp();
                    updateSpeedLabel();
                    TtsService.speak("Speed now " + describeSpeed());
                    break;

                case "normal":
                    TtsService.resetSpeed();
                    updateSpeedLabel();
                    TtsService.speak("Speed reset to normal.");
                    break;

                case "submit":
                    if (!finishBtn.isDisabled())
                        confirmAndSubmit();
                    else
                        TtsService.speak("You still have more questions.");
                    break;

                case "help":
                    speakHelp();
                    break;

                case "toggle":
                    toggleVoiceMode();
                    break;

                case "yes":
                    break;

                case "no":
                    break;
            }
        });
    }

    private void toggleVoiceMode() {
        voiceModeEnabled = !voiceModeEnabled;
        if (voiceModeEnabled) {
            voice.resume();
            voiceToggleBtn.setText("Voice: ON");
            TtsService.speak("Voice commands enabled.");
        } else {
            voice.pause();
            voiceToggleBtn.setText("Voice: OFF");
            TtsService.speak("Voice commands disabled. Use the buttons.");
        }
    }

    private void cycleSpeed() {
        int r = TtsService.getRate();
        if (r < -4)
            TtsService.setRate(0);
        else if (r < 0)
            TtsService.setRate(4);
        else if (r < 4)
            TtsService.setRate(-4);
        else
            TtsService.setRate(0);
        updateSpeedLabel();
        TtsService.speak("Speed now " + describeSpeed());
    }

    private void updateSpeedLabel() {
        speedBtn.setText("Speed: " + describeSpeed());
    }

    private String describeSpeed() {
        int r = TtsService.getRate();
        if (r <= -4)
            return "Slow";
        if (r >= 4)
            return "Fast";
        return "Normal";
    }

    private void speakHelp() {
        TtsService.speak(
                "Available voice commands. " +
                        "Say answer to start recording. " +
                        "Say stop when you finish speaking. " +
                        "Say next to move to the next question. " +
                        "Say skip to skip this question. " +
                        "Say previous to go back. " +
                        "Say repeat to hear the question again. " +
                        "Say hear my answer to hear what was written. " +
                        "Say slower or faster to change my speech speed. " +
                        "Say normal to reset the speed. " +
                        "Say finish or submit when you are done. " +
                        "Say voice off to use buttons only, or voice on to re-enable.");
    }

    /* ------------------------------------------------------------------ */
    /* Template loading */
    /* ------------------------------------------------------------------ */

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
            hearBtn.setDisable(true);
            skipBtn.setDisable(false);
            finishBtn.setDisable(true);
            transcript.clear();

            updateStatus("Loaded " + qs.size() + " questions from '" + examName + "'.");
            askCurrentQuestion();
        } catch (Exception ex) {
            updateStatus("Load error: " + ex.getMessage());
        }
    }

    /* ------------------------------------------------------------------ */
    /* UI updates */
    /* ------------------------------------------------------------------ */

    private void updateStatus(String text) {
        status.setText(text);
        System.out.println(">>> " + text);
    }

    private void askCurrentQuestion() {
        Question q = questions.get(currentIndex);
        String prompt = "Question " + q.getNumber() + ". " + q.getText();
        questionLabel.setText(prompt);

        boolean isLast = (currentIndex + 1 >= questions.size());
        finishBtn.setDisable(!isLast);
        hearBtn.setDisable(true);
        nextBtn.setDisable(true);

        updateStatus("Question " + q.getNumber() + " of " + questions.size()
                + (isLast ? " (last question)." : "")
                + " Say answer to begin, repeat to hear again, or skip.");

        TtsService.speak(prompt
                + (isLast
                        ? ". This is the last question. Say answer when ready."
                        : ". Say answer when you are ready."));
    }

    /* ------------------------------------------------------------------ */
    /* Recording */
    /* ------------------------------------------------------------------ */

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
            TtsService.speak("Microphone not available. Use the Skip button to move on.");
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
                    TranscriptionService ts = new TranscriptionService(modelPath);
                    String text = ts.transcribe(audio, recorder.getActualFormat());
                    ts.close();
                    Platform.runLater(() -> {
                        transcript.setText(text);
                        questions.get(currentIndex).setAnswer(text);
                        voice.resume();

                        hearBtn.setDisable(text.isBlank());

                        String spoken = text.isBlank()
                                ? "I did not catch anything. Say answer to try again, or skip."
                                : "You said: " + text + ". Say next to continue, hear to listen again, or skip.";

                        updateStatus(spoken);
                        TtsService.speak(spoken);

                        boolean isLast = (currentIndex + 1 >= questions.size());
                        nextBtn.setDisable(isLast);
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

    /* ------------------------------------------------------------------ */
    /* Hear my answer */
    /* ------------------------------------------------------------------ */

    private void hearMyAnswer() {
        if (questions == null)
            return;
        String answer = questions.get(currentIndex).getAnswer();
        if (answer == null || answer.isBlank()) {
            TtsService.speak("You have not answered yet. Say answer to begin.");
            return;
        }
        TtsService.speak("Your answer to question "
                + questions.get(currentIndex).getNumber()
                + " is: " + answer);
    }

    /* ------------------------------------------------------------------ */
    /* Navigation */
    /* ------------------------------------------------------------------ */

    private void skipQuestion() {
        if (questions == null)
            return;

        // Mark as unanswered (blank) and move forward
        questions.get(currentIndex).setAnswer("");
        transcript.clear();

        if (currentIndex + 1 >= questions.size()) {
            TtsService.speak("Skipped last question. Say finish to submit.");
            finishBtn.setDisable(false);
            nextBtn.setDisable(true);
            return;
        }

        currentIndex++;
        nextBtn.setDisable(true);
        hearBtn.setDisable(true);
        finishBtn.setDisable(true);
        askCurrentQuestion();
    }

    private void goNext() {
        if (currentIndex + 1 >= questions.size()) {
            confirmAndSubmit();
            return;
        }
        currentIndex++;
        transcript.clear();
        nextBtn.setDisable(true);
        hearBtn.setDisable(true);
        finishBtn.setDisable(true);
        askCurrentQuestion();
    }

    /* ------------------------------------------------------------------ */
    /* Submit */
    /* ------------------------------------------------------------------ */

    private void confirmAndSubmit() {
        int answered = 0;
        for (Question q : questions) {
            if (q.getAnswer() != null && !q.getAnswer().isBlank())
                answered++;
        }
        int unanswered = questions.size() - answered;

        String msg = unanswered == 0
                ? "All questions answered. Submit the exam now?"
                : "You still have " + unanswered + " unanswered question(s). Submit anyway?";

        TtsService.speak(msg);

        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Submit Exam");
        confirm.setHeaderText("Finish and submit?");
        confirm.setContentText(msg);

        confirm.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK)
                exportPdf();
        });
    }

    private void exportPdf() {
        try {
            TextInputDialog dialog = new TextInputDialog("Student");
            dialog.setTitle("Save Submission");
            dialog.setHeaderText("Enter the student's name:");
            dialog.setContentText("Name:");
            String name = dialog.showAndWait().orElse("Student");

            ExamStorage.save(name, currentExamTitle, questions);

            String docs = System.getProperty("user.home") + File.separator + "Documents";
            File dir = new File(docs);
            if (!dir.exists())
                dir.mkdirs();

            File out = new File(dir, "answers_" + name + ".pdf");
            writer.writeAnswersPdf(questions, out);

            updateStatus("Saved: " + out.getAbsolutePath());
            TtsService.speak("Your answers have been saved. Thank you. You may close the app now.");

            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            done.setTitle("Submitted");
            done.setHeaderText("Exam submitted");
            done.setContentText("Saved to:\n" + out.getAbsolutePath());
            done.showAndWait();

            voice.stop();
            App.navigate(new LandingScreen().build());
        } catch (Exception ex) {
            updateStatus("Export error: " + ex.getMessage());
        }
    }
}