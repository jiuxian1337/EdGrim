package tech.zkmjnic.edgrim.utils.ray;

import lombok.Data;

@Data
public class RayLine {

    private final double x;
    private final double z;

    public RayLine(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public double z() {
        return z;
    }
}
