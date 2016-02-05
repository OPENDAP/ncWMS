<%@ page import="uk.ac.rdg.resc.ncwms.util.Util" %>
<%@ page import="uk.ac.rdg.resc.ncwms.cache.TileCache" %>
<%@ page import="uk.ac.rdg.resc.ncwms.config.NcwmsController" %>
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server

    long mbyte = 1024*1024;
    long maxHeap = Runtime.getRuntime().maxMemory();
    long usedMemory = Runtime.getRuntime().totalMemory();
    long available =  maxHeap-usedMemory;

    long cachedTileCount = TileCache.getCachedTileCount();

%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%-- Administrative page
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.config.Config) --%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Administrative interface to ncWMS</title>
    </head>
    <body>

    <h1>ncWMS-1.4 Admin page</h1>

    <p><a href="../godiva2.html">Godiva2 interface</a></p>
    <c:choose>
        <c:when test="${config.allowsGlobalCapabilities}">
            <p><a href="../wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.3.0">WMS 1.3.0 Capabilities</a></p>
            <p><a href="../wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.1.1">WMS 1.1.1 Capabilities</a></p>
        </c:when>
        <c:otherwise>
            <em>The generation of Capabilities documents that include all datasets
            on this server is disabled: see Server Settings below.</em>
        </c:otherwise>
    </c:choose>
    <p><a href="usage.jsp">Usage monitor</a></p>
    <p><a href="../">ncWMS Front page</a></p>

    <form id="config" action="updateConfig" method="POST">
        
        <input type="submit" value="Save configuration" name="submit1"/>
        
        <h2>Datasets</h2>
        <table border="1">
        <tr><th>Edit variables</th><th>Unique ID</th><th>Title</th><th>Location</th><th>State</th><th>Last update</th><th>Auto refresh frequency</th><th>Force refresh?</th><th>Disabled?</th><th>Queryable?</th><th>Remove?</th><th>Data reading class</th><th>Link to more info</th><th>Copyright statement</th></tr>

            <c:forEach var="datasetEntry" items="${config.allDatasets}">
                <c:set var="dataset" value="${datasetEntry.value}"/>
                <tr<c:if test="${dataset.disabled}"> bgcolor="lightgrey"</c:if>>
                    <td>
                        <c:if test="${dataset.ready}">
                            <a href="editVariables?dataset=${dataset.id}">edit</a>
                        </c:if>
                    </td>
                    <td><input type="text" name="dataset.${dataset.id}.id" value="${dataset.id}"/></td>
                    <td><input type="text" name="dataset.${dataset.id}.title" value="${dataset.title}"/></td>
                    <td><input type="text" name="dataset.${dataset.id}.location" value="${dataset.location}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${dataset.error or dataset.loading}">
                                <a target="_blank" href="datasetStatus.jsp?dataset=${dataset.id}">${dataset.state}</a>
                            </c:when>
                            <c:otherwise>
                                ${dataset.state}
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${empty dataset.lastUpdateTime}">never</c:when>
                            <c:otherwise>${utils:dateTimeToISO8601(dataset.lastUpdateTime)}</c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <select name="dataset.${dataset.id}.updateinterval">
                            <option value="-1"<c:if test="${dataset.updateInterval < 0}"> selected="selected"</c:if>>Never</option>
                            <option value="1"<c:if test="${dataset.updateInterval == 1}"> selected="selected"</c:if>>Every minute</option>
                            <option value="10"<c:if test="${dataset.updateInterval == 10}"> selected="selected"</c:if>>Every 10 minutes</option>
                            <option value="30"<c:if test="${dataset.updateInterval == 30}"> selected="selected"</c:if>>Every half hour</option>
                            <option value="60"<c:if test="${dataset.updateInterval == 60}"> selected="selected"</c:if>>Hourly</option>
                            <option value="360"<c:if test="${dataset.updateInterval == 360}"> selected="selected"</c:if>>Every 6 hours</option>
                            <option value="720"<c:if test="${dataset.updateInterval == 720}"> selected="selected"</c:if>>Every 12 hours</option>
                            <option value="1440"<c:if test="${dataset.updateInterval == 1440}"> selected="selected"</c:if>>Daily</option>
                        </select>
                    </td>
                    <td><input type="checkbox" name="dataset.${dataset.id}.refresh"/></td>
                    <td><input type="checkbox" name="dataset.${dataset.id}.disabled"<c:if test="${dataset.disabled}"> checked="checked"</c:if>/></td>
                    <td><input type="checkbox" name="dataset.${dataset.id}.queryable"<c:if test="${dataset.queryable}"> checked="checked"</c:if>/></td>
                    <td><input type="checkbox" name="dataset.${dataset.id}.remove"/></td>
                    <td><input type="text" name="dataset.${dataset.id}.reader" value="${dataset.dataReaderClass}"/></td>
                    <td><input type="text" name="dataset.${dataset.id}.moreinfo" value="${dataset.moreInfoUrl}"/></td>
                    <td><input type="text" name="dataset.${dataset.id}.copyright" value="${dataset.copyrightStatement}"/></td>
                </tr>
            </c:forEach>
            <%-- Now add lines for the user to add new datasets --%>
            <c:forEach var="i" begin="0" end="2">
                <tr>
                    <td></td><%-- Corresponds with "Edit variables" --%>
                    <td><input type="text" name="dataset.new${i}.id" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.title" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.location" value=""/></td>
                    <td>N/A</td>
                    <td>N/A</td>
                    <td>
                        <select name="dataset.new${i}.updateinterval">
                            <option value="-1">Never</option>
                            <option value="1">Every minute</option>
                            <option value="10">Every 10 minutes</option>
                            <option value="30">Every half hour</option>
                            <option value="60">Hourly</option>
                            <option value="360">Every 6 hours</option>
                            <option value="720">Every 12 hours</option>
                            <option value="1440">Daily</option>
                        </select>
                    </td>
                    <td>N/A</td>
                    <td><input type="checkbox" name="dataset.new${i}.disabled"/></td>
                    <td><input type="checkbox" name="dataset.new${i}.queryable" checked="checked"/></td>
                    <td>N/A</td>
                    <td><input type="text" name="dataset.new${i}.reader" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.moreinfo" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.copyright" value=""/></td>
                </tr>
            </c:forEach>
        </table>


        <!-- ####################### Added by ndp 4/6/2014 ####################### -->
        <!-- This is the configuration section for the Dynamic Service configured for  -->
        <!-- this WMS.                                                             -->

        <h2>Dynamic Services</h2><p>
        Dynamic services are either local or DAP services which can be generated when requested, rather than being preconfigured.  This is 
        aimed at cases where a server wishes to access data stores which may consist of a very large number of datasets.  These datasets can
        be accessed via all of the usual ncWMS methods by adding the URL parameter DATASET={Unique ID}{/path/under/service/url}.
        </p><p>
        For example if you configure a dataset below with the ID "local" and the service URL "/media/data/model_output/" adding the parameter:
        </p><p>
        DATASET=local/mymodel/may_2014.nc
        </p><p>
        will generate a dataset from the NetCDF file at the path /media/data/model_output/mymodel/may_2014.nc
        </p><p>
        Any path will be tested against the regular expression supplied in the "Dataset Match Regex" and only paths matching that regular
        expression will be allowed.  For example, if the regex is set to ".*may.*" then the above example would work as before, but:
        </p><p>
        DATASET=local/mymodel/april_2014.nc
        </p><p>
        DATASET=local/mymodel/01-05-2014.nc
        </p><p>
        DATASET=local/mymodel/june_2014.nc
        </p><p>
        would all be treated as though there were no dataset, regardless of whether the files actually exist.
        </p>
           
        <table border="1" width="98%">
        <tr>
            <th>Unique ID</th>
            <th>Service URL</th>
            <th>Dataset Match Regex</th>
            <th>Disabled?</th>
            <th>Remove?</th>
            <th>Data reading class</th>
            <th>Link to more info</th>
            <th>Copyright statement</th>
        </tr>

            <c:forEach var="dynamicServiceEntry" items="${config.allDynamicServices}">
                <c:set var="dynamicService" value="${dynamicServiceEntry.value}"/>
                <tr<c:if test="${dynamicService.disabled}"> bgcolor="lightgrey"</c:if>>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.${dynamicService.id}.id" value="${dynamicService.id}"/></td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.${dynamicService.id}.serviceUrl" value="${dynamicService.serviceUrl}"/> </td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.${dynamicService.id}.datasetIdMatch"  value="${dynamicService.datasetIdMatch}"/> </td>

                    <td align="center" ><input type="checkbox" name="dynamicService.${dynamicService.id}.disabled"<c:if test="${dynamicService.disabled}"> checked="checked"</c:if> /></td>
                    <td align="center" ><input type="checkbox" name="dynamicService.${dynamicService.id}.remove"/></td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.${dynamicService.id}.reader" value="${dynamicService.dataReaderClass}"/></td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.${dynamicService.id}.moreinfo" value="${dynamicService.moreInfoUrl}"/></td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.${dynamicService.id}.copyright" value="${dynamicService.copyrightStatement}"/></td>
                </tr>
            </c:forEach>
            <%-- Now add lines for the user to add new Dynamic Services --%>
            <c:forEach var="i" begin="0" end="2">
                <tr>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.new${i}.id" value=""/></td>

                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.new${i}.serviceUrl" value=""/></td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.new${i}.datasetIdMatch" value=""/></td>


                    <td align="center" > <input type="checkbox" name="dynamicService.new${i}.disabled" /></td>
                    <td align="center" > N/A </td>      <%-- Corresponds to "remove" checkbox --%>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.new${i}.reader" value=""/></td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.new${i}.moreinfo" value=""/></td>
                    <td align="center" > <input type="text" style="width:95%;" name="dynamicService.new${i}.copyright" value=""/></td>


                </tr>
            </c:forEach>
        </table>
        <!-- ##################################################################### -->

        <!--<h2>THREDDS (experimental!)</h2>
        THREDDS catalog location: <input type="text" name="thredds.catalog.location" value="${config.threddsCatalogLocation}" size="60"/>-->
        
        <h2>Tile Cache settings</h2>
        <p>
            The ncWMS server uses a cache of recently-extracted data arrays (Tiles) to increase
            performance and reduce the load on the server.  Enabling the cache will consume
            both memory and  disk space (configurable below), and the number of Tiles
            that can be held in the cache (in memory or on disk) is a direct function of the
            Max Image Width and Max Image Height as defined below in the "Server Settings" area.<br/><br/>

            It is <em>crucial</em> that the combined memory footprint of the web applications (ncWMS
             and whatever else is running in your Web Service container) not exceed the available memory in the JVM,
            as this can cause the JVM to enter a deadlocked state that will consume all available
            JVM memory and CPU resources and may ultimately (at least on some operating systems)
            cause a global system crash. The upshot is that memory allocation for the cache should be done
            thoughtfully, and may require some tuning over time as the servers general pattern of
            resource consumption during operations reveals itself. Load testing is strongly recommended as
            a pathway to learning appropriate values before the server is rolled out for your enterprise.<br/> <br/>

            The in memory cache size and disk cache sizes are specified in megabytes, and the values are
            NOT checked against the current JVM settings. In other words you are allowed to break things. However
            the system will provide some warnings regarding these allocations.



        </p>


        <table style="border: 1px solid black;border-collapse: collapse;font-family: Courier">
            <th  colspan="2" style="border: 1px solid black; text-align: left;font-weight: bold">Current Memory Utilization</th>
            <tr>
                <td style="border: 1px solid black;">Total Available Memory (Max Heap) </td>
                <td style="border: 1px solid black;text-align: right;"> <%=maxHeap/mbyte%> MB</td>
            </tr>
            <tr>
                <td style="border: 1px solid black;">Currently Allocated Memory </td>
                <td style="border: 1px solid black;text-align: right;"> <%=usedMemory/mbyte%> MB</td>
            </tr>
            <tr>
                <td style="border: 1px solid black;">Available Memory (unallocated) </td>
                <td style="border: 1px solid black;text-align: right;"> <%=available/mbyte%> MB</td>
            </tr>

            <tr>
                <td style="border: 1px solid black;">Cached Tile Count </td>
                <td style="border: 1px solid black;text-align: center;"> <%=cachedTileCount%></td>
            </tr>
        </table>

        <c:set var="maxTileSize" value="${(config.server.maxImageWidth * config.server.maxImageHeight * 16 / (1024 * 1024))}"/>
        <p>
        The current max Tile size of ${config.server.maxImageWidth}x${config.server.maxImageHeight} pixels requires ${maxTileSize} MB of cache memory for each tile.
        </p>
        <p>
        While these values are presented to help inform you of the current state of the system please recognize that
        viewing them immediately after starting the server (before it has actually been asked to produce a WMS
        response) will provide a view of the memory utilization for the basic service stack (ncWMS plus whatever else
        is running in the server), which can be informative for
        assessing how much memory might be available for the cache during runtime. However, viewing these numbers
        after the server has been fulfilling WMS requests will provide a more comprehensive view of the total service
        memory utilization, including in memory caching activities.
        </p>


        <table border="1">
            <tr>
                <th>Enable cache?</th>
                <td><input type="checkbox" name="cache.enable"<c:if test="${config.cache.enabled}"> checked="checked"</c:if>/></td>
                <td>Check this box to enable the cache.  <font color="red">This can be changed while the server is running.</font></td>
            </tr>
            <tr>
                <th>Lifetime of each cached array (minutes)</th>
                <td><input type="text" name="cache.elementLifetime" value="${config.cache.elementLifetimeMinutes}"/></td>
                <td>Data arrays will expire automatically from the cache after this number of minutes. <br/>
                <span style="color: red; ">If you change this value you must restart the server for your change to take effect.</span></td>
            </tr>
            <tr>
                <c:set var="maxTilesInMemory" value="${ config.cache.maxCacheMemoryUtilization / maxTileSize}"/>

                <th>Maximum Memory For Cache (in MB)</th>
                <td><input type="text" name="cache.maxCacheMemoryUtilization" value="${config.cache.maxCacheMemoryUtilization}"/></td>

                <td>
                    Based on the current maximum image size of ${config.server.maxImageWidth}x${config.server.maxImageHeight} pixels this
                    value will allow configure the ncWMS cache to store no more than ${maxTilesInMemory} Tiles in the in-memory cache.<br/>
                    <span style="color: red; ">If you change this value you must restart the server for your change to take effect.</span>

                </td>
            </tr>
            <tr>
                <th>Enable disk store?</th>
                <td><input type="checkbox" name="cache.enableDiskStore"<c:if test="${config.cache.enableDiskStore}"> checked="checked"</c:if>/></td>
                <td>Check this box to enable the disk store (recommended).  If enabled, items that
                    do not fit into the memory footprint will overflow to disk.  All
                    items in the cache will automatically be written to disk when the
                    server is shut down or restarted.    <br/>
                <span style="color: red; ">If you change this value you must restart the server for your change to take effect.</span></td>
            </tr>
            <tr>
                <c:set var="maxTilesOnDisk" value="${ config.cache.maxCacheDiskUtilization / maxTileSize}"/>
                <th>Maximum Disk Utilization (in MB)</th>
                <td><input type="text" name="cache.maxCacheDiskUtilization" value="${config.cache.maxCacheDiskUtilization}"/></td>
                <td>Based on the current maximum image size of ${config.server.maxImageWidth}x${config.server.maxImageHeight} pixels this
                 value will configure the ncWMS cache to store no more than ${maxTilesOnDisk} Tiles in the disk cache.<br/>
                <span style="color: red; ">If you change this value you must restart the server for your change to take effect.</span></td>
            </tr>
        </table>
        
        <h2>Server settings</h2>
        <table border="1">
            <tr><th>Title</th><td><input type="text" name="server.title" value="${config.server.title}"/></td><td>Title for this WMS</td></tr>
            <!-- TODO: make the abstract field larger -->
            <tr><th>Abstract</th><td><input type="text" name="server.abstract" value="${config.server.serverAbstract}"/></td><td>More details about this server</td></tr>
            <tr><th>Keywords</th><td><input type="text" name="server.keywords" value="${config.server.keywords}"/></td><td>Comma-separated list of keywords</td></tr>
            <tr><th>URL</th><td><input type="text" name="server.url" value="${config.server.url}"/></td><td>Web site of the service provider</td></tr>
            <!-- TODO: do integer validation on max width and height -->
            <tr><th>Max image width</th><td><input type="text" name="server.maximagewidth" value="${config.server.maxImageWidth}"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Max image height</th><td><input type="text" name="server.maximageheight" value="${config.server.maxImageHeight}"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Allow GetFeatureInfo</th><td><input type="checkbox" name="server.allowfeatureinfo"<c:if test="${config.server.allowFeatureInfo}"> checked="checked"</c:if>/></td><td>Check this box to enable the GetFeatureInfo operation</td></tr>
            <tr><th>Allow global Capabilities</th><td><input type="checkbox" name="server.allowglobalcapabilities"<c:if test="${config.server.allowGlobalCapabilities}"> checked="checked"</c:if>/></td><td>Check this box to allow clients to request a
            WMS Capabilities document including all datasets on this server (not recommended if this server hosts a large number of datasets)</td></tr>
        </table>
        
        <h2>Contact information</h2>
        <table border="1">
            <tr><th>Name</th><td><input type="text" name="contact.name" value="${config.contact.name}"/></td><td>Name of server administrator</td></tr>
            <tr><th>Organization</th><td><input type="text" name="contact.org" value="${config.contact.org}"/></td><td>Organization of server administrator</td></tr>
            <tr><th>Telephone</th><td><input type="text" name="contact.tel" value="${config.contact.tel}"/></td><td>Telephone number of server administrator</td></tr>
            <tr><th>Email</th><td><input type="text" name="contact.email" value="${config.contact.email}"/></td><td>Email address of server administrator</td></tr>
        </table>
        
        <br />
        <input type="submit" value="Save configuration" name="submit2"/>
        
    </form>
    
    </body>
</html>
