package uk.ac.rdg.resc.ncwms.util;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;



/**
 * Created by ndp on 12/20/14.
 */
public class Util {

    public static final String DODS_PROTOCOL = "dods://";
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String HTTP_PROTOCOL = "http://";

    public static String getMemoryReport(){
        String msg;
        Runtime rt = Runtime.getRuntime();
        msg =  "Memory Usage:\n"    ;
        msg += " JVM Max Memory:   " + (rt.maxMemory() / 1024) / 1000. + " MB (JVM Maximum Allowable Heap)\n";
        msg += " JVM Total Memory: " + (rt.totalMemory() / 1024) / 1000. + " MB (JVM Heap size)\n";
        msg += " JVM Free Memory:  " + (rt.freeMemory() / 1024) / 1000. + " MB (Unused part of heap)\n";
        msg += " JVM Used Memory:  " + ((rt.totalMemory() - rt.freeMemory()) / 1024) / 1000. + " MB (Currently active memory)\n";
        return msg;

    }

    public static Date getLastModified(String location)  {
        Logger log = LoggerFactory.getLogger(Util.class);

        Date lmd = null;


        if(location.startsWith(DODS_PROTOCOL)){
            location = location.replace(DODS_PROTOCOL,HTTP_PROTOCOL);
        }


        if(location.startsWith(HTTP_PROTOCOL) || location.startsWith(HTTPS_PROTOCOL)) {

            long start = System.nanoTime();
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpHead httpget = new HttpHead(location);

            try {
                CloseableHttpResponse response = httpclient.execute(httpget);
                try {
                    if (response.containsHeader("Last-Modified")) {
                        Header lmod_hdr = response.getFirstHeader("Last-Modified");
                        String lmtStr =  lmod_hdr.getValue();
                        log.debug("getLastModified('"+location+"') - HTTP Last-Modified: "+lmtStr+" ("+ ((System.nanoTime() - start) / 1000000.0) + "ms)");

                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
                        lmd = sdf.parse(lmtStr);    // Yeah! Last Modified Time!
                    }

                } finally {
                    response.close();
                    httpclient.close();
                }

            } catch (Exception e) {
                log.error("getLastModified() - Caught {} Message: {}",e.getClass().getName(),e.getMessage());
                lmd = new Date(-1);
            }
        }
        else {
            File f = new File(location);
            if (f.exists()) {
                if (f.isFile()) {
                    // This is a local data file or an NcML file
                    long flmd = f.lastModified();
                    log.debug("getLastModified('"+location+"') - File Last-Modified: "+ flmd);
                    lmd = new Date(flmd); // Yeah! Last Modified Time!
                } else {
                    throw new IllegalArgumentException(location +
                            " exists but does not appear to be a simple file.");
                }

            }


        }

        if(lmd==null){
            lmd = new Date(0);    // That's what Netcdf-Java does when it goes wrong...
        }

        return lmd;

    }
}
