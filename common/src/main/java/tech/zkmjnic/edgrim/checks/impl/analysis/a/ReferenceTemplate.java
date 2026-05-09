package tech.zkmjnic.edgrim.checks.impl.analysis.a;

record ReferenceTemplate(String referenceName, @SuppressWarnings("unused") String referenceType,
                         WindowSignature signature) {
    ReferenceTemplate(String referenceName, String referenceType, WindowSignature signature) {
        this.referenceName = referenceName;
        this.referenceType = referenceType;
        this.signature = signature;
    }
}
