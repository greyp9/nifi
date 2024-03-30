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

import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CryptCustom {
    /**
     * Hard coded AES key bytes.
     */
    public static final String AES_256_KEY_HEX = "0000000000000000000000000000000000000000000000000000000000000000";
    public static final byte[] AES_256_KEY = Hex.decode(AES_256_KEY_HEX);

    /**
     * Hard coded crypto initialization vector.
     */
    public static final String IV_HEX = "00000000000000000000000000000000";
    public static final byte[] IV = Hex.decode(IV_HEX);
    public static final int IV_LENGTH = IV.length;

    /**
     * Hard coded cryptography parameters.
     */
    public static final String ALGORITHM = "AES";
    public static final String TRANSFORM_CTR = "AES/CTR/NoPadding";

    /**
     * Utility method for reading all bytes from byte stream.
     */
    public static byte[] read(final BufferedInputStream is) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        int b;
        while ((b = is.read()) >= 0) {
            os.write(b);
        }
        return os.toByteArray();
    }
}
