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
package org.apache.nifi.processors.greyp;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptContentCustom extends AbstractProcessor {

    private Key key;

    public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder()
            .name("success")
            .build();

    public static final Relationship RELATIONSHIP_FAILURE = new Relationship.Builder()
            .name("failure")
            .build();

    private static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(RELATIONSHIP_SUCCESS, RELATIONSHIP_FAILURE)));

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

    @OnScheduled
    public void onScheduled() {
        this.key = new SecretKeySpec(CryptCustom.AES_256_KEY, CryptCustom.ALGORITHM);
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        try {
            // read the plaintext flowfile bytes
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (final InputStream inputStream = session.read(flowFile)) {
                bos.write(CryptCustom.read(new BufferedInputStream(inputStream)));
            }
            final byte[] bytesPlaintext = bos.toByteArray();
            // encrypt the flowfile bytes
            final IvParameterSpec ivSpec = new IvParameterSpec(CryptCustom.IV);
            final Cipher cipher = Cipher.getInstance(CryptCustom.TRANSFORM_CTR);
            cipher.init(Cipher.ENCRYPT_MODE, this.key, ivSpec);
            final byte[] bytesCiphertext = cipher.doFinal(bytesPlaintext);
            // emit the encrypted bytes to the success relationship
            try (final OutputStream os = session.write(flowFile)) {
                os.write(CryptCustom.IV);
                os.write(bytesCiphertext);
            }
            session.transfer(flowFile, RELATIONSHIP_SUCCESS);
            session.commit();
        } catch (final GeneralSecurityException | IOException e) {
            throw new ProcessException(e);
        }
    }
}
