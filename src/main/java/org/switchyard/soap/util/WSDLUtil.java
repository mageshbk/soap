/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 
package org.switchyard.soap.util;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.wsdl.Definition;
import javax.xml.transform.stream.StreamSource;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

/**
 * Contains utility methods to examine/manipulate WSDLs.
 */
public final class WSDLUtil {

    private WSDLUtil() {
    }

    /**
     * Read the WSDL document and create a WSDL Definition.
     *
     * @param wsdlLocation location pointing to a WSDL XML definition.
     * @return the Definition.
     * @throws WSDLException If unable to read the WSDL
     */
    public static Definition readWSDL(final String wsdlLocation) throws WSDLException {
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader reader = wsdlFactory.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        return reader.readWSDL(wsdlLocation);
    }

    /**
     * Read the WSDL document accessible via the specified
     * URI into a StreamSource.
     *
     * @param wsdlURI a URI (can be a filename or URL) pointing to a
     * WSDL XML definition.
     * @return the StreamSource.
     * @throws WSDLException If unable to read the WSDL
     */
    public static StreamSource getStream(final String wsdlURI) throws WSDLException {
        try {
            URL url = getURL(null, wsdlURI);
            InputStream inputStream = url.openStream();
            StreamSource inputSource = new StreamSource(inputStream);
            inputSource.setSystemId(url.toString());
            return inputSource;
        } catch (Exception e) {
            throw new WSDLException(WSDLException.OTHER_ERROR,
                    "Unable to resolve imported document at '"
                    + wsdlURI, e);
        }
    }

    /**
     * Convert a path/uri to a URL.
     *
     * @param url a url path.
     * @param path a suffix path.
     * @return the URL.
     * @throws MalformedURLException If the url path is not valid
     */
    public static URL getURL(final URL url, final String path) throws MalformedURLException {
        try {
            return new URL(url, path);
        } catch (MalformedURLException murle) {
            File localFile = new File(path);
            if ((url == null) || ((url != null) && (localFile.isAbsolute()))) {
                return localFile.toURL();
            }
            throw murle;
        }
    }
}
