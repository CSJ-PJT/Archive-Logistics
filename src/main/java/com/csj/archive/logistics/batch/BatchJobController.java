package com.csj.archive.logistics.batch;

import com.csj.archive.logistics.common.ApiResponse;
import com.csj.archive.logistics.common.NotFoundException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/batch")
public class BatchJobController {
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job outboxPublishJob;
    private final Job nexusDailySettlementJob;

    public BatchJobController(JobLauncher jobLauncher,
                              JobExplorer jobExplorer,
                              @Qualifier("outboxPublishJob") Job outboxPublishJob,
                              @Qualifier("nexusDailySettlementJob") Job nexusDailySettlementJob) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.outboxPublishJob = outboxPublishJob;
        this.nexusDailySettlementJob = nexusDailySettlementJob;
    }

    @PostMapping("/outbox-publish/run")
    public ApiResponse<Map<String, Object>> runOutboxPublishJob() throws Exception {
        JobExecution execution = jobLauncher.run(outboxPublishJob, new JobParametersBuilder()
                .addString("requestId", UUID.randomUUID().toString())
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters());
        return ApiResponse.ok(execution(execution));
    }

    @PostMapping("/nexus-daily-settlement/run")
    public ApiResponse<Map<String, Object>> runNexusDailySettlementJob(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String factoryId) throws Exception {
        JobParametersBuilder parameters = new JobParametersBuilder()
                .addString("requestId", UUID.randomUUID().toString())
                .addLong("requestedAt", System.currentTimeMillis());
        if (date != null) {
            parameters.addString("settlementDate", date.toString());
        }
        if (factoryId != null && !factoryId.isBlank()) {
            parameters.addString("factoryId", factoryId);
        }
        JobExecution execution = jobLauncher.run(nexusDailySettlementJob, parameters.toJobParameters());
        return ApiResponse.ok(execution(execution));
    }

    @GetMapping("/jobs")
    public ApiResponse<List<Map<String, Object>>> jobs() {
        List<Map<String, Object>> executions = jobExplorer.getJobNames()
                .stream()
                .flatMap(jobName -> jobExplorer.getJobInstances(jobName, 0, 10).stream())
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .sorted(Comparator.comparing(JobExecution::getCreateTime).reversed())
                .limit(50)
                .map(this::execution)
                .toList();
        return ApiResponse.ok(executions);
    }

    @GetMapping("/jobs/{executionId}")
    public ApiResponse<Map<String, Object>> job(@PathVariable Long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            throw new NotFoundException("Batch job execution not found: " + executionId);
        }
        return ApiResponse.ok(execution(execution));
    }

    private Map<String, Object> execution(JobExecution execution) {
        JobInstance instance = execution.getJobInstance();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("executionId", execution.getId());
        response.put("jobName", instance == null ? "unknown" : instance.getJobName());
        response.put("status", execution.getStatus().name());
        response.put("exitCode", execution.getExitStatus().getExitCode());
        response.put("createTime", execution.getCreateTime());
        response.put("startTime", execution.getStartTime());
        response.put("endTime", execution.getEndTime());
        return response;
    }
}
