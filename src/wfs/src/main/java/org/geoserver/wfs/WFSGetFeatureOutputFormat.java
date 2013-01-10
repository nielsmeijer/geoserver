/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.ResultTypeType;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.response.WFSResponse;


/**
 * Base class for a response to a WFS GetFeature operation.
 * <p>
 * The result of a GetFeature operation is an instance of
 * {@link FeatureCollectionType}. Subclasses are responsible for serializing
 * an instance of this type in {@link #write(FeatureCollectionType, OutputStream, Operation)}.
 * </p>
 * <p>
 * Subclasses also need declare the mime-type in which the format is encoded.
 * </p>
 *
 * @author Gabriel Rold?n, Axios Engineering
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public abstract class WFSGetFeatureOutputFormat extends WFSResponse {
    
    /**
     * Based on definition of valid xml element name at http://www.w3.org/TR/xml/#NT-Name
     */
    static final Pattern XML_ELEMENT = Pattern.compile("[:A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02ff\\u0370-\\u037d"
            + "\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f\\u2c00-\\u2fef\\u3001-\\ud7ff"
            + "\\uf900-\\ufdcf\\ufdf0-\\ufffd\\x10000-\\xEFFFF]"
            + "[:A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6"
            + "\\u00F8-\\u02ff\\u0370-\\u037d\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f"
            + "\\u2c00-\\u2fef\\u3001-\\udfff\\uf900-\\ufdcf\\ufdf0-\\ufffd\\\\x10000-\\\\xEFFFF\\-\\.0-9"
            + "\\u00b7\\u0300-\\u036f\\u203f-\\u2040]*\\Z");

    /**
     * logger
     */
    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");
    /**
     * Constructor which sets the outputFormat.
     *
     * @param outputFormat The well-known name of the format, not <code>null</code>
     */
    public WFSGetFeatureOutputFormat(GeoServer gs, String outputFormat) {
        super(gs, FeatureCollectionType.class, outputFormat);
    }
    
    /**
     * Constructor which sets the outputFormats.
     *
     * @param outputFormats Set of well-known name of the format, not <code>null</code>
     */
    public WFSGetFeatureOutputFormat(GeoServer gs, Set<String> outputFormats) {
        super(gs, FeatureCollectionType.class, outputFormats);
    }

    /**
     * Returns the mime type <code>text/xml</code>.
     * <p>
     * Subclasses should override this method to provide a diffent output
     * format.
     * </p>
     */
    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return "text/xml";
    }

    /**
     * Ensures that the operation being executed is a GetFeature operation.
     * <p>
     * Subclasses may implement
     * </p>
     */
    public boolean canHandle(Operation operation) {
        //GetFeature operation?
        if ("GetFeature".equalsIgnoreCase(operation.getId())
                || "GetFeatureWithLock".equalsIgnoreCase(operation.getId())) {
            //also check that the resultType is "results"
            GetFeatureType request = (GetFeatureType) OwsUtils.parameter(operation.getParameters(),
                    GetFeatureType.class);

            if (request.getResultType() == ResultTypeType.RESULTS_LITERAL) {
                //call subclass hook
                return canHandleInternal(operation);
            }
        }

        return false;
    }
    
    /**
     * capabilities output format string.  Something that's a valid XML element name.
     * This should be overriden in each outputformat subclass, and if it's not a warning will be
     * issued.
     */
    public /*abstract*/ String getCapabilitiesElementName() {
        LOGGER.severe("ERROR IN " + this.getClass() + " IMPLEMENTATION.  getCapabilitiesElementName() should return a" + 
                "valid XML element name string for use in the WFS 1.0.0 capabilities document.");
        String of = getOutputFormat();
        if(of == null) {
            return null;
        }
        
        // wfs 1.1 form is not a valid xml element, do a check
        if (XML_ELEMENT.matcher(of).matches()) {
            return of;
        } else {
            String name = this.getClass().getName();
            if ( name.indexOf('.') != -1 ) {
                name = name.substring(name.lastIndexOf('.') + 1);
            }
            
            return name;
        }
    }
    
    /**
     * Returns the list of output format names generated by this format, for inclusion in
     * the WFS 1.0 capabilities document as XML element names 
     * @return
     */
    public List<String> getCapabilitiesElementNames() {
        String name = getCapabilitiesElementName();
        if(name == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(name);
        }
    }
    
    /**
     * Subclasses can delegate to this method if they want the full list of valid
     * output format element names to be returned in the WFS 1.0 capabilities  
     * @return
     */
    protected List<String> getAllCapabilitiesElementNames() {
        List<String> result = new ArrayList<String>();
        for (String name : getOutputFormats()) {
            if (XML_ELEMENT.matcher(name).matches()) {
                result.add(name);
            }
        }
        
        // have the output order be independent of the used JDK
        Collections.sort(result);
        
        return result;
    }

    /**
     * Hook for subclasses to add addtional checks to {@link #canHandle(Operation)}.
     * <p>
     * Subclasses may override this method if need be, the default impelementation
     * returns <code>true</code>
     * </p>
     * @param operation The operation being performed.
     *
     * @return <code>true</code> if the output format can handle the operation,
     *         otherwise <code>false</code>
     */
    protected boolean canHandleInternal(Operation operation) {
        return true;
    }

    /**
     * Calls through to {@link #write(FeatureCollectionType, OutputStream, Operation)}.
     */
    public final void write(Object value, OutputStream output, Operation operation)
        throws IOException, ServiceException {
        write((FeatureCollectionType) value, output, operation);
    }

    /**
     * Serializes the feature collection in the format declared.
     *
     * @param featureCollection The feature collection.
     * @param output The output stream to serialize to.
     * @param getFeature The GetFeature operation descriptor.
     */
    protected abstract void write(FeatureCollectionType featureCollection, OutputStream output,
        Operation getFeature) throws IOException, ServiceException;
}
