/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.commons.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * Authentication provider interface.
 */
public interface AuthenticationProvider extends Serializable {
    /**
     * Returns a set of HTTP headers (key-value pairs) that should be added to a
     * HTTP call. This will be called by the AtomPub and the Web Services
     * binding. You might want to check the binding in use before you set the
     * headers.
     * 
     * @param url
     *            the URL of the HTTP call
     * 
     * @return the HTTP headers or <code>null</code> if no additional headers
     *         should be set
     */
    Map<String, List<String>> getHTTPHeaders(String url);

    /**
     * Returns a SOAP header that should be added to a Web Services call.
     * 
     * @param portObject
     *            the port object
     * 
     * @return the SOAP headers or <code>null</code> if no additional headers
     *         should be set
     */
    Element getSOAPHeaders(Object portObject);

    /**
     * Receives the HTTP headers after a call.
     * 
     * @param url
     *            the URL
     * @param statusCode
     *            the status code
     * @param headers
     *            the HTTP headers
     */
    void putResponseHeaders(String url, int statusCode, Map<String, List<String>> headers);
}
