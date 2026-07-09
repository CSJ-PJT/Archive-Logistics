package com.csj.archive.logistics.batch;

import com.csj.archive.logistics.outbox.OutboxService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OutboxRetryJobConfig {
    @Bean
    public Job outboxRetryJob(JobRepository jobRepository, Step outboxRetryStep) {
        return new JobBuilder("outboxRetryJob", jobRepository)
                .start(outboxRetryStep)
                .build();
    }

    @Bean
    public Step outboxRetryStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                OutboxService outboxService) {
        return new StepBuilder("outboxRetryStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    outboxService.retryFailed();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
