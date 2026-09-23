package com.example.services;

import com.example.models.Question;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.util.List;

public class PdfWriterService {

    public void writeAnswersPdf(List<Question> questions, File output)
            throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs.beginText();
            cs.newLineAtOffset(50, 750);
            cs.showText("Exam Answers");
            cs.endText();

            float y = 710;

            for (Question q : questions) {
                if (y < 100) {
                    cs.close();
                    page = new PDPage();
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = 750;
                }

                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText("Q" + q.getNumber() + ": " + truncate(q.getText(), 80));
                cs.endText();
                y -= 20;

                cs.setFont(PDType1Font.HELVETICA, 12);
                for (String line : wrap(q.getAnswer(), 90)) {
                    cs.beginText();
                    cs.newLineAtOffset(70, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 16;
                }
                y -= 20;
            }

            cs.close();
            doc.save(output);
        }
    }

    private String truncate(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("(no answer)");
            return lines;
        }
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                if (line.length() + word.length() + 1 > width) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
                if (line.length() > 0)
                    line.append(" ");
                line.append(word);
            }
            if (line.length() > 0)
                lines.add(line.toString());
        }
        return lines;
    }
}