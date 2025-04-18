/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.authorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestUser {

    @Test
    public void testSimpleCreation() {
        final String identifier = "1";
        final String identity = "user1";

        final User user = new User.Builder()
                .identifier(identifier)
                .identity(identity)
                .build();

        assertEquals(identifier, user.getIdentifier());
        assertEquals(identity, user.getIdentity());
    }

    @Test
    public void testMissingIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new User.Builder()
                .identity("user1")
                .build());
    }

    @Test
    public void testMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new User.Builder()
                .identifier("1")
                .build());
    }

    @Test
    public void testFromUser() {
        final String identifier = "1";
        final String identity = "user1";

        final User user = new User.Builder()
                .identifier(identifier)
                .identity(identity)
                .build();

        assertEquals(identifier, user.getIdentifier());
        assertEquals(identity, user.getIdentity());

        final User user2 = new User.Builder(user).build();
        assertEquals(user.getIdentifier(), user2.getIdentifier());
        assertEquals(user.getIdentity(), user2.getIdentity());
    }

    @Test
    public void testFromUserAndChangeIdentifier() {
        final User user = new User.Builder()
                .identifier("1")
                .identity("user1")
                .build();

        assertThrows(IllegalStateException.class, () -> new User.Builder(user).identifier("2").build());
    }

}
