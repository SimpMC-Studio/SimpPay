package org.simpmc.simppay.convert;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ImportResult {
    private int imported;
    private int skipped;
    private int failed;
    private final List<String> errors = new ArrayList<>();

    public void addImported() {
        imported++;
    }

    public void addSkipped() {
        skipped++;
    }

    public void addFailed(String error) {
        failed++;
        errors.add(error);
    }
}
