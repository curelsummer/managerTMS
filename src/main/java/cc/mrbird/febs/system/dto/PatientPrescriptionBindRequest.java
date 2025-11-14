package cc.mrbird.febs.system.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PatientPrescriptionBindRequest {
    @NotNull
    private Long patientId;
    @NotNull
    private Long prescriptionId;
}


