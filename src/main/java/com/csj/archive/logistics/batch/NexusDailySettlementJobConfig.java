package com.csj.archive.logistics.batch;

import com.csj.archive.logistics.settlement.NexusDailySettlementService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
public class NexusDailySettlementJobConfig {
    @Bean
    public Job nexusDailySettlementJob(JobRepository jobRepository, Step nexusDailySettlementStep) {
        return new JobBuilder("nexusDailySettlementJob", jobRepository)
                .start(nexusDailySettlementStep)
                .build();
    }

    @Bean
    public Step nexusDailySettlementStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         NexusDailySettlementService settlementService) {
        return new StepBuilder("nexusDailySettlementStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String date = (String) chunkContext.getStepContext().getJobParameters().get("settlementDate");
                    String factoryId = (String) chunkContext.getStepContext().getJobParameters().get("factoryId");
                    settlementService.run(date == null || date.isBlank() ? null : LocalDate.parse(date),
                            factoryId == null || factoryId.isBlank() ? null : factoryId);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
