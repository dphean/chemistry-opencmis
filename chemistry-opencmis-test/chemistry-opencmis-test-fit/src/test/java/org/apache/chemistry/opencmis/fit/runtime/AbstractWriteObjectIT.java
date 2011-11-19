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
package org.apache.chemistry.opencmis.fit.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractWriteObjectIT extends AbstractSessionTest {

    @Test
    public void createFolder() {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.FOLDER_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        ObjectId id = session.createFolder(properties, parentId);
        assertNotNull(id);
    }

    @Test
    public void createDocument() throws IOException {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.DOCUMENT_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        String filename = UUID.randomUUID().toString();
        String mimetype = "text/html; charset=UTF-8";
        String content1 = "Im Walde rauscht ein Wasserfall. Wenn's nicht mehr rauscht ist's Wasser all.";

        byte[] buf1 = content1.getBytes("UTF-8");
        ByteArrayInputStream in1 = new ByteArrayInputStream(buf1);
        ContentStream contentStream = session.getObjectFactory().createContentStream(filename, buf1.length, mimetype,
                in1);
        assertNotNull(contentStream);

        ObjectId id = session.createDocument(properties, parentId, contentStream, VersioningState.NONE);
        assertNotNull(id);

        // verify content
        Document doc = (Document) session.getObject(id);
        assertNotNull(doc);
        // Assert.assertEquals(buf1.length, doc.getContentStreamLength());
        // Assert.assertEquals(mimetype, doc.getContentStreamMimeType());
        // Assert.assertEquals(filename, doc.getContentStreamFileName());
        String content2 = getContentAsString(doc.getContentStream());
        assertEquals(content1, content2);
    }

    @Test
    public void createBigDocument() {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.DOCUMENT_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        String filename = UUID.randomUUID().toString();
        String mimetype = "application/octet-stream";

        final int size = 10 * 1024 * 1024; // 10 MiB

        InputStream in = new InputStream() {

            private int counter = -1;

            @Override
            public int read() throws IOException {
                counter++;
                if (counter >= size) {
                    return -1;
                }

                return '0' + (counter / 10);
            }
        };

        ContentStream contentStream = session.getObjectFactory().createContentStream(filename, size, mimetype, in);
        assertNotNull(contentStream);

        long start = System.currentTimeMillis();
        ObjectId id = session.createDocument(properties, parentId, contentStream, VersioningState.NONE);
        long end = System.currentTimeMillis();

        assertNotNull(id);
        
        Document doc = (Document) session.getObject(id);
        assertNotNull(doc);
        
        doc.delete(true);

        log.info("createDocument with " + size + " bytes:" + (end - start) + "ms");
    }

    @Test
    public void setBigContent() {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.DOCUMENT_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        String filename = UUID.randomUUID().toString();
        String mimetype = "application/octet-stream";

        ObjectId id = session.createDocument(properties, parentId, null, VersioningState.NONE);
        assertNotNull(id);

        // verify content
        Document doc = (Document) session.getObject(id);
        assertNotNull(doc);

        final int size = 10 * 1024 * 1024; // 10 MiB

        InputStream in = new InputStream() {

            private int counter = -1;

            @Override
            public int read() throws IOException {
                counter++;
                if (counter >= size) {
                    return -1;
                }

                return '0' + (counter / 10);
            }
        };

        ContentStream contentStream = session.getObjectFactory().createContentStream(filename, size, mimetype, in);
        assertNotNull(contentStream);

        long start = System.currentTimeMillis();
        doc.setContentStream(contentStream, true);
        long end = System.currentTimeMillis();

        doc.delete(true);

        log.info("setContentStream with " + size + " bytes:" + (end - start) + "ms");
    }

    @Test
    public void createDocumentFromSource() throws IOException {
        try {
            // verify content
            String path = "/" + Fixture.TEST_ROOT_FOLDER_NAME + "/" + FixtureData.DOCUMENT1_NAME;
            Document srcDocument = (Document) session.getObjectByPath(path);
            assertNotNull("Document not found: " + path, srcDocument);
            String srcContent = getContentAsString(srcDocument.getContentStream());

            ObjectId parentFolder = session.createObjectId(fixture.getTestRootId());
            String name = UUID.randomUUID().toString();

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PropertyIds.NAME, name);

            ObjectId dstDocumentId = session.createDocumentFromSource(srcDocument, properties, parentFolder,
                    VersioningState.NONE);
            assertNotNull(dstDocumentId);
            Document dstDocument = (Document) session.getObject(dstDocumentId);
            String dstContent = getContentAsString(dstDocument.getContentStream());
            assertEquals(srcContent, dstContent);

        } catch (CmisNotSupportedException e) {
            // not an error
            log.info(e.getMessage());
        }
    }

    @Test
    public void deleteAndCreateContent() throws IOException {
        // verify content

        String path = "/" + Fixture.TEST_ROOT_FOLDER_NAME + "/" + FixtureData.DOCUMENT1_NAME;
        Document document = (Document) session.getObjectByPath(path);
        assertNotNull("Document not found: " + path, document);

        // check default content
        ContentStream contentStream = document.getContentStream();
        assertNotNull(contentStream);
        String contentString = getContentAsString(contentStream);
        assertNotNull(contentString);

        // delete and set new content
        // ObjectId id = (return id not supported by AtomPub)
        document.deleteContentStream();
        // assertNotNull(id);

        String filename = UUID.randomUUID().toString();
        String mimetype = "text/html; charset=UTF-8";
        String content1 = "Im Walde rauscht ein Wasserfall. Wenn's nicht mehr rauscht ist's Wasser all.";

        byte[] buf1 = content1.getBytes("UTF-8");
        ByteArrayInputStream in1 = new ByteArrayInputStream(buf1);
        contentStream = session.getObjectFactory().createContentStream(filename, buf1.length, mimetype, in1);
        assertNotNull(contentStream);

        document.setContentStream(contentStream, true);

        // check default content
        ContentStream contentStream2 = document.getContentStream();
        assertNotNull(contentStream2);
        String contentString2 = getContentAsString(contentStream2);
        assertNotNull(contentString2);

        assertEquals(content1, contentString2);
    }

    @Test
    public void updateProperties() {
        // verify content
        String path = "/" + Fixture.TEST_ROOT_FOLDER_NAME + "/" + FixtureData.DOCUMENT1_NAME;
        Document document = (Document) session.getObjectByPath(path);
        assertNotNull("Document not found: " + path, document);

        // TODO: adapt test to refactored interface
        // document.setProperty(PropertyIds.NAME, "Neuer Name");
        // document.updateProperties();
        // assertEquals("Neuer Name", document.getName());
    }

    @Test
    public void updateSinglePropertyAndCheckName() {
        // verify content
        String path = "/" + Fixture.TEST_ROOT_FOLDER_NAME + "/" + FixtureData.DOCUMENT1_NAME;
        Document document = (Document) session.getObjectByPath(path);
        assertNotNull("Document not found: " + path, document);

        String value = UUID.randomUUID().toString();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.CHECKIN_COMMENT, value);

        String id = document.getId();
        assertNotNull(id);

        // update single property
        ObjectId newId = document.updateProperties(properties);
        assertNotNull(newId);
        assertEquals(id, newId.getId()); // should not be a new version

        session.clear();

        // verify
        String s1 = FixtureData.DOCUMENT1_NAME.toString();
        String s2 = document.getName();
        assertEquals(s1, s2);

        Property<String> p = document.getProperty(PropertyIds.NAME);
        String s3 = p.getFirstValue();
        assertEquals(s1, s3);

        Document document2 = (Document) session.getObjectByPath(path);
        assertNotNull("Document not found: " + path, document2);
    }

    private static String getContentAsString(ContentStream stream) throws IOException {
        assertNotNull(stream);
        InputStream in2 = stream.getStream();
        assertNotNull(in2);
        StringBuffer sbuf = null;
        sbuf = new StringBuffer(in2.available());
        int count;
        byte[] buf2 = new byte[100];
        while ((count = in2.read(buf2)) != -1) {
            for (int i = 0; i < count; i++) {
                sbuf.append((char) buf2[i]);
            }
        }
        in2.close();
        return sbuf.toString();
    }

    @Test
    public void createDocumentAndSetContent() throws IOException {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.DOCUMENT_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        String filename = UUID.randomUUID().toString();
        String mimetype = "text/html; charset=UTF-8";
        String content1 = "Im Walde rauscht ein Wasserfall. Wenn's nicht mehr rauscht ist's Wasser all.";

        byte[] buf1 = content1.getBytes("UTF-8");
        ByteArrayInputStream in1 = new ByteArrayInputStream(buf1);
        ContentStream contentStream = session.getObjectFactory().createContentStream(filename, buf1.length, mimetype,
                in1);
        assertNotNull(contentStream);

        ObjectId id = session.createDocument(properties, parentId, null, VersioningState.NONE);
        assertNotNull(id);

        // set and verify content
        Document doc = (Document) session.getObject(id);
        assertNotNull(doc);
        doc.setContentStream(contentStream, true);

        doc.refresh();
        assertEquals(buf1.length, doc.getContentStreamLength());
        assertEquals(mimetype, doc.getContentStreamMimeType());
        assertEquals(filename, doc.getContentStreamFileName());
        String content2 = getContentAsString(doc.getContentStream());
        assertEquals(content1, content2);
    }

    @Ignore
    @Test
    public void createDocumentAndSetContentNoOverwrite() throws IOException {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.DOCUMENT_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        String filename = UUID.randomUUID().toString();
        String mimetype = "text/html; charset=UTF-8";
        String content1 = "Im Walde rauscht ein Wasserfall. Wenn's nicht mehr rauscht ist's Wasser all.";

        byte[] buf1 = content1.getBytes("UTF-8");
        ByteArrayInputStream in1 = new ByteArrayInputStream(buf1);
        ContentStream contentStream = session.getObjectFactory().createContentStream(filename, buf1.length, mimetype,
                in1);
        assertNotNull(contentStream);

        ObjectId id = session.createDocument(properties, parentId, null, VersioningState.NONE);
        assertNotNull(id);

        // set and verify content
        Document doc = (Document) session.getObject(id);
        assertNotNull(doc);
        doc.setContentStream(contentStream, false);

        doc.refresh();
        assertEquals(buf1.length, doc.getContentStreamLength());
        assertEquals(mimetype, doc.getContentStreamMimeType());
        assertEquals(filename, doc.getContentStreamFileName());
        String content2 = getContentAsString(doc.getContentStream());
        assertEquals(content1, content2);
    }

    @Test
    public void createDocumentAndUpdateContent() throws IOException {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.DOCUMENT_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        String filename1 = UUID.randomUUID().toString();
        String mimetype = "text/html; charset=UTF-8";
        String content1 = "Im Walde rauscht ein Wasserfall. Wenn's nicht mehr rauscht ist's Wasser all.";

        byte[] buf1 = content1.getBytes("UTF-8");
        ByteArrayInputStream in1 = new ByteArrayInputStream(buf1);
        ContentStream contentStream1 = session.getObjectFactory().createContentStream(filename1, buf1.length, mimetype,
                in1);
        assertNotNull(contentStream1);

        ObjectId id = session.createDocument(properties, parentId, contentStream1, VersioningState.NONE);
        assertNotNull(id);

        // set and verify content
        String filename2 = UUID.randomUUID().toString();
        String content2 = "abc";

        byte[] buf2 = content2.getBytes("UTF-8");
        ByteArrayInputStream in2 = new ByteArrayInputStream(buf2);
        ContentStream contentStream2 = session.getObjectFactory().createContentStream(filename2, buf2.length, mimetype,
                in2);
        assertNotNull(contentStream2);

        Document doc = (Document) session.getObject(id);
        assertNotNull(doc);
        doc.setContentStream(contentStream2, true);

        doc.refresh();
        assertEquals(buf2.length, doc.getContentStreamLength());
        assertEquals(mimetype, doc.getContentStreamMimeType());
        assertEquals(filename2, doc.getContentStreamFileName());
        String content3 = getContentAsString(doc.getContentStream());
        assertEquals(content2, content3);
    }

    @Ignore
    @Test(expected = CmisContentAlreadyExistsException.class)
    public void createDocumentAndUpdateContentNoOverwrite() throws IOException {
        ObjectId parentId = session.createObjectId(fixture.getTestRootId());
        String folderName = UUID.randomUUID().toString();
        String typeId = FixtureData.DOCUMENT_TYPE_ID.value();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, typeId);

        String filename1 = UUID.randomUUID().toString();
        String mimetype = "text/html; charset=UTF-8";
        String content1 = "Im Walde rauscht ein Wasserfall. Wenn's nicht mehr rauscht ist's Wasser all.";

        byte[] buf1 = content1.getBytes("UTF-8");
        ByteArrayInputStream in1 = new ByteArrayInputStream(buf1);
        ContentStream contentStream1 = session.getObjectFactory().createContentStream(filename1, buf1.length, mimetype,
                in1);
        assertNotNull(contentStream1);

        ObjectId id = session.createDocument(properties, parentId, contentStream1, VersioningState.NONE);
        assertNotNull(id);

        // set and verify content
        String filename2 = UUID.randomUUID().toString();
        String content2 = "abc";

        byte[] buf2 = content2.getBytes("UTF-8");
        ByteArrayInputStream in2 = new ByteArrayInputStream(buf2);
        ContentStream contentStream2 = session.getObjectFactory().createContentStream(filename2, buf2.length, mimetype,
                in2);
        assertNotNull(contentStream2);

        Document doc = (Document) session.getObject(id);
        assertNotNull(doc);
        doc.setContentStream(contentStream2, false);

        doc.refresh();
        assertEquals(buf2.length, doc.getContentStreamLength());
        assertEquals(mimetype, doc.getContentStreamMimeType());
        assertEquals(filename2, doc.getContentStreamFileName());
        String content3 = getContentAsString(doc.getContentStream());
        assertEquals(content2, content3);
    }

}
