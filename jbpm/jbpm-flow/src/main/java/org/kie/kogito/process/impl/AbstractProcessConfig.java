/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.process.impl;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.kie.api.event.process.ProcessEventListener;
import org.kie.kogito.Addons;
import org.kie.kogito.auth.IdentityProvider;
import org.kie.kogito.calendar.BusinessCalendar;
import org.kie.kogito.event.EventPublisher;
import org.kie.kogito.jobs.JobsService;
import org.kie.kogito.process.ProcessConfig;
import org.kie.kogito.process.ProcessEventListenerConfig;
import org.kie.kogito.process.ProcessVersionResolver;
import org.kie.kogito.process.WorkItemHandlerConfig;
import org.kie.kogito.services.identity.NoOpIdentityProvider;
import org.kie.kogito.services.jobs.impl.StaticJobService;
import org.kie.kogito.services.signal.DefaultSignalManagerHub;
import org.kie.kogito.services.uow.CollectingUnitOfWorkFactory;
import org.kie.kogito.services.uow.DefaultUnitOfWorkManager;
import org.kie.kogito.signal.SignalManagerHub;
import org.kie.kogito.uow.UnitOfWorkManager;
import org.kie.kogito.uow.events.UnitOfWorkEventListener;

public abstract class AbstractProcessConfig implements ProcessConfig {

    private final WorkItemHandlerConfig workItemHandlerConfig;
    private final SignalManagerHub signalManagerHub = new DefaultSignalManagerHub();
    private final ProcessEventListenerConfig processEventListenerConfig;
    private final UnitOfWorkManager unitOfWorkManager;
    private final JobsService jobsService;
    private final ProcessVersionResolver versionResolver;
    private final IdentityProvider identityProvider;
    private final BusinessCalendar businessCalendar;

    protected AbstractProcessConfig(
            Iterable<WorkItemHandlerConfig> workItemHandlerConfig,
            Iterable<ProcessEventListenerConfig> processEventListenerConfigs,
            Iterable<ProcessEventListener> processEventListeners,
            Iterable<UnitOfWorkManager> unitOfWorkManager,
            Iterable<JobsService> jobsService,
            Iterable<EventPublisher> eventPublishers,
            String kogitoService,
            Iterable<UnitOfWorkEventListener> unitOfWorkListeners,
            Iterable<ProcessVersionResolver> versionResolver,
            Iterable<IdentityProvider> identityProvider,
            Iterable<BusinessCalendar> businessCalendar) {

        this.workItemHandlerConfig = mergeWorkItemHandler(workItemHandlerConfig, DefaultWorkItemHandlerConfig::new);
        this.processEventListenerConfig = merge(processEventListenerConfigs, processEventListeners);
        this.unitOfWorkManager = orDefault(unitOfWorkManager, () -> new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()));
        this.jobsService = orDefault(jobsService, StaticJobService::staticJobService);
        this.versionResolver = orDefault(versionResolver, () -> null);
        this.identityProvider = orDefault(identityProvider, NoOpIdentityProvider::new);
        this.businessCalendar = orDefault(businessCalendar, () -> null);

        eventPublishers.forEach(publisher -> unitOfWorkManager().eventManager().addPublisher(publisher));
        unitOfWorkListeners.forEach(listener -> unitOfWorkManager().register(listener));
        unitOfWorkManager().eventManager().setService(kogitoService);
    }

    private static WorkItemHandlerConfig mergeWorkItemHandler(Iterable<WorkItemHandlerConfig> workItemHandlerConfigs,
            Supplier<WorkItemHandlerConfig> supplier) {
        Iterator<WorkItemHandlerConfig> iterator = workItemHandlerConfigs.iterator();
        if (iterator.hasNext()) {
            WorkItemHandlerConfig config = iterator.next();
            return iterator.hasNext() ? new MultiWorkItemHandlerConfig(workItemHandlerConfigs) : config;
        } else {
            return supplier.get();
        }
    }

    @Override
    public WorkItemHandlerConfig workItemHandlers() {
        return workItemHandlerConfig;
    }

    @Override
    public ProcessEventListenerConfig processEventListeners() {
        return processEventListenerConfig;
    }

    @Override
    public SignalManagerHub signalManagerHub() {
        return signalManagerHub;
    }

    @Override
    public UnitOfWorkManager unitOfWorkManager() {
        return unitOfWorkManager;
    }

    @Override
    public JobsService jobsService() {
        return jobsService;
    }

    @Override
    public ProcessVersionResolver versionResolver() {
        return versionResolver;
    }

    @Override
    public IdentityProvider identityProvider() {
        return identityProvider;
    }

    @Override
    public BusinessCalendar getBusinessCalendar() {
        return this.businessCalendar;
    }

    public org.kie.kogito.Addons addons() {
        return Addons.EMTPY;
    }

    static <T> T orDefault(Iterable<T> instance, Supplier<? extends T> supplier) {
        Iterator<T> iterator = instance.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return supplier.get();
        }
    }

    static ProcessEventListenerConfig merge(
            Iterable<ProcessEventListenerConfig> processEventListenerConfigs,
            Iterable<ProcessEventListener> processEventListeners) {
        List<ProcessEventListenerConfig> l1 = StreamSupport.stream(processEventListenerConfigs.spliterator(), false).collect(Collectors.toList());
        List<ProcessEventListener> l2 = StreamSupport.stream(processEventListeners.spliterator(), false).collect(Collectors.toList());

        Stream<ProcessEventListener> processEventListenerStream = l1.stream().flatMap(c -> c.listeners().stream());
        Stream<ProcessEventListener> eventListenerStream = l2.stream();

        return new CachedProcessEventListenerConfig(
                Stream.concat(processEventListenerStream, eventListenerStream)
                        .collect(Collectors.toList()));
    }
}
