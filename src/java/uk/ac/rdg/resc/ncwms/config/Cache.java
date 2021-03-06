/*
 * Copyright (c) 2008 The University of Reading
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

package uk.ac.rdg.resc.ncwms.config;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Configuration for the {@link uk.ac.rdg.resc.ncwms.cache.TileCache TileCache}.
 *
 * @author Jon Blower
 */
@Root(name="cache")
public class Cache
{
    @Attribute(name="enabled", required=false)
    private boolean enabled = false; // Ships with cache disabled: admins have to explicitly enable
    
    @Element(name="elementLifetimeMinutes", required=false)
    private int elementLifetimeMinutes = 60 * 24; // default is one day

//    @Element(name="availableMemoryReserve", required=false)
//    private int availableMemoryReserve = 50; // Reserves 50% of available memory (at startup) for everything "not" cache


    /**
     * The amount of memory (in megabytes) that that cache is allowed to utilize
     */
    @Element(name="maxNumItemsInMemory", required=false)
    private int maxCacheMemoryUtilization = 200; // Size of the in-memory cache in MB
    
    @Element(name="enableDiskStore", required=false)
    private boolean enableDiskStore = true;
    
    /**
     * The number of megabytes of disk space that the cache is allowed to use.
     */
    @Element(name="maxNumItemsOnDisk", required=false)
    private int maxCacheDiskUtilization = 2000; // Size of the disk cache in MB
    
    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public int getElementLifetimeMinutes()
    {
        return elementLifetimeMinutes;
    }

    public void setElementLifetimeMinutes(int elementLifetimeMinutes)
    {
        this.elementLifetimeMinutes = elementLifetimeMinutes;
    }

    public int getMaxCacheMemoryUtilization()
    {
        return maxCacheMemoryUtilization;
    }

    public void setMaxCacheMemoryUtilization(int maxCacheMemoryUtilization)
    {
        this.maxCacheMemoryUtilization = maxCacheMemoryUtilization;
    }

    public boolean isEnableDiskStore()
    {
        return enableDiskStore;
    }

    public void setEnableDiskStore(boolean enableDiskStore)
    {
        this.enableDiskStore = enableDiskStore;
    }

    /*
    public int getAvailableMemoryReserve()
    {
        return availableMemoryReserve;
    }

    public void setAvailableMemoryReserve(int availableMemoryReserve)
    {
        this.availableMemoryReserve = availableMemoryReserve;
    }
   */

    public int getMaxCacheDiskUtilization()
    {
        return maxCacheDiskUtilization;
    }

    public void setMaxCacheDiskUtilization(int maxCacheDiskUtilization)
    {
        this.maxCacheDiskUtilization = maxCacheDiskUtilization;
    }



}
