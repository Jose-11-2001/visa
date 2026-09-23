package com.example.services;

import com.example.models.Question;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads exam answers as a plain text file
 * (question number | question text | answer).
 */
public class ExamStorage {

    private static final String FILE = "D:/Projects/visa/exam_answers.txt";

    public static void save(List<Question> questions) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE))) {
            for (Question q : questions) {
                w.write(q.getNumber() + "\t" +
                        q.getText().replace("\n", " ") + "\t" +
                        q.getAnswer().replace("\n", " "));
                w.newLine();
            }
        }
    }

    public static List<Question> load() throws IOException {
        File f = new File(FILE);
        if (!f.exists())
            return new ArrayList<>();

        List<Question> list = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split("\t", 3);
                if (parts.length < 3)
                    continue;
                Question q = new Question(Integer.parseInt(parts[0]), parts[1]);
                q.setAnswer(parts[2]);
                list.add(q);
            }
        }
        return list;
    }
}