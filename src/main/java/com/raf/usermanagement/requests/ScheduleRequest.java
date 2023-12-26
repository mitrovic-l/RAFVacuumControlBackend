package com.raf.usermanagement.requests;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleRequest {
    private Long vacuumId;
    private LocalDateTime scheduledTime;
    private VacuumOperation operation;
    public enum VacuumOperation{
        START, STOP, DISCHARGE
    }
}
