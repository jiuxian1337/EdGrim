package tech.zkmjnic.edgrim.checks.impl.aim.analysis.a;

import java.util.List;

public final class WindowSignature {
    final double[] yawAbsNorm;
    final double[] pitchAbsNorm;
    final double[] energyNorm;
    final int[] yawSign;
    final int[] pitchSign;
    final int[] yawFlip;
    final int[] pitchFlip;
    public final double totalEnergy;
    final double peakEnergy;
    final double dominantConsistency;
    final double centerBandRatio;

    WindowSignature(
            double[] yawAbsNorm,
            double[] pitchAbsNorm,
            double[] energyNorm,
            int[] yawSign,
            int[] pitchSign,
            int[] yawFlip,
            int[] pitchFlip,
            double totalEnergy,
            double peakEnergy,
            double dominantConsistency,
            double centerBandRatio
    ) {
        this.yawAbsNorm = yawAbsNorm;
        this.pitchAbsNorm = pitchAbsNorm;
        this.energyNorm = energyNorm;
        this.yawSign = yawSign;
        this.pitchSign = pitchSign;
        this.yawFlip = yawFlip;
        this.pitchFlip = pitchFlip;
        this.totalEnergy = totalEnergy;
        this.peakEnergy = peakEnergy;
        this.dominantConsistency = dominantConsistency;
        this.centerBandRatio = centerBandRatio;
    }

    public static WindowSignature fromRotations(List<RecordedRotation> rotations, int centerIndex) {
        final int n = rotations.size();
        final double[] yawAbs = new double[n];
        final double[] pitchAbs = new double[n];
        final double[] energy = new double[n];
        final int[] yawSign = new int[n];
        final int[] pitchSign = new int[n];
        final int[] yawFlip = new int[Math.max(0, n - 1)];
        final int[] pitchFlip = new int[Math.max(0, n - 1)];

        double totalYawAbs = 0.0;
        double totalPitchAbs = 0.0;
        double totalEnergy = 0.0;
        double peakEnergy = 0.0;
        double signedYawSum = 0.0;
        double signedPitchSum = 0.0;

        for (int i = 0; i < n; i++) {
            final RecordedRotation rotation = rotations.get(i);
            yawAbs[i] = Math.abs(rotation.deltaYaw);
            pitchAbs[i] = Math.abs(rotation.deltaPitch);
            energy[i] = yawAbs[i] + pitchAbs[i];
            yawSign[i] = signOf(rotation.deltaYaw);
            pitchSign[i] = signOf(rotation.deltaPitch);
            peakEnergy = Math.max(peakEnergy, energy[i]);
            totalYawAbs += yawAbs[i];
            totalPitchAbs += pitchAbs[i];
            totalEnergy += energy[i];
            signedYawSum += rotation.deltaYaw;
            signedPitchSum += rotation.deltaPitch;
        }

        for (int i = 1; i < n; i++) {
            yawFlip[i - 1] = flipOf(yawSign[i - 1], yawSign[i]);
            pitchFlip[i - 1] = flipOf(pitchSign[i - 1], pitchSign[i]);
        }

        final double yawDenominator = Math.max(totalYawAbs, 1.0E-6);
        final double pitchDenominator = Math.max(totalPitchAbs, 1.0E-6);
        final double energyDenominator = Math.max(totalEnergy, 1.0E-6);
        final double dominantConsistency = Math.max(
                Math.abs(signedYawSum) / yawDenominator,
                Math.abs(signedPitchSum) / pitchDenominator
        );

        int centerStart = Math.max(0, centerIndex - 1);
        int centerEnd = Math.min(n - 1, centerIndex + 1);
        double centerBandEnergy = 0.0;
        for (int i = centerStart; i <= centerEnd; i++) {
            centerBandEnergy += energy[i];
        }

        final double[] yawAbsNorm = new double[n];
        final double[] pitchAbsNorm = new double[n];
        final double[] energyNorm = new double[n];
        for (int i = 0; i < n; i++) {
            yawAbsNorm[i] = yawAbs[i] / yawDenominator;
            pitchAbsNorm[i] = pitchAbs[i] / pitchDenominator;
            energyNorm[i] = energy[i] / energyDenominator;
        }

        return new WindowSignature(
                yawAbsNorm,
                pitchAbsNorm,
                energyNorm,
                yawSign,
                pitchSign,
                yawFlip,
                pitchFlip,
                totalEnergy,
                peakEnergy,
                dominantConsistency,
                centerBandEnergy / energyDenominator
        );
    }

    private static int signOf(float value) {
        if (value > 1.0E-6F) {
            return 1;
        }
        if (value < -1.0E-6F) {
            return -1;
        }
        return 0;
    }

    private static int flipOf(int previous, int current) {
        if (previous == 0 || current == 0 || previous == current) {
            return 0;
        }
        return 1;
    }
}
