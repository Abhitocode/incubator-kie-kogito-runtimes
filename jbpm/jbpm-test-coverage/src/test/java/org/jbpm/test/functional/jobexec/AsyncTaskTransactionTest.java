/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.test.functional.jobexec;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.assertj.core.api.Assertions;
import org.jbpm.executor.impl.wih.AsyncWorkItemHandler;
import org.jbpm.test.JbpmAsyncJobTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.runtime.query.QueryContext;

public class AsyncTaskTransactionTest extends JbpmAsyncJobTestCase {

    public static final String ASYNC_DATA_EXECUTOR = "org/jbpm/test/functional/jobexec/AsyncDataExecutor.bpmn2";
    public static final String ASYNC_DATA_EXECUTOR_ID = "org.jbpm.test.functional.jobexec.AsyncDataExecutor";

    public static final String ASYNC_EXECUTOR_2 = "org/jbpm/test/functional/jobexec/AsyncExecutor2.bpmn2";
    public static final String ASYNC_EXECUTOR_2_ID = "org.jbpm.test.functional.jobexec.AsyncExecutor2";

    public static final String USER_COMMAND = "org.jbpm.test.jobexec.UserCommand";
    public static final String USER_FAILING_COMMAND = "org.jbpm.test.jobexec.UserFailingCommand";

    @Test
    public void testJobCommitInAsyncExec() throws Exception {
        KieSession ksession = registerAsyncHandler(createKSession(ASYNC_EXECUTOR_2, ASYNC_DATA_EXECUTOR));

        UserTransaction ut = getUserTransaction();
        ut.begin();

        Map<String, Object> pm = new HashMap<String, Object>();
        pm.put("_command", USER_COMMAND);
        ProcessInstance pi = ksession.startProcess(ASYNC_EXECUTOR_2_ID, pm);

        ut.commit();

        assertProcessInstanceCompleted(pi.getId());

        Thread.sleep(4 * 1000);
        Assertions.assertThat(getExecutorService().getCompletedRequests(new QueryContext())).hasSize(1);
    }

    @Test
    public void testJobRollbackInAsyncExec() throws Exception {
        KieSession ksession = registerAsyncHandler(createKSession(ASYNC_EXECUTOR_2, ASYNC_DATA_EXECUTOR));

        UserTransaction ut = getUserTransaction();
        ut.begin();

        Map<String, Object> pm = new HashMap<String, Object>();
        pm.put("_command", USER_COMMAND);
        ProcessInstance pi = ksession.startProcess(ASYNC_EXECUTOR_2_ID, pm);

        assertProcessInstanceCompleted(pi.getId());

        ut.rollback();

        assertProcessInstanceNeverRun(pi.getId());
        Assertions.assertThat(getExecutorService().getCompletedRequests(new QueryContext())).hasSize(0);
    }

    @Test
    public void testJobCommit() throws Exception {
        KieSession ksession = registerAsyncHandler(createKSession(ASYNC_DATA_EXECUTOR));

        UserTransaction ut = getUserTransaction();
        ut.begin();

        Map<String, Object> pm = new HashMap<String, Object>();
        pm.put("command", USER_COMMAND);
        ProcessInstance pi = ksession.startProcess(ASYNC_DATA_EXECUTOR_ID, pm);
        // the JobExecutor will act on the job only after commit
        ut.commit();

        Thread.sleep(4 * 1000);
        assertProcessInstanceCompleted(pi.getId());
    }

    @Test
    public void testJobRollback() throws Exception {
        KieSession ksession = registerAsyncHandler(createKSession(ASYNC_DATA_EXECUTOR));

        UserTransaction ut = getUserTransaction();
        ut.begin();

        Map<String, Object> pm = new HashMap<String, Object>();
        pm.put("_command", USER_FAILING_COMMAND);
        ProcessInstance pi = ksession.startProcess(ASYNC_DATA_EXECUTOR_ID, pm);

        ut.rollback();

        assertProcessInstanceNeverRun(pi.getId());
    }

    private UserTransaction getUserTransaction() throws Exception {
        return InitialContext.doLookup("java:comp/UserTransaction");
    }

    private KieSession registerAsyncHandler(KieSession ksession) {
        WorkItemManager wim = ksession.getWorkItemManager();
        wim.registerWorkItemHandler("async", new AsyncWorkItemHandler(getExecutorService()));

        return ksession;
    }

}
