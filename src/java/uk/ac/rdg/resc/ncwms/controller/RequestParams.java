/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

import javax.servlet.http.HttpServletRequest;

/**
 * Class that contains the parameters of the user's request.  Parameter names
 * are not case sensitive.
 *
 * @author Jon Blower
 */
public class RequestParams
{
    private Map<String, String> paramMap =  new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    /**
     * Creates a new RequestParams object from the given Map of parameter names
     * and values (normally gained from HttpServletRequest.getParameterMap()).
     * The Map matches parameter names (Strings) to parameter values (String
     * arrays).  This method is
     */
    @Deprecated
    public RequestParams(Map<?, ?> httpRequestParamMap)
    {

        ingestParameters(httpRequestParamMap);

    }


    /**
     * Creates a new RequestParams object from the HttpServletRequest object.
     * Dynamic Service datasets are identified either through the "dataset" query
     * parameter or as an extension of the URL path . The former is NOT a WMS
     * compliant request and the latter is. The keys in the request become
     * case insensitive.
     *
     */
    public RequestParams(HttpServletRequest request){


        ingestParameters(request.getParameterMap());

        addDynamicDatasetToParams(request);

    }

    private void ingestParameters(Map<?,?> requestParamMap){

        @SuppressWarnings("unchecked")
        Map<String, String[]> pMap = (Map<String, String[]>)requestParamMap;

        //
        // Url decode everything...
        //
        for (String name : pMap.keySet())
        {
            String[] values = pMap.get(name);
            assert values.length >= 1;
            try
            {
                String key = URLDecoder.decode(name.trim(), "UTF-8");
                String value = URLDecoder.decode(values[0].trim(), "UTF-8");
                paramMap.put(key, value);
            }
            catch(UnsupportedEncodingException uee)
            {
                // Shouldn't happen: UTF-8 should always be supported
                throw new AssertionError(uee);
            }
        }
    }

    /**
     * Returns the value of the parameter with the given name as a String, or null if the
     * parameter does not have a value.  This method is not sensitive to the case
     * of the parameter name.  Use getWmsVersion() to get the requested WMS version.
     */
    public String getString(String paramName)
    {
        return this.paramMap.get(paramName);
    }
    
