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
package org.kie.kogito.persistence.jdbc;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

abstract class Repository {

    static final String INSERT = "INSERT INTO process_instances (id, payload, process_id, process_version, version) VALUES (?, ?, ?, ?, ?)";
    static final String INSERT_BUSINESS_KEY = "INSERT INTO business_key_mapping (business_key,process_instance_id) VALUES (?,?)";
    static final String FIND_ALL = "SELECT payload, version FROM process_instances WHERE process_id = ?";
    static final String FIND_BY_ID = "SELECT payload, version FROM process_instances WHERE process_id = ? and id = ?";
    static final String FIND_BY_BUSINESS_KEY = "SELECT payload, version FROM process_instances INNER JOIN business_key_mapping ON id = process_instance_id WHERE business_key = ? and process_id = ?";
    static final String UPDATE = "UPDATE process_instances SET payload = ? WHERE process_id = ? and id = ?";
    static final String UPDATE_WITH_LOCK = "UPDATE process_instances SET payload = ?, version = ? WHERE process_id = ? and id = ? and version = ?";
    static final String DELETE = "DELETE FROM process_instances WHERE process_id = ? and id = ?";
    static final String PROCESS_VERSION_EQUALS_TO = "and process_version = ?";
    static final String PROCESS_VERSION_IS_NULL = "and process_version is null";
    static final String MIGRATE_BULK = "UPDATE process_instances SET process_id = ?, process_version = ? WHERE process_id = ? ";
    static final String MIGRATE_INSTANCE = "UPDATE process_instances SET process_id = ?, process_version = ? WHERE id = ANY (?) and process_id = ? ";
    static final String FIND_ALL_WAITING_FOR_EVENT_TYPE =
            "SELECT payload, version FROM event_types, process_instances WHERE process_instances.id = event_types.process_instance_id AND process_id = ? AND event_type = ?";
    static final String DELETE_ALL_WAITING_FOR_EVENT_TYPE = "DELETE FROM event_types WHERE process_instance_id = ?";
    static final String INSERT_WAITING_FOR_EVENT_TYPE = "INSERT INTO event_types (process_instance_id, event_type) VALUES(?,?)";

    static class Record {
        private final byte[] payload;
        private final long version;

        public byte[] getPayload() {
            return payload;
        }

        public long getVersion() {
            return version;
        }

        public Record(byte[] payload, long version) {
            this.payload = payload;
            this.version = version;
        }
    }

    abstract void insertInternal(String processId, String processVersion, UUID id, byte[] payload, String businessKey, String[] eventTypes);

    abstract void updateInternal(String processId, String processVersion, UUID id, byte[] payload, String[] eventTypes);

    abstract boolean updateWithLock(String processId, String processVersion, UUID id, byte[] payload, long version, String[] eventTypes);

    abstract boolean deleteInternal(String processId, String processVersion, UUID id);

    abstract Optional<Record> findByIdInternal(String processId, String processVersion, UUID id);

    abstract Optional<Record> findByBusinessKey(String processId, String processVersion, String businessKey);

    abstract Stream<Record> findAllInternal(String processId, String processVersion);

    abstract Stream<Record> findAllInternalWaitingFor(String id, String version, String eventType);

    protected RuntimeException uncheckedException(Exception ex, String message, Object... param) {
        return new RuntimeException(String.format(message, param), ex);
    }

    abstract long migrate(String id, String version, String targetProcessId, String targetProcessVersion);

    abstract void migrate(String id, String version, String targetProcessId, String targetProcessVersion, String[] processIds);

}
