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
package org.jbpm.compiler.canonical;

import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.ruleflow.core.factory.CatchLinkNodeFactory;
import org.jbpm.workflow.core.node.CatchLinkNode;

import com.github.javaparser.ast.stmt.BlockStmt;

public class CatchLinkNodeVisitor extends AbstractNodeVisitor<CatchLinkNode> {

    public CatchLinkNodeVisitor(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    protected String getNodeKey() {
        return "catchLinkNode";
    }

    @Override
    public void visitNode(String factoryField,
            CatchLinkNode node,
            BlockStmt body,
            VariableScope variableScope,
            ProcessMetaData metadata) {
        String nodeId = getNodeId(node);
        body.addStatement(getAssignedFactoryMethod(factoryField, CatchLinkNodeFactory.class, nodeId,
                getNodeKey(), getWorkflowElementConstructor(node.getId())));
        body.addStatement(getDoneMethod(nodeId));
        body.addStatement(getNameMethod(node, "CatchLink"));
    }

}