    /**
     * Returns the value of the parameter with the given name, throwing a
     * WmsException if the parameter does not exist.  Use getMandatoryWmsVersion()
     * to get the requested WMS version.
     */
    public String getMandatoryString(String paramName) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            throw new WmsException("Must provide a value for parameter "
                + paramName.toUpperCase());
        }
        return value;
    }
    
    /**
     * Finds the WMS version that the user has requested.  This looks for both
     * WMTVER and VERSION, the latter taking precedence.  WMTVER is used by
     * older versions of WMS and older clients may use this in version negotiation.
     * @return The request WMS version as a string, or null if not set
     */
    public String getWmsVersion()
    {
        String version = this.getString("version");
        if (version == null)
        {
            version = this.getString("wmtver");
        }
        return version; // might be null
    }
    
    /**
     * Finds the WMS version that the user has requested, throwing a WmsException
     * if a version has not been set.
     * @return The request WMS version as a string
     * @throws WmsException if neither VERSION nor WMTVER have been requested
     */
    public String getMandatoryWmsVersion() throws WmsException
    {
        String version = this.getWmsVersion();
        if (version == null)
        {
            throw new WmsException("Must provide a value for VERSION");
        }
        return version;
    }
    
    /**
     * Returns the value of the parameter with the given name as a positive integer,
     * or the provided default if no parameter with the given name has been supplied.
     * Throws a WmsException if the parameter does not exist or if the value
     * is not a valid positive integer.  Zero is counted as a positive integer.
     */
    public int getPositiveInt(String paramName, int defaultValue) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null) return defaultValue;
        return parsePositiveInt(paramName, value);
    }
    
    /**
     * Returns the value of the parameter with the given name as a positive integer,
     * throwing a WmsException if the parameter does not exist or if the value
     * is not a valid positive integer.  Zero is counted as a positive integer.
     */
    public int getMandatoryPositiveInt(String paramName) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            throw new WmsException("Must provide a value for parameter "
                + paramName.toUpperCase());
        }
        return parsePositiveInt(paramName, value);
    }
    
    private static int parsePositiveInt(String paramName, String value) throws WmsException
    {
        try
        {
            int i = Integer.parseInt(value);
            if (i < 0)
            {
                throw new WmsException("Parameter " + paramName.toUpperCase() +
                    " must be a valid positive integer");
            }
            return i;
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Parameter " + paramName.toUpperCase() +
                " must be a valid positive integer");
        }
    }
    
    /**
     * Returns the value of the parameter with the given name, or the supplied
     * default value if the parameter does not exist.
     */
    public String getString(String paramName, String defaultValue)
    {
        String value = this.getString(paramName);
        if (value == null) return defaultValue;
        return value;
    }

    /**
     * Returns the value of the parameter with the given name as a boolean value,
     * or the provided default if no parameter with the given name has been supplied.
     * @throws WmsException if the value is not a valid boolean string ("true" or "false",
     * case-insensitive).
     */
    public boolean getBoolean(String paramName, boolean defaultValue) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null) return defaultValue;
        value = value.trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new WmsException("Invalid boolean value for parameter " + paramName);
    }
    
    /**
     * Returns the value of the parameter with the given name, or the supplied
     * default value if the parameter does not exist.
     * @throws WmsException if the value of the parameter is not a valid
     * floating-point number
     */
    public float getFloat(String paramName, float defaultValue) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Float.parseFloat(value);
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Parameter " + paramName.toUpperCase() +
                " must be a valid floating-point number");
        }
    }


    /**
     *
     * @param request
     * @return
     */
    private String getDatasetId(HttpServletRequest request){


        String dataset = getString("DATASET");

        // Check the path for dynamic dataset content
        String pathDataset = request.getPathInfo();
        if(pathDataset!=null){
            // Found a dynamic dataset name, woot!

            // Dump leading / chars.
            while(pathDataset.startsWith("/") && pathDataset.length()>0)
                pathDataset = pathDataset.substring(1);

            // If there's still something left, make it the dataset name.
            if(pathDataset.length()>0)
                dataset = pathDataset;
        }
        return dataset;

    }

    /**
     * Appends the value of the DATASET parameter to LAYERS, LAYER, and QUERY_LAYERS, as appropriate and replaces
     * the internal MAp with the new modified one.
     */
    private void addDynamicDatasetToParams(HttpServletRequest request) {

        boolean first;
        String key;

        String dataset = getDatasetId(request);

        if (dataset != null && !"".equals(dataset)) {

            Map<String, String> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            parameters.putAll(paramMap);


            // If the dataset parameter does not exist then this MUST be a dataset defined
            // in the URL path, so we add parameter to the list for downstream compatibility.
            //
            if(!parameters.containsKey("dataset")){
                parameters.put("dataset",dataset);
            }

            //
            // Add the dataset name to each of the layer names
            //

            key = "layers";
            String layersStr = getString(key);

            if (layersStr != null && !"".equals(layersStr)) {
                StringBuilder finalLayersString = new StringBuilder();
                String[] layers = layersStr.split(",");
                first = true;
                for (String layer : layers) {
                    if(!first)
                        finalLayersString.append(",");
                    finalLayersString.append(dataset).append("/").append(layer);
                    first = false;
                }
                parameters.put(key, finalLayersString.toString() );
            }

            //
            // If applicable, add it to the QUERY_LAYERS parameter too
            //

            key = "query_layers";
            String querylayersStr = getString(key);

            if (querylayersStr != null && !"".equals(querylayersStr)) {
                StringBuilder finalQueryLayersString = new StringBuilder();
                String[] queryLayers = querylayersStr.split(",");
                first = true;
                for (String queryLayer : queryLayers) {
                    if(!first)
                        finalQueryLayersString.append(",");

                    finalQueryLayersString.append(dataset).append("/").append(queryLayer);
                    first = false;
                }
                parameters.put(key, finalQueryLayersString.toString());
            }

            //
            // If applicable, add it to the LAYER parameter too
            //
            key = "layer";
            String layerStr = getString(key);
            if (layerStr != null && !"".equals(layerStr)) {
                layerStr = dataset + "/" + layerStr;
                parameters.put(key, layerStr);
            }

            paramMap = parameters;
        }

    }







}
