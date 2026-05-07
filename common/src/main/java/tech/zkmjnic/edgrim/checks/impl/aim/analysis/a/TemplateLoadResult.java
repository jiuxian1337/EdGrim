package tech.zkmjnic.edgrim.checks.impl.aim.analysis.a;

import java.util.List;

final class TemplateLoadResult {
    final List<ReferenceTemplate> templates;
    final int fileCount;

    TemplateLoadResult(List<ReferenceTemplate> templates, int fileCount) {
        this.templates = templates;
        this.fileCount = fileCount;
    }
}
