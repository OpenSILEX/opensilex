package org.opensilex.faidare.model;

public class Faidarev1StudySummaryDTO {
    private String locationDbId;
    private String studyDbId;
    private String locationName;
    private String studyName;

    public String getLocationDbId() {
        return locationDbId;
    }

    public Faidarev1StudySummaryDTO setLocationDbId(String locationDbId) {
        this.locationDbId = locationDbId;
        return this;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public Faidarev1StudySummaryDTO setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
        return this;
    }

    public String getLocationName() {
        return locationName;
    }

    public Faidarev1StudySummaryDTO setLocationName(String locationName) {
        this.locationName = locationName;
        return this;
    }

    public String getStudyName() {
        return studyName;
    }

    public Faidarev1StudySummaryDTO setStudyName(String studyName) {
        this.studyName = studyName;
        return this;
    }
}
