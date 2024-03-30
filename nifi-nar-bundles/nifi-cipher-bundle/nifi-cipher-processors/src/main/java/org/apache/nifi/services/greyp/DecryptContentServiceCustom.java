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
package org.apache.nifi.services.greyp;

import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processors.greyp.CryptCustom;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.serialization.MalformedRecordException;
import org.apache.nifi.serialization.RecordReader;
import org.apache.nifi.serialization.RecordReaderFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptContentServiceCustom extends AbstractControllerService implements RecordReaderFactory {

    public static final PropertyDescriptor RECORD_READER = new PropertyDescriptor.Builder()
            .name("record-reader")
            .identifiesControllerService(RecordReaderFactory.class)
            .build();

    private Key key;

    private RecordReaderFactory readerFactory;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(RECORD_READER);
        return properties;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {
        this.key = new SecretKeySpec(CryptCustom.AES_256_KEY, CryptCustom.ALGORITHM);
        readerFactory = context.getProperty(RECORD_READER).asControllerService(RecordReaderFactory.class);
    }

    @Override
    public RecordReader createRecordReader(
            FlowFile flowFile, InputStream in, ComponentLog logger)
            throws MalformedRecordException, IOException, SchemaNotFoundException {
        throw new IOException("NOT IMPLEMENTED");
    }

    @Override
    public RecordReader createRecordReader(
            Map<String, String> variables, InputStream in, long inputLength, ComponentLog logger)
            throws MalformedRecordException, IOException, SchemaNotFoundException {
        // read the ciphertext flowfile bytes
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(CryptCustom.read(new BufferedInputStream(in)));
        final byte[] bytesCiphertext = bos.toByteArray();
        // decrypt the flowfile bytes
        final int lengthCiphertext = bytesCiphertext.length - CryptCustom.IV_LENGTH;
        final IvParameterSpec ivSpec = new IvParameterSpec(bytesCiphertext, 0, CryptCustom.IV_LENGTH);
        try {
            final Cipher cipher = Cipher.getInstance(CryptCustom.TRANSFORM_CTR);
            cipher.init(Cipher.DECRYPT_MODE, this.key, ivSpec);
            final byte[] bytesPlaintext = cipher.doFinal(bytesCiphertext, CryptCustom.IV_LENGTH, lengthCiphertext);
            // forward the plaintext bytes to the chained record reader
            return readerFactory.createRecordReader(variables, new ByteArrayInputStream(bytesPlaintext), bytesPlaintext.length, logger);
        } catch (final GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
}
