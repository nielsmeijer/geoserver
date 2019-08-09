/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import com.boundlessgeo.gsr.model.AbstractGSRModel;
import com.boundlessgeo.gsr.model.GSRModel;
import com.boundlessgeo.gsr.model.AbstractGSRModel.Link;
import com.boundlessgeo.gsr.translate.map.LayerDAO;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A list of {@link LayerOrTable}, that can be serialized as JSON
 *
 * Also provides a number of static utility methods for interacting with this list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LayersAndTables extends AbstractGSRModel implements GSRModel {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LayersAndTables.class);

    public final ArrayList<LayerOrTable> layers;

    public final ArrayList<LayerOrTable> tables;

    public LayersAndTables(ArrayList<LayerOrTable> layers, ArrayList<LayerOrTable> tables) {
        this.layers = layers;
        this.tables = tables;
    }

    @Override
    public String toString() {
        return layers.toString() + ";" + tables.toString();
    }

    /**
     * Layer names are just integers IDs in Esri, but not in GeoServer. This method is basically a hack and really ought
     * to be rethought.
     * <p>
     * TODO
     *
     * @param catalog
     * @param layerName
     * @param workspaceName
     * @return
     */
    public static String integerIdToGeoserverLayerName(Catalog catalog, String layerName, String workspaceName) {
        String name = layerName;
        try {
            LayerOrTable layerOrTable = LayerDAO.find(catalog, workspaceName, Integer.parseInt(layerName));
            name = layerOrTable.getName();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } catch (NumberFormatException e) {
            //Just use string layer name for now.
        }
        return workspaceName + ":" + name;
    }

    public ArrayList<LayerOrTable> getLayers() {
        return layers;
    }

    public ArrayList<LayerOrTable> getTables() {
        return tables;
    }
    
    
}
