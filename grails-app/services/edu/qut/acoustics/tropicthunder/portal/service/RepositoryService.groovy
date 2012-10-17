package edu.qut.acoustics.tropicthunder.portal.service

import java.io.*
import groovy.json.*

/**
 * Manages the data repository. This class uses The Fascinator repository.
 * 
 * 
 * @author Shilo Banihit<shiloworks@gmail.com>
 */

class RepositoryService {
    def configService
    def dbService    
    
    def urlRepo
    def urlPathHarvest
    def token
    def privateKey
    def publicKey
    def tokenExpiry
    def repoStatPushed
    def metaFileExt
    
    def init() {
        urlRepo = configService.config.settings.fascinator.urlRepo
        urlPathHarvest = configService.config.settings.fascinator.pathHarvest
        privateKey = configService.config.settings.fascinator.privateKey
        publicKey = configService.config.settings.fascinator.publicKey
        tokenExpiry = configService.config.settings.fascinator.tokenExpiry                        
        repoStatPushed = configService.config.settings.repository.statPushed
        metaFileExt = configService.config.settings.harvester.metaFileExt
    }
    
    /**
    * Queues the file to the repository for harvesting.
    */
    def push() {        
        dbService.getQueued().each {  
            // prepare the meta file            
            def metafile = new File(it.fullPath + metaFileExt)
            def recording = it
            metafile.withWriter('UTF-8') {writer ->
               def jsonBuilder = new StreamingJsonBuilder(writer)
               jsonBuilder.metadata {
                   fileName  recording.fileName
                   filePath  recording.filePath
                   fullPath  recording.fullPath
                   repoId  recording.repoId
                   lastModifiedDate  recording.lastModifiedDate
                   deviceId  recording.deviceId
                   siteName  recording.siteName
                   compression  recording.compression
                   startDt  recording.startDt
                   endDt  recording.endDt
                   channels  recording.channels
                   sampleRate  recording.sampleRate
                   encFormat  recording.encFormat
                   durationSecs  recording.durationSecs                    
               }
            }
            // push to the repository    
            getJsonResult(urlRepo, urlPathHarvest, [filepath:recording.fullPath,reharvest:"false",owner:"admin"], null)
            
            // save the status
            recording.setRepoStat(repoStatPushed)            
            dbService.saveRecording(recording)
        }
    }   
    
    /**
     *
    */
    def pull() {
        
    }
    
    def getJsonResult(uri, path, query, jsonClos) {
        withHttp(uri:uri) {
            get(path:path, query:query, contentType:'application/json') { resp, json ->
                log.debug path + ", status:" + resp.status
                json.each {
                    log.debug it
                }
            }
        }
    }
}
