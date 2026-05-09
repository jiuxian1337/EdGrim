package tech.zkmjnic.edgrim.checks.impl.analysis.a;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.checks.impl.analysis.AnalysisA;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ReferenceLibrary {
    private final List<ReferenceTemplate> legitTemplates;
    private final List<ReferenceTemplate> cheatTemplates;

    ReferenceLibrary(List<ReferenceTemplate> legitTemplates, List<ReferenceTemplate> cheatTemplates) {
        this.legitTemplates = legitTemplates;
        this.cheatTemplates = cheatTemplates;
    }

    public static ReferenceLibrary load(int sampleSize) {
        final TemplateLoadResult legitLoadResult = loadTemplates(AnalysisA.LEGIT_DIR, sampleSize, "legit");
        final TemplateLoadResult cheatLoadResult = loadTemplates(AnalysisA.CHEAT_DIR, sampleSize, "cheat");
        locateDirectory(AnalysisA.RECORDED_DIR);

        LogUtil.info(String.format(
                "[AnalysisA] loaded legit files=%d templates=%d, cheat files=%d templates=%d",
                legitLoadResult.fileCount(),
                legitLoadResult.templates().size(),
                cheatLoadResult.fileCount(),
                cheatLoadResult.templates().size()
        ));
        return new ReferenceLibrary(legitLoadResult.templates(), cheatLoadResult.templates());
    }

    private static TemplateLoadResult loadTemplates(String directoryName, int sampleSize, String type) {
        final File directory = locateDirectory(directoryName);
        if (directory == null || !directory.isDirectory()) {
            return new TemplateLoadResult(Collections.emptyList(), 0);
        }

        final File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null || files.length == 0) {
            return new TemplateLoadResult(Collections.emptyList(), 0);
        }

        final List<ReferenceTemplate> templates = new ArrayList<>();
        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    try {
                        final RecordedAttackSample sample = AnalysisA.GSON.fromJson(trimmed, RecordedAttackSample.class);
                        if (sample == null || sample.rotations() == null || sample.rotations().size() != sampleSize) {
                            continue;
                        }

                        final WindowSignature signature = WindowSignature.fromRotations(sample.rotations(), sampleSize / 2);
                        templates.add(new ReferenceTemplate(file.getName(), type, signature));
                    } catch (Throwable ignored) {
                    }
                }
            } catch (IOException ignored) {
            }
        }

        return new TemplateLoadResult(templates, files.length);
    }

    public static File locateDirectory(String childName) {
        final File dataFolder = EdGrimAPI.INSTANCE.getGrimPlugin().getDataFolder();
        final File root = new File(dataFolder, AnalysisA.ROOT_DIR);
        if (!root.exists()) {
            root.mkdirs();
        }

        final File directory = new File(root, childName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private static double averageDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum / a.length;
    }

    private static double averageFlipDistance(int[] a, int[] b) {
        if (a.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum / a.length;
    }

    private static double averageSignDistance(int[] a, int[] b) {
        if (a.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]) / 2.0;
        }
        return sum / a.length;
    }

    private static double ratioDistance(double first, double second) {
        return Math.abs(first - second) / Math.max(Math.max(first, second), 1.0E-6);
    }

    public boolean hasTemplates() {
        return !legitTemplates.isEmpty() && !cheatTemplates.isEmpty();
    }

    public ReferenceScore score(WindowSignature signature, boolean legit) {
        final List<ReferenceTemplate> templates = legit ? legitTemplates : cheatTemplates;
        if (templates.isEmpty()) {
            return ReferenceScore.empty();
        }

        final List<Double> similarities = new ArrayList<>(templates.size());
        String bestReferenceName = "";
        double bestSimilarity = 0.0;
        for (ReferenceTemplate template : templates) {
            final double similarity = similarity(signature, template.signature());
            similarities.add(similarity);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestReferenceName = template.referenceName();
            }
        }

        similarities.sort(Comparator.reverseOrder());
        final int topCount = Math.min(AnalysisA.TOP_MATCH_COUNT, similarities.size());
        double topAverage = 0.0;
        for (int i = 0; i < topCount; i++) {
            topAverage += similarities.get(i);
        }
        if (topCount > 0) {
            topAverage /= topCount;
        }
        return new ReferenceScore(bestReferenceName, bestSimilarity, topAverage);
    }

    private double similarity(WindowSignature a, WindowSignature b) {
        final double yawAbsDistance = averageDistance(a.yawAbsNorm, b.yawAbsNorm);
        final double pitchAbsDistance = averageDistance(a.pitchAbsNorm, b.pitchAbsNorm);
        final double energyDistance = averageDistance(a.energyNorm, b.energyNorm);
        final double yawFlipDistance = averageFlipDistance(a.yawFlip, b.yawFlip);
        final double pitchFlipDistance = averageFlipDistance(a.pitchFlip, b.pitchFlip);
        final double yawSignDistance = averageSignDistance(a.yawSign, b.yawSign);
        final double pitchSignDistance = averageSignDistance(a.pitchSign, b.pitchSign);
        final double totalEnergyRatio = ratioDistance(a.totalEnergy, b.totalEnergy);
        final double peakEnergyRatio = ratioDistance(a.peakEnergy, b.peakEnergy);
        final double consistencyDistance = Math.abs(a.dominantConsistency - b.dominantConsistency);
        final double centerBandDistance = Math.abs(a.centerBandRatio - b.centerBandRatio);

        final double distance =
                yawAbsDistance * 0.22
                        + pitchAbsDistance * 0.22
                        + energyDistance * 0.18
                        + yawSignDistance * 0.08
                        + pitchSignDistance * 0.08
                        + yawFlipDistance * 0.05
                        + pitchFlipDistance * 0.05
                        + totalEnergyRatio * 0.04
                        + peakEnergyRatio * 0.03
                        + consistencyDistance * 0.03
                        + centerBandDistance * 0.02;

        return Math.max(0.0, 1.0 - distance);
    }
}
