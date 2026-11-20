package com.library.jobs;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled. Each job also checks app.jobs.enabled, so jobs can be switched off without code changes. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
