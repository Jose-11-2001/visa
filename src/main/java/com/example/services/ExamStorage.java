package com.example.services;

import com.example.models.Question;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Stores each exam submission as a separate file so the teacher can
 * review any student's session.
 */
public class ExamStorage {

    // Cross-platform base directory: %APPDATA%\VIS Exam App
    private static final String BASE_DIR = System.getenv("APPDATA") != null
            ? System.getenv("APPDATA") + File.separator + "VIS Exam App"
            : System.getProperty("user.home") + File.separator + "VIS Exam App";

    private static final String DIR = BASE_DIR + File.separator + "submissions";
    private static final String TEMPLATE_DIR = BASE_DIR + File.separator + "templates";
    private static final String SEP = "\t";

    /** Represents one saved submission. */
    public static class Submission {
        public final String studentName;
        public final String examTitle;
        public final String timestamp;
        public final File file;
        public final List<Question> questions;

        Submission(String studentName, String examTitle, String timestamp,
                File file, List<Question> questions) {
            this.studentName = studentName;
            this.examTitle = examTitle;
            this.timestamp = timestamp;
            this.file = file;
            this.questions = questions;
        }

        @Override
        public String toString() {
            return studentName + " — " + examTitle + " (" + timestamp + ")";
        }
    }

    /** Save a new submission. Returns the file path. */
    public static File save(String studentName, String examTitle,
            List<Question> questions) throws IOException {
        Files.createDirectories(Path.of(DIR));
        String stamp = String.valueOf(System.currentTimeMillis());
        String safeName = studentName.replaceAll("[^a-zA-Z0-9_-]", "_");
        File file = new File(DIR, safeName + "_" + stamp + ".txt");

        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write("STUDENT" + SEP + studentName);
            w.newLine();
            w.write("EXAM" + SEP + examTitle);
            w.newLine();
            w.write("TIME" + SEP + new java.util.Date());
            w.newLine();
            w.write("---");
            w.newLine();
            for (Question q : questions) {
                w.write("Q" + SEP + q.getNumber() + SEP
                        + q.getText().replace("\n", " ") + SEP
                        + q.getAnswer().replace("\n", " "));
                w.newLine();
            }
        }
        return file;
    }

    /** Load one submission from a file. */
    public static Submission load(File file) throws IOException {
        String student = "Unknown";
        String exam = "Unknown";
        String time = "";
        List<Question> questions = new ArrayList<>();
        boolean headerDone = false;

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!headerDone) {
                    if (line.equals("---")) {
                        headerDone = true;
                        continue;
                    }
                    String[] p = line.split(SEP, 2);
                    if (p.length < 2)
                        continue;
                    switch (p[0]) {
                        case "STUDENT":
                            student = p[1];
                            break;
                        case "EXAM":
                            exam = p[1];
                            break;
                        case "TIME":
                            time = p[1];
                            break;
                    }
                } else {
                    String[] p = line.split(SEP, 4);
                    if (p.length < 4)
                        continue;
                    Question q = new Question(Integer.parseInt(p[1]), p[2]);
                    q.setAnswer(p[3]);
                    questions.add(q);
                }
            }
        }
        return new Submission(student, exam, time, file, questions);
    }

    /** List every saved submission, newest first. */
    public static List<Submission> listSubmissions() {
        List<Submission> list = new ArrayList<>();
        File dir = new File(DIR);
        if (!dir.exists() || dir.listFiles() == null)
            return list;

        File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
        if (files == null)
            return list;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        for (File f : files) {
            try {
                list.add(load(f));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    /** Save a blank exam (questions only, no answers). */
    public static File saveTemplate(String examTitle, List<Question> questions)
            throws IOException {
        Files.createDirectories(Path.of(TEMPLATE_DIR));
        String safe = examTitle.replaceAll("[^a-zA-Z0-9_-]", "_");
        File file = new File(TEMPLATE_DIR, safe + ".txt");

        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write("EXAM" + SEP + examTitle);
            w.newLine();
            w.write("---");
            w.newLine();
            for (Question q : questions) {
                w.write("Q" + SEP + q.getNumber() + SEP
                        + q.getText().replace("\n", " "));
                w.newLine();
            }
        }
        return file;
    }

    /** List all saved exam templates. */
    public static List<String> listTemplates() {
        List<String> list = new ArrayList<>();
        File dir = new File(TEMPLATE_DIR);
        if (!dir.exists())
            return list;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
        if (files == null)
            return list;
        for (File f : files) {
            list.add(f.getName().replaceAll("\\.txt$", ""));
        }
        return list;
    }

    /** Load a saved exam template by name (without extension). */
    public static List<Question> loadTemplate(String examName) throws IOException {
        File file = new File(TEMPLATE_DIR, examName + ".txt");
        if (!file.exists())
            return List.of();

        List<Question> questions = new ArrayList<>();
        boolean headerDone = false;

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!headerDone) {
                    if (line.equals("---")) {
                        headerDone = true;
                        continue;
                    }
                    continue;
                }
                String[] p = line.split(SEP, 3);
                if (p.length < 3)
                    continue;
                questions.add(new Question(Integer.parseInt(p[1]), p[2]));
            }
        }
        return questions;
    }
}