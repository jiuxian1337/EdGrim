package tech.zkmjnic.edgrim.checks.impl.aim.analysis.a;

final class ReferenceTemplate {
    final String referenceName;
    @SuppressWarnings("unused")
    final String referenceType;
    final WindowSignature signature;

    ReferenceTemplate(String referenceName, String referenceType, WindowSignature signature) {
        this.referenceName = referenceName;
        this.referenceType = referenceType;
        this.signature = signature;
    }
}
