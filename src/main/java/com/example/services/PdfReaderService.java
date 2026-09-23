package com.example.services;

import com.example.models.Question;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfReaderService {

    public List<Question> extractQuestions(File pdfFile) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            String raw = new PDFTextStripper().getText(doc);
            return parseQuestions(raw);
        }
    }

    private List<Question> parseQuestions(String raw) {
        // Matches "1.", "Q1.", "Question 1:", etc.
        Pattern p = Pattern.compile(
                "(?m)^\\s*(?:Q(?:uestion)?\\s*)?(\\d+)[.):\\-]\\s+");
        Matcher m = p.matcher(raw);

        List<Question> questions = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();

        while (m.find()) {
            starts.add(m.end());
            numbers.add(Integer.parseInt(m.group(1)));
        }

        if (starts.isEmpty()) {
            questions.add(new Question(1, raw.trim()));
            return questions;
        }

        for (int i = 0; i < starts.size(); i++) {
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : raw.length();
            String text = raw.substring(starts.get(i), end).trim();
            questions.add(new Question(numbers.get(i), text));
        }
        return questions;
    }
}