package edu.qut.acoustics.tropicthunder.domain

import org.bson.types.ObjectId

class Recording {

    ObjectId id    
    // metadata specific
    Date startDt
    Date endDt
    double durationSecs    
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
    String projectName
    String storageId
    
    static mapping = {
        repoId index:true
        repStatus index:true
        fileName index:true
        fullPath index:true
        deviceId index:true
        siteName index:true
        projectName index:true
    }
    
    static constraints = {
        startDt              nullable:true
        endDt                nullable:true
        durationSecs         nullable:true
        encFormat            nullable:true
        sampleRate           nullable:true
        channels             nullable:true
        compression          nullable:true
        fileName             nullable:true
        filePath             nullable:true
        repoId               nullable:true
        repoStat             nullable:true
        lastModifiedDate     nullable:true
        deviceId             nullable:true
        siteName             nullable:true
        projectName          nullable:true
        storageId            nullable:true
    }        
}
