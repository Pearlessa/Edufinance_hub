/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.util;

import java.util.List;

public class DnaCalculator {

    public static double computeAcademicAverage(List<Double> marks) {
        if (marks == null || marks.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double m : marks) {
            if (m == null || m < 0.0 || m > 100.0) {
                throw new IllegalArgumentException("Subject marks must strictly range between 0% and 100%.");
            }
            sum += m;
        }
        return Math.round((sum / marks.size()) * 10.0) / 10.0;
    }

    public static int calculateDnaScore(double average, String incomeBracket) {
        int score = 40; // Baseline entry score

        // Merit Weighting (Up to 35 Points)
        if (average >= 80.0) {
            score += 35;
        } else if (average >= 70.0) {
            score += 25;
        } else if (average >= 60.0) {
            score += 15;
        } else if (average >= 50.0) {
            score += 8;
        }

        // Financial Need Weighting (Up to 24 Points) - driven by the
        // student's actual selected household income bracket, not a
        // fixed placeholder figure.
        if ("UNDER R85K".equalsIgnoreCase(incomeBracket) || "R86K - R 349K".equalsIgnoreCase(incomeBracket)) {
            score += 24;
        } else if ("R350K - R600K".equalsIgnoreCase(incomeBracket)) {
            score += 14;
        } else {
            score += 5;
        }

        return Math.min(score, 99);
    }
}