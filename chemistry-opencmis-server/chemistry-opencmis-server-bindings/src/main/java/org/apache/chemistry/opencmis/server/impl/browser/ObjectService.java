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
package org.apache.chemistry.opencmis.server.impl.browser;

import static org.apache.chemistry.opencmis.server.shared.HttpUtils.getBooleanParameter;
import static org.apache.chemistry.opencmis.server.shared.HttpUtils.getEnumParameter;
import static org.apache.chemistry.opencmis.server.shared.HttpUtils.getStringParameter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Constants;
import org.apache.chemistry.opencmis.commons.impl.ReturnVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.server.impl.browser.json.JSONConverter;
import org.json.simple.JSONObject;

/**
 * Object Service operations.
 */
public final class ObjectService {

    private static final int BUFFER_SIZE = 64 * 1024;

    private ObjectService() {
    }

    /**
     * createDocument.
     */
    public static void createDocument(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String folderId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        VersioningState versioningState = getEnumParameter(request, Constants.PARAM_VERSIONIG_STATE,
                VersioningState.class);
        String transaction = getStringParameter(request, BrowserBindingUtils.PARAM_TRANSACTION);

        ControlParser cp = new ControlParser(request);

        TypeCache typeCache = new TypeCache(repositoryId, service);

        String newObjectId = service.createDocument(repositoryId,
                BrowserBindingUtils.createProperties(cp, null, typeCache), folderId,
                BrowserBindingUtils.createContentStream(request), versioningState,
                BrowserBindingUtils.createPolicies(cp), BrowserBindingUtils.createAddAcl(cp),
                BrowserBindingUtils.createRemoveAcl(cp), null);

        ObjectInfo objectInfo = service.getObjectInfo(repositoryId, newObjectId);
        if (objectInfo == null) {
            throw new CmisRuntimeException("Object Info is missing!");
        }

        ObjectData object = objectInfo.getObject();
        if (object == null) {
            throw new CmisRuntimeException("Object is null!");
        }

        JSONObject jsonObject = JSONConverter.convert(object, typeCache);

        response.setStatus(HttpServletResponse.SC_CREATED);
        BrowserBindingUtils.setCookie(request, response, repositoryId, transaction,
                BrowserBindingUtils.createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

        BrowserBindingUtils.writeJSON(jsonObject, request, response);
    }

    /**
     * createFolder.
     */
    public static void createFolder(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String folderId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        String transaction = getStringParameter(request, BrowserBindingUtils.PARAM_TRANSACTION);

        ControlParser cp = new ControlParser(request);

        TypeCache typeCache = new TypeCache(repositoryId, service);

        String newObjectId = service.createFolder(repositoryId,
                BrowserBindingUtils.createProperties(cp, null, typeCache), folderId,
                BrowserBindingUtils.createPolicies(cp), BrowserBindingUtils.createAddAcl(cp),
                BrowserBindingUtils.createRemoveAcl(cp), null);

        ObjectInfo objectInfo = service.getObjectInfo(repositoryId, newObjectId);
        if (objectInfo == null) {
            throw new CmisRuntimeException("Object Info is missing!");
        }

        ObjectData object = objectInfo.getObject();
        if (object == null) {
            throw new CmisRuntimeException("Object is null!");
        }

        JSONObject jsonObject = JSONConverter.convert(object, typeCache);

        response.setStatus(HttpServletResponse.SC_CREATED);
        BrowserBindingUtils.setCookie(request, response, repositoryId, transaction,
                BrowserBindingUtils.createCookieValue(HttpServletResponse.SC_CREATED, object.getId(), null, null));

        BrowserBindingUtils.writeJSON(jsonObject, request, response);
    }

    /**
     * getObject.
     */
    public static void getObject(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String objectId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        ReturnVersion returnVersion = getEnumParameter(request, Constants.PARAM_RETURN_VERSION, ReturnVersion.class);
        String filter = getStringParameter(request, Constants.PARAM_FILTER);
        Boolean includeAllowableActions = getBooleanParameter(request, Constants.PARAM_ALLOWABLE_ACTIONS);
        IncludeRelationships includeRelationships = getEnumParameter(request, Constants.PARAM_RELATIONSHIPS,
                IncludeRelationships.class);
        String renditionFilter = getStringParameter(request, Constants.PARAM_RENDITION_FILTER);
        Boolean includePolicyIds = getBooleanParameter(request, Constants.PARAM_POLICY_IDS);
        Boolean includeAcl = getBooleanParameter(request, Constants.PARAM_ACL);

        // execute
        ObjectData object = null;

        if ((returnVersion == ReturnVersion.LATEST) || (returnVersion == ReturnVersion.LASTESTMAJOR)) {
            object = service.getObjectOfLatestVersion(repositoryId, objectId, null,
                    returnVersion == ReturnVersion.LASTESTMAJOR, filter, includeAllowableActions, includeRelationships,
                    renditionFilter, includePolicyIds, includeAcl, null);
        } else {
            object = service.getObject(repositoryId, objectId, filter, includeAllowableActions, includeRelationships,
                    renditionFilter, includePolicyIds, includeAcl, null);
        }

        if (object == null) {
            throw new CmisRuntimeException("Object is null!");
        }

        TypeCache typeCache = new TypeCache(repositoryId, service);
        JSONObject jsonObject = JSONConverter.convert(object, typeCache);

        response.setStatus(HttpServletResponse.SC_OK);
        BrowserBindingUtils.writeJSON(jsonObject, request, response);
    }

    /**
     * getContentStream.
     */
    public static void getContentStream(CallContext context, CmisService service, String repositoryId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get parameters
        String objectId = (String) context.get(BrowserBindingUtils.CONTEXT_OBJECT_ID);
        String streamId = getStringParameter(request, Constants.PARAM_STREAM_ID);

        BigInteger offset = context.getOffset();
        BigInteger length = context.getLength();

        // execute
        ContentStream content = service.getContentStream(repositoryId, objectId, streamId, offset, length, null);

        if ((content == null) || (content.getStream() == null)) {
            throw new CmisRuntimeException("Content stream is null!");
        }

        String contentType = content.getMimeType();
        if (contentType == null) {
            contentType = Constants.MEDIATYPE_OCTETSTREAM;
        }

        // set headers
        if ((offset == null) && (length == null)) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        response.setContentType(contentType);

        // send content
        InputStream in = new BufferedInputStream(content.getStream(), BUFFER_SIZE);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());

        byte[] buffer = new byte[BUFFER_SIZE];
        int b;
        while ((b = in.read(buffer)) > -1) {
            out.write(buffer, 0, b);
        }

        in.close();
        out.flush();
    }
}