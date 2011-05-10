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
package org.apache.chemistry.opencmis.client.bindings.spi.webservices;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;

import org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper;
import org.apache.chemistry.opencmis.client.bindings.spi.Session;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.jaxb.ACLService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.ACLServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.DiscoveryService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.DiscoveryServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.MultiFilingService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.MultiFilingServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.NavigationService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.NavigationServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.ObjectService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.ObjectServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.PolicyService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.PolicyServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.RelationshipService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.RelationshipServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.RepositoryService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.RepositoryServicePort;
import org.apache.chemistry.opencmis.commons.impl.jaxb.VersioningService;
import org.apache.chemistry.opencmis.commons.impl.jaxb.VersioningServicePort;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractPortProvider {

    private static final Log log = LogFactory.getLog(AbstractPortProvider.class);

    public static final String CMIS_NAMESPACE = "http://docs.oasis-open.org/ns/cmis/ws/200908/";

    public static final String REPOSITORY_SERVICE = "RepositoryService";
    public static final String OBJECT_SERVICE = "ObjectService";
    public static final String DISCOVERY_SERVICE = "DiscoveryService";
    public static final String NAVIGATION_SERVICE = "NavigationService";
    public static final String MULTIFILING_SERVICE = "MultiFilingService";
    public static final String VERSIONING_SERVICE = "VersioningService";
    public static final String RELATIONSHIP_SERVICE = "RelationshipService";
    public static final String POLICY_SERVICE = "PolicyService";
    public static final String ACL_SERVICE = "ACLService";

    protected static final int CHUNK_SIZE = 64 * 1024;

    protected Session session;

    /**
     * Return the Repository Service port object.
     */
    public RepositoryServicePort getRepositoryServicePort() {
        return (RepositoryServicePort) getPortObject(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE);
    }

    /**
     * Return the Navigation Service port object.
     */
    public NavigationServicePort getNavigationServicePort() {
        return (NavigationServicePort) getPortObject(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE);
    }

    /**
     * Return the Object Service port object.
     */
    public ObjectServicePort getObjectServicePort() {
        return (ObjectServicePort) getPortObject(SessionParameter.WEBSERVICES_OBJECT_SERVICE);
    }

    /**
     * Return the Versioning Service port object.
     */
    public VersioningServicePort getVersioningServicePort() {
        return (VersioningServicePort) getPortObject(SessionParameter.WEBSERVICES_VERSIONING_SERVICE);
    }

    /**
     * Return the Discovery Service port object.
     */
    public DiscoveryServicePort getDiscoveryServicePort() {
        return (DiscoveryServicePort) getPortObject(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE);
    }

    /**
     * Return the MultiFiling Service port object.
     */
    public MultiFilingServicePort getMultiFilingServicePort() {
        return (MultiFilingServicePort) getPortObject(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE);
    }

    /**
     * Return the Relationship Service port object.
     */
    public RelationshipServicePort getRelationshipServicePort() {
        return (RelationshipServicePort) getPortObject(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE);
    }

    /**
     * Return the Policy Service port object.
     */
    public PolicyServicePort getPolicyServicePort() {
        return (PolicyServicePort) getPortObject(SessionParameter.WEBSERVICES_POLICY_SERVICE);
    }

    /**
     * Return the ACL Service port object.
     */
    public ACLServicePort getACLServicePort() {
        return (ACLServicePort) getPortObject(SessionParameter.WEBSERVICES_ACL_SERVICE);
    }

    public void endCall(Object portObject) {
        AuthenticationProvider authProvider = CmisBindingsHelper.getAuthenticationProvider(session);
        if (authProvider != null) {
            BindingProvider bp = (BindingProvider) portObject;
            String url = (String) bp.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            @SuppressWarnings("unchecked")
            Map<String, List<String>> headers = (Map<String, List<String>>) bp.getResponseContext().get(
                    MessageContext.HTTP_RESPONSE_HEADERS);
            authProvider.putResponseHeaders(url, headers);
        }
    }

    // ---- internal ----

    /**
     * Gets a port object from the session or (re-)initializes the port objects.
     */
    @SuppressWarnings("unchecked")
    protected Object getPortObject(String serviceKey) {
        Map<String, Service> serviceMap = (Map<String, Service>) session.get(SpiSessionParameter.SERVICES);

        // does the service map exist?
        if (serviceMap == null) {
            session.writeLock();
            try {
                // try again
                serviceMap = (Map<String, Service>) session.get(SpiSessionParameter.SERVICES);
                if (serviceMap == null) {
                    serviceMap = Collections.synchronizedMap(new HashMap<String, Service>());
                    session.put(SpiSessionParameter.SERVICES, serviceMap, true);
                }

                if (serviceMap.containsKey(serviceKey)) {
                    return createPortObject(serviceMap.get(serviceKey));
                }

                // create service object
                Service serviceObject = initServiceObject(serviceKey);
                serviceMap.put(serviceKey, serviceObject);

                // create port object
                return createPortObject(serviceObject);
            } finally {
                session.writeUnlock();
            }
        }

        // is the service in the service map?
        if (!serviceMap.containsKey(serviceKey)) {
            session.writeLock();
            try {
                // try again
                if (serviceMap.containsKey(serviceKey)) {
                    return createPortObject(serviceMap.get(serviceKey));
                }

                // create object
                Service serviceObject = initServiceObject(serviceKey);
                serviceMap.put(serviceKey, serviceObject);

                return createPortObject(serviceObject);
            } finally {
                session.writeUnlock();
            }
        }

        return createPortObject(serviceMap.get(serviceKey));
    }

    /**
     * Creates a service object.
     */
    protected Service initServiceObject(String serviceKey) {
        Service serviceObject = null;

        if (log.isDebugEnabled()) {
            log.debug("Initializing Web Service " + serviceKey + "...");
        }

        try {
            // get WSDL URL
            URL wsdlUrl = new URL((String) session.get(serviceKey));

            // build the requested service object
            if (SessionParameter.WEBSERVICES_REPOSITORY_SERVICE.equals(serviceKey)) {
                serviceObject = new RepositoryService(wsdlUrl, new QName(CMIS_NAMESPACE, REPOSITORY_SERVICE));
            } else if (SessionParameter.WEBSERVICES_NAVIGATION_SERVICE.equals(serviceKey)) {
                serviceObject = new NavigationService(wsdlUrl, new QName(CMIS_NAMESPACE, NAVIGATION_SERVICE));
            } else if (SessionParameter.WEBSERVICES_OBJECT_SERVICE.equals(serviceKey)) {
                serviceObject = new ObjectService(wsdlUrl, new QName(CMIS_NAMESPACE, OBJECT_SERVICE));
            } else if (SessionParameter.WEBSERVICES_VERSIONING_SERVICE.equals(serviceKey)) {
                serviceObject = new VersioningService(wsdlUrl, new QName(CMIS_NAMESPACE, VERSIONING_SERVICE));
            } else if (SessionParameter.WEBSERVICES_DISCOVERY_SERVICE.equals(serviceKey)) {
                serviceObject = new DiscoveryService(wsdlUrl, new QName(CMIS_NAMESPACE, DISCOVERY_SERVICE));
            } else if (SessionParameter.WEBSERVICES_MULTIFILING_SERVICE.equals(serviceKey)) {
                serviceObject = new MultiFilingService(wsdlUrl, new QName(CMIS_NAMESPACE, MULTIFILING_SERVICE));
            } else if (SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE.equals(serviceKey)) {
                serviceObject = new RelationshipService(wsdlUrl, new QName(CMIS_NAMESPACE, RELATIONSHIP_SERVICE));
            } else if (SessionParameter.WEBSERVICES_POLICY_SERVICE.equals(serviceKey)) {
                serviceObject = new PolicyService(wsdlUrl, new QName(CMIS_NAMESPACE, POLICY_SERVICE));
            } else if (SessionParameter.WEBSERVICES_ACL_SERVICE.equals(serviceKey)) {
                serviceObject = new ACLService(wsdlUrl, new QName(CMIS_NAMESPACE, ACL_SERVICE));
            } else {
                throw new CmisRuntimeException("Cannot find Web Services service object [" + serviceKey + "]!");
            }
        } catch (CmisBaseException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CmisConnectionException("Cannot initalize Web Services service object [" + serviceKey + "]: "
                    + e.getMessage(), e);
        }

        return serviceObject;
    }

    /**
     * Creates a port object.
     */
    protected abstract Object createPortObject(Service service);
}