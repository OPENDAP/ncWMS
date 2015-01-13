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

package uk.ac.rdg.resc.ncwms.cache;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.config.Config;

/**
 * <p>Uses the <a href="http://ehcache.sf.net">EHCache</a> software to cache
 * arrays of data that have been extracted.  This cache reduces the load on the server
 * in cases where clients make the same requests for data multiple times.  This 
 * happens commonly when clients use a tiling WMS interface such as OpenLayers or
 * Google Maps.  Since the cache stores data arrays and not images, clients can
 * make a new request for the same data in a different style: the data will then
 * be loaded from the cache and styled.  This supports the useful functionality
 * of the Godiva2 website, which allows the user to change the colour scale
 * of an image to increase (or reduce) contrast.</p>
 *
 * <p>It is of course important to ensure that the cache remains consistent with
 * the underlying data.  We wish to avoid, as far as possible, the situation where
 * cached data are used incorrectly because the underlying data have changed.
 * The following measures are taken to preserve cache consistency:</p>
 * <ol>
 * <li>Each item in the cache will expire after a given time interval, which is
 * settable using the administrative interface (through the
 * {@link uk.ac.rdg.resc.ncwms.config.Config Config} class).</li>
 *
 * <li>If we know the exact file (on the local disk) that corresponds with the
 * given cache request, we check the last modified time and size of this file.
 * If either of these has changed then the cached data will not be used.  (This
 * check is achieved by including these quantities in the {@link TileCacheKey}).  This
 * mechanism is used when a dataset is either a single file or a glob aggregation.
 * It does not, however, work correctly for OPeNDAP datasets or NcML aggregations,
 * because we do not have access to the underlying data files in these cases.</li>
 *
 * <li>For OPeNDAP datasets and NcML aggregations, we check the last modified time
 * of the relevant {@link uk.ac.rdg.resc.ncwms.config.Dataset Dataset} object.  This
 * means that when the metadata for the dataset are re-loaded, all items in the cache
 * that come from this dataset become invalid.  This at least ensures that the cache remains
 * consistent with the metadata holdings.  It will often mean that items in the cache
 * become invalid even when the underlying data have not changed, but this is preferable
 * to a situation in which cached data are used incorrectly.  (The latter situation
 * is still possible but is made less likely by this mechanism.)</li>
 * </ol>
 *
 * <p>Items are never explicitly removed from the cache by the ncWMS code: ehcache
 * does the clean-up in a background thread using a least-recently-used (LRU)
 * algorithm.</p>
 *
 * @author Jon Blower
 */
public class TileCache
{
    private static final Logger logger = LoggerFactory.getLogger(TileCache.class);
    
    private static final String CACHE_NAME = "tilecache";


    // Using the empty array pattern for the retrieval of List contents is generally consider a performance no-no.
    // private static final Float[] EMPTY_FLOAT_ARRAY = new Float[0];

    private CacheManager cacheManager;

    /** The location of the tile cache: will be injected by Spring */
    private File cacheDirectory;

    /** The Config object containing the cache configuration: will be injected by Spring */
    private Config ncwmsConfig;

    private static AtomicLong cachedTileCount = new AtomicLong(0);


    public static long getCachedTileCount(){
        return cachedTileCount.get();
    }


