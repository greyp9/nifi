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
package org.apache.nifi.controller;

import org.apache.nifi.controller.repository.FlowFileSwapManager;
import org.apache.nifi.util.NiFiProperties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * <p>
 * An implementation of the {@link FlowFileSwapManager} that swaps FlowFiles
 * to/from local disk.  The swap file is encrypted using AES/CTR, using the
 * encryption key defined in nifi.properties.
 * </p>
 */
public class FileSystemSwapManagerEncrypt extends FileSystemSwapManager {

    private final SecretKey secretKey;

    /**
     * Default no args constructor for service loading only.
     */
    public FileSystemSwapManagerEncrypt() {
        throw new IllegalStateException("ctor not supported; nifi properties not available");
    }

    public FileSystemSwapManagerEncrypt(final NiFiProperties nifiProperties) {
        super(nifiProperties);
        final byte[] key = new byte[SIZE_KEY_AES_256];
        Arrays.fill(key, (byte) 0);
        secretKey = new SecretKeySpec(key, ALGORITHM);
    }

    public FileSystemSwapManagerEncrypt(final Path flowFileRepoPath) {
        throw new IllegalStateException("ctor not supported; nifi properties not available");
    }

    protected InputStream getInputStream(final File file) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        try {
            final byte[] iv = new byte[SIZE_IV_AES];
            final int countIV = fis.read(iv);
            if (countIV != SIZE_IV_AES) {
                throw new IOException("problem reading IV:" + countIV);
            }
            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new CipherInputStream(fis, cipher);
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    protected OutputStream getOutputStream(final File file) throws IOException {
        final byte[] iv = new byte[SIZE_IV_AES];
        new SecureRandom().nextBytes(iv);
        final FileOutputStream fos = new FileOutputStream(file);
        fos.write(iv);
        try {
            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new CipherOutputStream(fos, cipher);
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CTR/NoPadding";
    private static final int SIZE_KEY_AES_256 = 32;
    private static final int SIZE_IV_AES = 16;
}
