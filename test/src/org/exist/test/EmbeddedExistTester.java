/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2009 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id$
 */
package org.exist.test;

import static org.junit.Assert.*;

import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.OutputKeys;

import org.exist.util.MimeTable;
import org.exist.util.MimeType;

import org.exist.xmldb.DatabaseInstanceManager;
import org.exist.xmldb.XmldbURI;

import org.exist.storage.serializers.EXistOutputKeys;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

import org.xmldb.api.modules.*;

/**
 *
 * @author dizzzz
 */
public class EmbeddedExistTester {

    protected final static String URI = XmldbURI.LOCAL_DB;
    protected final static String DRIVER = "org.exist.xmldb.DatabaseImpl";
    
    protected static Collection rootCollection = null;

    protected static XPathQueryService xpxqService = null;
    protected static Database database = null;
    protected static CollectionManagementService cmService = null;
    protected static XQueryService xqService = null;

    @BeforeClass
    public static void before() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XMLDBException {
        Class<?> cl = Class.forName("org.exist.xmldb.DatabaseImpl");
        database = (Database) cl.newInstance();
        database.setProperty("create-database", "true");
        DatabaseManager.registerDatabase(database);

        rootCollection = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "");
        xpxqService = (XPathQueryService) rootCollection.getService("XPathQueryService", "1.0");
        cmService = (CollectionManagementService) rootCollection.getService("CollectionManagementService", "1.0");
        xqService = (XQueryService) rootCollection.getService("XQueryService", "1.0");
    }

    @AfterClass
    public static void after() throws XMLDBException {
        DatabaseManager.deregisterDatabase(database);
        DatabaseInstanceManager dim = (DatabaseInstanceManager) rootCollection.getService("DatabaseInstanceManager", "1.0");
        dim.shutdown();
        database = null;
    }

    protected static Collection createCollection(Collection collection, String collectionName) throws XMLDBException {
        Collection newCollection = collection.getChildCollection(collectionName);
        if (newCollection == null) {
            cmService.createCollection(collectionName);
        }

        newCollection = DatabaseManager.getCollection(URI + "/" + collectionName, "admin", "");
        assertNotNull(newCollection);
        return newCollection;
    }

    protected static void storeResource(final Collection collection, final String documentName, final byte[] content) throws XMLDBException {
        final MimeType mime = MimeTable.getInstance().getContentTypeFor(documentName);
        final String type = mime.isXMLType() ? XMLResource.RESOURCE_TYPE : BinaryResource.RESOURCE_TYPE;
        final Resource resource = collection.createResource(documentName, type);
        resource.setContent(content);
        collection.storeResource(resource);
        collection.close();
    }

    protected static ResourceSet executeQuery(String query) throws XMLDBException {
        CompiledExpression compiledQuery = xqService.compile(query);
        ResourceSet result = xqService.execute(compiledQuery);
        return result;
    }
    
    protected static String executeOneValue(String query) throws XMLDBException {
        ResourceSet results = executeQuery(query);
        assertEquals(1, results.getSize());
        return (String) results.getResource(0).getContent();
    }
    

    protected static byte[] readFile(final Path directory, final String filename) throws IOException {
        return readFile(directory.resolve(filename));
    }

    protected static byte[] readFile(final Path file) throws IOException {
        assertTrue(Files.isReadable(file));
        return Files.readAllBytes(file);
    }

    protected String getXMLResource(Collection collection, String resource) throws XMLDBException{
		collection.setProperty(OutputKeys.INDENT, "yes");
		collection.setProperty(EXistOutputKeys.EXPAND_XINCLUDES, "no");
        collection.setProperty(EXistOutputKeys.PROCESS_XSL_PI, "yes");
		XMLResource res = (XMLResource)collection.getResource(resource);
        String retval = res.getContent().toString();
        collection.close();
        return retval;
    }
}