    /** Creates a TileCache in the given working directory. */
    public void init_old()
    {
        // Setting the location of the disk store programmatically is tedious,
        // requiring the creation of lots of objects...
        Configuration tileCacheConfig = new Configuration();
        DiskStoreConfiguration diskStore = new DiskStoreConfiguration();
        diskStore.setPath(this.cacheDirectory.getPath());
        tileCacheConfig.addDiskStore(diskStore);
        tileCacheConfig.addDefaultCache(new CacheConfiguration());
        tileCacheConfig.setName("ncWMS-tile-cache");
        this.cacheManager = new CacheManager(tileCacheConfig);

        Cache tileCache = new Cache(
            CACHE_NAME,                                      // Name for the cache
            ncwmsConfig.getCache().getMaxCacheMemoryUtilization(), // Maximum number of elements in memory
            MemoryStoreEvictionPolicy.LRU,                   // evict least-recently-used elements
            ncwmsConfig.getCache().isEnableDiskStore(),      // Use the disk store?
            "",                                              // disk store path (ignored)
            false,                                           // elements are not eternal
            ncwmsConfig.getCache().getElementLifetimeMinutes() * 60, // Elements will last for this number of seconds in the cache
            0,                                               // Ignore time since last access/modification
            ncwmsConfig.getCache().isEnableDiskStore(),      // Will persist cache to disk in between JVM restarts
            1000,                                            // number of seconds between clearouts of disk store
            null,                                            // no registered event listeners
            null,                                            // no bootstrap cache loader
            ncwmsConfig.getCache().getMaxCacheDiskUtilization()    // Maximum number of elements on disk
        );

        this.cacheManager.addCache(tileCache);
        logger.info("Tile cache started");
    }




    /** Creates a TileCache in the given working directory. */
    public void init()
    {


        //int reserve = ncwmsConfig.getCache().getAvailableMemoryReserve();

        //double highWaterLimit = 0.50;

        //long maxBytesLocalDisk = 20; // Gigabytes

        //boolean constrainByMemorySize = false;

        //double empiricalScaleCoefficient = 3.533;


        long maxHeap = Runtime.getRuntime().maxMemory();
        logger.debug("init() - maxHeap:           "+maxHeap+" bytes");

        long usedMemory = Runtime.getRuntime().totalMemory();
        logger.debug("init() - usedMemory:        "+usedMemory+" bytes");

        long available =  maxHeap-usedMemory;
        logger.debug("init() - available:         "+available+" bytes available.");

        //logger.debug("init() - reserve:           "+ reserve + "%");

        // Compute the maximum tile size based on the current configuration.
        long maxTileSize =  ncwmsConfig.getMaxImageWidth() *  ncwmsConfig.getMaxImageHeight() * 16; // Because a Float object is 16 bytes in Java land.
        logger.debug("init() - MaxTileSize: "+maxTileSize+" bytes (Based on "+ncwmsConfig.getMaxImageWidth()+"x"+ncwmsConfig.getMaxImageHeight()+" pixel max image size from the configuration)");


        // Read the cache utilization values from the configuration
        long maxBytesLocalHeap = ncwmsConfig.getCache().getMaxCacheMemoryUtilization() * 1024 * 1024;  // Convert MB to B
        logger.debug("init() - maxBytesLocalHeap: "+maxBytesLocalHeap+" bytes (from configuration) which is ~"+ (int)(100*((double)maxBytesLocalHeap)/available )+ "% of the "+available+" bytes available.");

        // Compute the number of max size Tiles that will fit in the memory cache.
        double maxTilesInMemory =  ((double)maxBytesLocalHeap/maxTileSize);
        logger.debug("init() - maxTilesInMemory: "+maxTilesInMemory);

        long maxDiskSpace = ((long)ncwmsConfig.getCache().getMaxCacheDiskUtilization()) * 1024 * 1024;    // Convert MB to B
        logger.debug("init() - maxDiskSpace: "+maxDiskSpace+" bytes (from configuration).");

        // Compute the number of max size Tiles that will fit in the disk cache.
        double maxTilesOnDisk =  ((double)maxDiskSpace/maxTileSize);
        logger.debug("init() - maxTilesOnDisk: "+maxTilesOnDisk);



        CacheConfiguration cc = new CacheConfiguration();

         // Name for the cache
         cc.name(CACHE_NAME);

         // evict least-recently-used elements
         cc.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);


         // cached elements are not eternal
         cc.eternal(false);

         // Elements will last for this number of seconds in the cache
         cc.timeToLiveSeconds(ncwmsConfig.getCache().getElementLifetimeMinutes() * 60);

         // Ignore time since last access/modification
         cc.timeToIdleSeconds(0);


        logger.debug("init() - Limiting cache size by number of entries because doing it by memory makes the put() operation expensive");


