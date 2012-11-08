package edu.qut.acoustics.tropicthunder.portal.service

import java.io.*
import groovy.json.*
import java.security.MessageDigest

/**
 * Manages the data repository. 
 * 
 * This class uses The Fascinator repository. Communication currently secured by a token.
 * 
 * 
 * @author Shilo Banihit<shiloworks@gmail.com>
 */

class RepositoryService {
    def configService
    def dbService    
    
    def urlRepo
    def urlPathHarvest
    def urlPathSolr
    def token
    def privateKey
    def publicKey
    def tokenExpiry
    def repoStatPushed
    def repoStatLive
    def metaFileExt
    def configFileExt
    def renderPendingWait
    def renderPendingMaxCheck
    def maxRender

    def secToken
    def secTokenCreateDt
    
    def init() {
        urlRepo = configService.config.settings.fascinator.urlRepo
        urlPathHarvest = configService.config.settings.fascinator.pathHarvest
        privateKey = configService.config.settings.fascinator.privateKey
        publicKey = configService.config.settings.fascinator.publicKey
        tokenExpiry = configService.config.settings.fascinator.tokenExpiry                        
        repoStatPushed = configService.config.settings.repository.statPushed
        repoStatLive = configService.config.settings.repository.statLive
        metaFileExt = configService.config.settings.harvester.metaFileExt
        configFileExt = configService.config.settings.harvester.configFileExt
        urlPathSolr = configService.config.settings.fascinator.pathSolr
        renderPendingWait = configService.config.settings.fascinator.renderPendingWait
        renderPendingMaxCheck = configService.config.settings.fascinator.renderPendingMaxCheck
        maxRender = configService.config.settings.fascinator.maxRender
    }
    
    /**
    * Queues the file to the repository for harvesting.
    * 
    * Returns the number of records queued.
    */
    def push() {        
        //createSecToken(Date.parse(configService.config.settings.fascinator.timestampFormat,"20121017152800"))
        validateSecToken()
        log.debug "Using sectoken:$secToken"
        def ctr = 0
        def pendingRenderCtr = 0
        dbService.getQueued().each {  
            // check if we've hit the render the max...
            if (pendingRenderCtr == maxRender) {
                log.debug "Waiting for pending ${maxRender} harvests to complete."
                pull(maxRender, maxRender * renderPendingMaxCheck)                
                pendingRenderCtr = 0
                log.debug "Pending harvests completed, continuing with harvest..."
            }
            // prepare the meta file            
            def metafile = new File(it.fullPath + metaFileExt)
            def recording = it
            // build the metadata file
            def strWriter = new StringWriter()
            def jsonBuilder = new StreamingJsonBuilder(strWriter)
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
            
            metafile.withWriter('UTF-8') {writer ->
                writer.write(JsonOutput.prettyPrint(strWriter.toString()))
            }           
            // push to the repository    
            getJsonResult(urlRepo, urlPathHarvest, [filepath:recording.fullPath,reharvest:"false",owner:"admin", token:secToken], null)
            
            // save the status            
            recording.setRepoStat(repoStatPushed)                        
            dbService.saveRecording(recording)            
            ctr++
            pendingRenderCtr++
        }
        if (pendingRenderCtr > 0) {
            log.debug "Records have been submitted for harvesting, waiting for completion."
            pull(pendingRenderCtr, pendingRenderCtr * renderPendingMaxCheck )
        }
        return ctr
    }   
    
    /**
     * Gets all pushed records for harvesting and check the 'render-pending' flag.
     * 
    */
    def pull(numRecs, numChecks) {
        def renderedCtr = 0
        def checkCtr = 0
        def statusMap = [harvestStat:null, renderPending:true]
        while (renderedCtr < numRecs && checkCtr < numChecks ) {
            dbService.getPushed().each {
                statusMap.harvestStat = null
                statusMap.renderPending = true
                def recording = it
                getRenderPendingValue(recording.fileName, statusMap)
                if (statusMap.harvestStat != null && statusMap.renderPending == false) {
                    // save the status                    
                    recording.setRepoStat(repoStatLive)                                                    
                    dbService.saveRecording(recording)
                    renderedCtr++
                }                
            }
            checkCtr++
            log.debug "pull() wait number: ${checkCtr}, maxWait: ${numChecks}"
            // sleeping after checking all recs...
            sleep(renderPendingWait)            
        }
    }
    
    def getRenderPendingValue(fileName, statusMap) {
        def renderPending = true
        def harvestStat = getJsonResult(urlRepo, urlPathSolr, [q:"(file_name:$fileName AND item_type:object)", wt:"json"], null)
        if (harvestStat.response.docs[0] != null) {
            renderPending = Boolean.parseBoolean(harvestStat.response.docs[0]['render-pending'][0])
            log.debug "Render-pending for $fileName is: $renderPending"
        } else {
            log.debug "Render-pending not available for $fileName"
        }
        statusMap.harvestStat = harvestStat
        statusMap.renderPending = renderPending
    }
    
    /**
     * Get JSON document from Fascinator URL. 
     * 
     */
    def getJsonResult(uri, path, query, jsonClos) {    
        def retval = null
        try {
            withHttp(uri:uri) {
                get(path:path, query:query, contentType:'application/json') { resp, json ->
                    log.debug "$path, status: $resp.status" 
                    json.each {
                        log.debug it
                    }
                    retval = json
                }
            }
        } catch (Exception e) {
            log.error e
        }
        return retval
    }
    
    /**
     * Check the validity of the token used for Fascinator communication
     * 
    */
    def validateSecToken() {        
        if (secTokenCreateDt == null || (secTokenCreateDt.getTime() + tokenExpiry > System.currentTimeMillis())) {
            createSecToken(new Date())
        }
    }
    
    /**
     * Create the security token for Fascinator communication
    */
    def createSecToken(tokenDt) {
        secTokenCreateDt = tokenDt
        def timestamp = secTokenCreateDt.format(configService.config.settings.fascinator.timestampFormat)
        def userTsPair = "${configService.config.settings.fascinator.username}:${timestamp}"
        def tokenSeed = "${userTsPair}:${configService.config.settings.fascinator.privateKey}".toString()
        def hash = MessageDigest.getInstance("MD5").digest(tokenSeed.getBytes("UTF-8")).encodeHex().toString()
        secToken = "${userTsPair}:${configService.config.settings.fascinator.publicKey}:${hash}".toString()
    }        
}
