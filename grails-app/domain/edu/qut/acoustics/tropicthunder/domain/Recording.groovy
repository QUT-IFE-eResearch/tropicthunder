package edu.qut.acoustics.tropicthunder.domain

import org.bson.types.ObjectId

class Recording {

    ObjectId id    
    // metadata specific
    Date startDt
    Date endDt
    float durationSecs    
    String encFormat
    String sampleRate
    String channels
    String compression    
    // repository specific..
    String fileName
    String filePath
    String fullPath    
    String repoId
    String repoStat   
    long lastModifiedDate
    String deviceId
    String siteName
    
    static mapping = {
        repoId index:true
        repStatus index:true
        fileName index:true
        fullPath index:true
        deviceId index:true
        siteName index:true
    }
    
    static constraints = {
    }
}