        /*  Dropped this because we are actually going to let users break stuff...
        // Make the evaluation and update the maxTilesInMemory value if needed.
        if(inMemoryTileLimit < maxTilesInMemory){
            logger.warn("init() - The configuration for max objects/tiles in memory cache exceeds the reserve limit of {}% of available heap.",100 - reserve);
            logger.warn("init() - Resetting memory cache limit to {} Tiles",inMemoryTileLimit);
            maxTilesInMemory = inMemoryTileLimit;
            ncwmsConfig.getCache().setMaxCacheMemoryUtilization(inMemoryTileLimit);
        }
        */

        // Maximum number of elements in memory
        int tilesInMemory = (int)maxTilesInMemory;
        cc.maxEntriesLocalHeap(tilesInMemory);
        logger.debug("init() - Set maxEntriesLocalHeap: "+tilesInMemory);

        // Maximum number of elements on disk
        int tilesOnDisk = (int)maxTilesOnDisk;
        cc.maxEntriesLocalDisk(tilesOnDisk);
        logger.debug("init() - Set maxEntriesLocalDisk: " + tilesOnDisk);




        // We may want to look at using these to configure the cache...
        // cc.maxEntriesInCache(maxTilesInMemory)



         if(ncwmsConfig.getCache().isEnableDiskStore()){

             // Saves stuff in a local disk cache that does not persist between restarts.
             // Why no persistence? Because that's teh enterprise version of the ehcahe lib
             // and that requires $$
             cc.persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP));


             // number of seconds between clearouts of disk store
             cc.diskExpiryThreadIntervalSeconds(1000);
         }
         else {
             // Don't ever mess with the disk...
             cc.persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
         }


        Configuration cacheManagerConfig = new Configuration()
            .diskStore(new DiskStoreConfiguration()
                    .path(cacheDirectory.getPath()));
        cacheManagerConfig.setName("ncWMS-CacheManager");

        cacheManagerConfig.addCache(cc);

        this.cacheManager = new CacheManager(cacheManagerConfig);

        logger.info("init() - Tile cache started");


    }
    
    /**
     * Shuts down the cache
     */
    public void shutdown()
    {
        this.cacheManager.shutdown();
        logger.info("Tile cache shut down");
    }
    
    /**
     * Gets an array of data from this cache, returning null if there is no
     * data matching the given key
     */
    public List<Float> get(TileCacheKey key)
    {
        Cache cache = this.cacheManager.getCache(CACHE_NAME);
        long start = System.nanoTime();
        try {
        Element el = cache.get(key);
        if (el == null)
        {
            logger.debug("get() - Not found in tile cache: {}", key);
            return null;
        }
        else
        {
            Float[] arr = (Float[])el.getObjectValue();
            logger.debug("get() - Found in tile cache. size: {} bytes, key: {}",arr.length*4, key);
            return arr == null ? null : Arrays.asList(arr);
        }
        }
        finally {
            logger.debug("get() - Elapsed time: {} us",(System.nanoTime()-start)/1000);
        }

    }
    
    /**
     * Adds an array of data to this cache.
     */
    public void put(TileCacheKey key, List<Float> data)
    {
        long start = System.nanoTime();

        Float[] arr = data.toArray(new Float[data.size()]);
        this.cacheManager.getCache(CACHE_NAME).put(new Element(key, arr));

        cachedTileCount.set(getCachedTileCount_quick());

        logger.debug("put() - Data object added to tile cache. size: {} bytes, key: {}",arr.length*4, key);
        logger.debug("put() - Elapsed time: {} us",(System.nanoTime()-start)/1000);
    }

    /** Called by Spring to set the directory for the cached tiles */
    public void setCacheDirectory(File cacheDirectory)
    {
        this.cacheDirectory = cacheDirectory;
    }

    /** Called by Spring to set the Config object */
    public void setConfig(Config config)
    {
        this.ncwmsConfig = config;
    }


    public long getCachedTileCount_quick(){
        return cacheManager.getCache(CACHE_NAME).getKeysNoDuplicateCheck().size();
    }
}
